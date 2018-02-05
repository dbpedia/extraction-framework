package org.dbpedia.extraction.scripts

import java.io.File

import com.fasterxml.jackson.core.{JsonParseException, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.{JsonNode, MappingJsonFactory}
import org.dbpedia.extraction.destinations.{DestinationUtils, WriterDestination}
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{IOUtils, Language, RichFile}
import org.dbpedia.iri.UriUtils

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scala.collection.convert.decorateAsScala._

/**
  * Created by chile on 30.09.17.
  * Extracts links from the NYT API raw json resource representations
  * Input files should be the result files of this crawler script: extraction-framework/scripts/src/main/bash/nytApiCrawler.sh
  *
  * Results need postprocessing of resolving redirects/disambiguations and the uri to iri script (just to be sure)
  */
object NYTLinkExtractor {

  private val nytBaseUri = "http://api.nytimes.com/svc/semantic/v2/concept/name/"
  private var linkRelationMap = Map[String, OntologyProperty]()
  private val factory = new MappingJsonFactory()
  private var destination: WriterDestination = _

  private def readLinkNode(link: JsonNode): NytLink ={
    var linkId: Int = 0
    var relation: String = null
    var linkTarget: String = null
    var linkType: String = null
    var id: Int = 0
    var conceptType: String = null
    var conceptName: String = null

    for(field <- link.fields().asScala){
      if(field.getKey == "concept_id")
        linkId = field.getValue.asInt()
      else if(field.getKey == "concept_name")
        conceptName = field.getValue.asText()
      else if(field.getKey == "concept_type")
        conceptType = field.getValue.asText()
      else if(field.getKey == "link_id")
        id = field.getValue.asInt()
      else if(field.getKey == "relation")
        relation = field.getValue.asText()
      else if(field.getKey == "link_type")
        linkType = field.getValue.asText()
      else if(field.getKey == "link")
        linkTarget = field.getValue.asText()
    }
    NytLink(linkId, relation, linkTarget, linkType, id, conceptName, conceptType)
  }

  private def readSingleResourceNode(node: JsonNode): Option[NytResource] ={
    if(node == null)
      return None

    var id: Int = 0
    var conceptType: String = null
    var conceptName: String = null
    var active: Boolean = false

    //all outgoing Links
    var links = ListBuffer[NytLink]()

    for(field <- node.fields().asScala){
      if(field.getKey == "concept_id")
        id = field.getValue.asInt()
      else if(field.getKey == "concept_name")
        conceptName = field.getValue.asText()
      else if(field.getKey == "concept_type")
        conceptType = field.getValue.asText()
      else if(field.getKey == "concept_status") {
        if (field.getValue.asText() == "Active")
          active = true
      }
      else if(field.getKey == "links"){
        for(link <- field.getValue.elements().asScala)
          links += readLinkNode(link)
      }
    }

    if(active)
      Some(NytResource(id, conceptName, conceptType, links.toList))
    else
      None
  }

  private def gotoNextToken(jp: JsonParser): Boolean = {
    Try{     jp.nextToken()   }  match{
      case Success(t) => t != JsonToken.END_OBJECT
      case Failure(f) => f match {
        case uc : JsonParseException if uc.getMessage.contains("Unexpected character") =>
          true
        case _ =>
          System.err.println("A json object does not match the expected format.")
          true
      }
    }
  }

  private def readRawApiResultFile(filename: String): Unit ={

    val stream = IOUtils.inputStream(new RichFile(new File(filename)))
    val jp = factory.createParser(stream)
    val start = jp.nextToken()

    if (start != JsonToken.START_OBJECT) {
      System.out.println("Error: root should be object: quiting.")
      return
    }
    while (gotoNextToken(jp)) {
      Try{String.valueOf(jp.getCurrentName).toInt} match{
        case Failure(f) => System.err.println("A json object does not match the expected format.")
        case Success(i) =>
          jp.nextToken()
          Try{jp.readValueAsTree[JsonNode]()} match{
            case Failure(f) => System.err.println("A json object does not match the expected format.")
            case Success(s) => readSingleResourceNode(s) match{
              case Some(r) => destination.write(convertLinks(r.links))
              case None =>
            }
          }
      }
    }
  }

  private def getNytResourceUri(res: NytConceptBase): String ={
    UriUtils.createURI(nytBaseUri + res.conceptType + "/" + res.conceptName).get.toASCIIString
  }

  private def fromDbpediaUri(link: NytLink): Quad ={
    val subject = UriUtils.uriToIri(link.targetName).toString
    val predicate = linkRelationMap.get(link.relation) match{
      case Some(r) => r
      case None =>
        System.err.println("Found unmapped relation: " + link.relation)
        linkRelationMap("related")
    }
    val target = getNytResourceUri(link)
    new Quad(null, null, subject, predicate, target, null, null)
  }

  private def fromWikipediaLink(link: NytLink): Quad ={
    val subject =  UriUtils.uriToIri(UriUtils.createURI(Language.English.resourceUri.toString + link.targetName).get.toASCIIString).toString
    val predicate = linkRelationMap.get(link.relation) match{
      case Some(r) => r
      case None =>
        System.err.println("Found unmapped relation: " + link.relation)
        linkRelationMap("related")
    }
    val target = getNytResourceUri(link)
    new Quad(null, null, subject, predicate, target, null, null)
  }

  private def convertLinks(links: List[NytLink]): List[Quad] ={
    links.map(l => l.linkType match{
      case "dbpedia_uri" => Some(fromDbpediaUri(l))
      case "wikipedia_raw_name" => Some(fromWikipediaLink(l))
      case _ => None
    }).filter(x => x.isDefined).map(_.get)
  }

  def main(args: Array[String]): Unit = {

    destination = DestinationUtils.createWriterDestination(new RichFile(new File(args.head)))
    val ontology = new OntologyReader().read( XMLSource.fromFile(new File("../ontology.xml"), Language.Mappings))

    //mapping possible link relations to link predicates
    linkRelationMap += "sameAs" -> ontology.getOntologyProperty("owl:sameAs").get
    linkRelationMap += "broader" -> ontology.getOntologyProperty("skos:narrower").get  // turning this around since we point to nyt
    linkRelationMap += "narrower" -> ontology.getOntologyProperty("skos:broader").get
    linkRelationMap += "related" -> ontology.getOntologyProperty("skos:related").get

    destination.open()
    for (file <- args.drop(1)) {
      System.out.println("reading file: " + file)
      readRawApiResultFile(file)
    }
    destination.close()
  }

  class NytConceptBase(val id: Int, val conceptName: String, val conceptType: String)
  case class NytResource(override val id: Int, override val conceptName: String, override val conceptType: String, links: List[NytLink])
    extends NytConceptBase(id, conceptName, conceptType)
  case class NytLink(linksId: Int, relation: String, targetName: String, linkType: String, override val id: Int, override val conceptName: String, override val conceptType: String)
    extends NytConceptBase(id, conceptName, conceptType)
}
