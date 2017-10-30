package org.dbpedia.extraction.mappings

import java.io.{File, IOException}

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.dbpedia.extraction.config.mappings.wikidata._
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.{OntologyProperty, _}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{JsonConfig, Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.Config

import scala.collection.convert.decorateAsScala._
import scala.language.{postfixOps, reflectiveCalls}

/**
 * Created by ali on 10/26/14.
 * This extractor maps wikidata statements to DBpedia ontology
 * wd:Q64 dbo:primeMinister wd:Q8863.
 *
 * In order to extract n-ary relation mapped statements are reified.
 * For reification unique statement URIs is created.
 * Mapped statements reified on the form of
 * wd:Q64_P6_Q8863 rdf:type rdf:Statement.
 * wd:Q64_P6_Q8863 rdf:subject wd:Q64 .
 * wd:Q64_P6_Q8863 rdf:predicate dbo:primeMinister.
 * wd:Q64_P6_Q8863 rdf:object wd:Q8863.
 *
 * Qualifiers use same statement URIs and mapped on the form of
 * wd:Q64_P6_Q8863 dbo:startDate "2001-6-16"^^xsd:date.
 * wd:Q64_P6_Q8863 dbo:endDate "2014-12-11"^^xsd:date.
 */
@SoftwareAgentAnnotation(classOf[WikidataR2RExtractor], AnnotationType.Extractor)
class WikidataR2RExtractor(
                            context: {
                              def ontology: Ontology
                              def language: Language
                              def configFile: Config
                            }
                            )
  extends JsonNodeExtractor {

  val config: JsonConfig = new JsonConfig(JsonConfig.getClass.getClassLoader.getResource("wikidatar2r.json"))

  var equivalentProperties: Map[String, Set[String]] = context.ontology.wikidataPropertiesMap.map(x => x._1 -> x._2.map(y => y.uri))

  //class mappings generated with script WikidataSubClassOf and written to json file.
  val classMappings = readClassMappings(context.configFile.wikidataMappingsFile)

  private val rdfType = context.ontology.properties("rdf:type")
  private val wikidataSplitIri = context.ontology.properties("wikidataSplitIri")
  private val rdfStatement = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement"
  private val rdfSubject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject"
  private val rdfPredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate"
  private val rdfObject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#object"
  private val wikidataProperrtyUri = "https://www.wikidata.org/wiki/Property"

  // this is where we will store the output
  override val datasets = Set(
    DBpediaDatasets.WikidataR2R_literals,
    DBpediaDatasets.WikidataR2R_objects,
    DBpediaDatasets.WikidataR2R_mappingerrors,
    DBpediaDatasets.WikidataDublicateIriSplit,
    DBpediaDatasets.WikidataReifiedR2R,
    DBpediaDatasets.WikidataReifiedR2RQualifier,
    DBpediaDatasets.GeoCoordinates,
    DBpediaDatasets.Images,
    DBpediaDatasets.OntologyTypes,
    DBpediaDatasets.OntologyTypesTransitive,
    DBpediaDatasets.WikidataSameAsExternal,
    DBpediaDatasets.WikidataNameSpaceSameAs,
    DBpediaDatasets.WikidataR2R_ontology,
    DBpediaDatasets.WikidataTypeLikeStatements)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      for ((statementGroup) <- page.wikiDataDocument.getStatementGroups) {
        val duplicateList = getDuplicates(statementGroup)
        val statements = checkRank(statementGroup)
        statements.foreach {
          statement => {
            val claim = statement.getClaim
            val property = claim.getMainSnak.getPropertyId.getId
            val equivPropertySet = getEquivalentProperties(property)
            claim.getMainSnak match {
              case mainSnak: ValueSnak => {
                val value = mainSnak.getValue

                val equivClassSet = getEquivalentClass(value)
                val receiver: WikidataCommandReceiver = new WikidataCommandReceiver
                val command: WikidataTransformationCommands = config.getCommand(property, value, equivClassSet, equivPropertySet, receiver)
                command.execute()

                val statementUri = WikidataUtil.getStatementUri(subjectUri, property, value)

                val PV = property + " " + value
                if (duplicateList.contains(PV)) {
                  val statementUriWithHash = WikidataUtil.getStatementUriWithHash(subjectUri, property, value, statement.getStatementId)
                  quads += new Quad(context.language, DBpediaDatasets.WikidataDublicateIriSplit, statementUri, wikidataSplitIri, statementUriWithHash, page.wikiPage.sourceIri, null)
                }
                quads ++= getQuad(page, subjectUri, statementUri, receiver.getMap())

                //Wikidata qualifiers R2R mapping
                quads ++= getQualifersQuad(page, statementUri, claim)
              }

              case _ =>
            }

          }
        }
      }
    }

    splitDatasets(quads, subjectUri, page)
  }

  def checkRank(statementGroup: StatementGroup): Seq[Statement] ={
    var statements = Seq[Statement]();

    //If statementGroup has preferred statement
    statements = statementGroup.getStatements.filter(_.getRank.equals(StatementRank.PREFERRED));

    //If there is no preferred statement, take all Normal statemnts
    if (statements.isEmpty) {
      statements = statementGroup.getStatements.filter(_.getRank.equals(StatementRank.NORMAL))
    }
    statements
  }
  def getQuad(page: JsonNode, subjectUri: String,statementUri:String,map: mutable.Map[String, String]): ArrayBuffer[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    map.foreach {
      propertyValue => {
        try {
          val ontologyProperty = context.ontology.properties(propertyValue._1)
          val datatype = findType(null, ontologyProperty.range)
          if (propertyValue._2.startsWith("http:") && datatype != null && datatype.name == "xsd:string") {
            quads += new Quad(context.language, DBpediaDatasets.WikidataR2R_mappingerrors, subjectUri, ontologyProperty, propertyValue._2.toString, page.wikiPage.sourceIri, datatype)
          } else {

            //split to literal / object datasets
            val mapDataset = if (ontologyProperty.isInstanceOf[OntologyObjectProperty]) DBpediaDatasets.WikidataR2R_objects else DBpediaDatasets.WikidataR2R_literals
            //Wikidata R2R mapping without reification
            val quad = new Quad(context.language, mapDataset, subjectUri, ontologyProperty, propertyValue._2.toString, page.wikiPage.sourceIri, datatype)
            quads += quad

            //Reification added to R2R mapping
            quads ++= getReificationQuads(page, statementUri, datatype, quad)
          }
        } catch {
          case e: Exception => println("exception caught: " + e)
        }

      }
    }
    quads
  }

  def getReificationQuads(page: JsonNode, statementUri: String, datatype: Datatype, quad: Quad): ArrayBuffer[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfType, rdfStatement, page.wikiPage.sourceIri)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfSubject, quad.subject, page.wikiPage.sourceIri, null)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfPredicate, quad.predicate, page.wikiPage.sourceIri, null)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfObject, quad.value, page.wikiPage.sourceIri, datatype)

    quads
  }

  def getQualifersQuad(page: JsonNode, statementUri: String, claim: Claim): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    val receiverForQualifier: WikidataCommandReceiver = new WikidataCommandReceiver

    claim.getQualifiers.foreach {

      qualifier => {
        for (qual <- qualifier) {
          qual match {
            case valueSnak: ValueSnak => {
              val qualifierProperty = valueSnak.getPropertyId.getId

              val qualifierValue = valueSnak.getValue

              val equivClassSet = getEquivalentClass(qualifierValue)
              val equivPropertySet = getEquivalentProperties(qualifierProperty)

              val commandQualifier: WikidataTransformationCommands =
                config.getCommand(qualifierProperty, qualifierValue, equivClassSet, equivPropertySet, receiverForQualifier)

              commandQualifier.execute()

              receiverForQualifier.getMap().foreach {
                mappedQualifierValue => {
                  try {
                    val ontologyProperty = context.ontology.properties(mappedQualifierValue._1)
                    val datatype = if (ontologyProperty.range.isInstanceOf[Datatype]) ontologyProperty.range.asInstanceOf[Datatype] else null
                    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2RQualifier,
                      statementUri, ontologyProperty, mappedQualifierValue._2, page.wikiPage.sourceIri, datatype)
                  } catch {
                    case e: Exception => println("exception caught: " + e)
                  }
                }
              }
            }
            case _ =>
          }
        }
      }
    }

    quads
  }

  private def getDuplicates(statementGroup: StatementGroup): mutable.MutableList[String] = {
    var duplicateList = mutable.MutableList[String]();
    statementGroup.getStatements.foreach {
      statement => {
        val claim = statement.getClaim
        val property = claim.getMainSnak.getPropertyId.getId
        claim.getMainSnak match {
          case mainSnak: ValueSnak =>
            val value = mainSnak.getValue
            val PV = property + " " + value
            duplicateList +=PV
          case _ =>
        }
      }
    }
    duplicateList= duplicateList.diff(duplicateList.distinct).distinct
    duplicateList
  }

  private def findType(datatype: Datatype, range: OntologyType): Datatype = {
    if (datatype != null) datatype
    else range match {
      case datatype: Datatype => datatype
      case _ => null
    }
  }

  private def getEquivalentClass(value: Value): Set[OntologyClass] = {
    value match {
      case v: ItemIdValue =>
        val wikidataItem = WikidataUtil.getItemId(v)
          if (classMappings.nonEmpty){
            classMappings.get(wikidataItem) match {
              case Some(mappings) => return mappings
              case None =>
            }
          }
      case _ =>
    }
    Set()
  }

  private def readClassMappings(file: File): mutable.Map[String,Set[OntologyClass]] = {
    val finalMap = mutable.Map[String,Set[OntologyClass]]()

    try {
      val source = scala.io.Source.fromFile(file)
      val jsonString = source.getLines() mkString
      val mapper = new ObjectMapper() with ScalaObjectMapper
      mapper.registerModule(DefaultScalaModule)
      val mapFromJson = mapper.readValue[Map[String, String]](jsonString)

      mapFromJson.foreach{
        pair =>
          mapFromJson.get(pair._1) match {
            case Some(ontologyKey) => {
              context.ontology.classes.get(ontologyKey) match {
                case Some(oClass)=> finalMap+=pair._1 -> Set(oClass)
                case _=>
              }}
            case _=>
          }
      }
    } catch {
      case ioe: IOException => println("Please check class mapping file "+ioe)
      case npe: NullPointerException=> println("Null pointer exception, file is empty" + npe)
    }

    context.ontology.wikidataClassesMap.foreach {
      classMap => finalMap+=classMap._1.replace("wikidata:","")->classMap._2
    }

    finalMap
  }

  private def getEquivalentProperties(property: String): Set[String] = {
    equivalentProperties.get("wikidata:" + property) match{
      case Some(x) => x
      case None => Set[String]()
    }
  }

  /**
   * Splits the quads to different datasets according to custom rules
   */
  private def splitDatasets(originalGraph: Seq[Quad], subjectUri : String, page: JsonNode) : Seq[Quad] = {
    val adjustedGraph = new ArrayBuffer[Quad]

    originalGraph.foreach(q => {
      if (q.dataset.equals(DBpediaDatasets.WikidataR2R_literals.encoded) || q.dataset.equals(DBpediaDatasets.WikidataR2R_objects.encoded)) {
        q.predicate match {

            // split type statements, some types e.g. cordinates go to separate datasets
          case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" =>
            q.value match {
              case "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing"
                   => adjustedGraph += q.copy(dataset = DBpediaDatasets.GeoCoordinates.encoded)
              case _ => // This is the deafult types we get
                adjustedGraph += q.copy(dataset = DBpediaDatasets.OntologyTypes.encoded)
                // Generate inferred types
                context.ontology.classes.get(q.value.replace("http://dbpedia.org/ontology/", "")) match {
                  case Some(clazz) =>
                    for (cls <- clazz.relatedClasses.filter(_ != clazz))
                      adjustedGraph += new Quad(context.language, DBpediaDatasets.OntologyTypesTransitive, subjectUri, rdfType, cls.uri, page.wikiPage.sourceIri)
                  case None =>
                }
            }

          case "http://www.w3.org/2000/01/rdf-schema#subClassOf"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataR2R_ontology.encoded)

            // coordinates dataset
          case "http://www.w3.org/2003/01/geo/wgs84_pos#lat" | "http://www.w3.org/2003/01/geo/wgs84_pos#long" | "http://www.georss.org/georss/point"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.GeoCoordinates.encoded)

            //Images dataset
          case  "http://xmlns.com/foaf/0.1/thumbnail" | "http://xmlns.com/foaf/0.1/depiction" | "http://dbpedia.org/ontology/thumbnail"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.Images.encoded)

            // sameAs links
          case "http://www.w3.org/2002/07/owl#sameAs" =>
            // We get the commons:Creator links
            if (q.value.startsWith("http://commons.dbpedia.org")) {
              adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataNameSpaceSameAs.encoded)
            } else {
              adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataSameAsExternal.encoded)
            }

          case _ => adjustedGraph += q
        }
      } else adjustedGraph += q
    })

    adjustedGraph
  }
}