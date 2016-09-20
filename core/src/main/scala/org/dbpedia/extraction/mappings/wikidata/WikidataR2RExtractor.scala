package org.dbpedia.extraction.mappings

import java.io.{IOException}

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.dbpedia.extraction.config.mappings.wikidata._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, Quad}
import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.{JsonConfig, Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import scala.language.postfixOps

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
class WikidataR2RExtractor(
                            context: {
                              def ontology: Ontology
                              def language: Language
                            }
                            )
  extends JsonNodeExtractor {

  val config: JsonConfig = new JsonConfig(this.getClass.getResource("wikidatar2rconfig.json"))

  //class mappings generated with script WikidataSubClassOf and written to json file.
  val classMappings = readClassMappings("auto_generated_mapping.json")

  private val rdfType = context.ontology.properties("rdf:type")
  private val wikidataSplitIri = context.ontology.properties("wikidataSplitIri")
  private val rdfStatement = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Statement"
  private val rdfSubject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#subject"
  private val rdfPredicate = "http://www.w3.org/1999/02/22-rdf-syntax-ns#predicate"
  private val rdfObject = "http://www.w3.org/1999/02/22-rdf-syntax-ns#object"

  // this is where we will store the output
  val WikidataR2RErrorDataset = new Dataset("wikidata-r2r-mapping-errors")
  val WikidataDuplicateIRIDataset = new Dataset("wikidata-duplicate-iri-split")
  override val datasets = Set(DBpediaDatasets.WikidataR2R_literals, DBpediaDatasets.WikidataR2R_objects, WikidataR2RErrorDataset,WikidataDuplicateIRIDataset,
                              DBpediaDatasets.WikidataReifiedR2R, DBpediaDatasets.WikidataReifiedR2RQualifier, DBpediaDatasets.GeoCoordinates,
                              DBpediaDatasets.Images, DBpediaDatasets.OntologyTypes, DBpediaDatasets.OntologyTypesTransitive,
                              DBpediaDatasets.WikidataSameAsExternal, DBpediaDatasets.WikidataNameSpaceSameAs, DBpediaDatasets.WikidataR2R_ontology)

  override def extract(page: JsonNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    if (page.wikiPage.title.namespace != Namespace.WikidataProperty) {
      for ((statementGroup) <- page.wikiDataDocument.getStatementGroups) {
        val duplicateList = getDuplicates(statementGroup)
        statementGroup.getStatements.foreach {
          statement => {
            val claim = statement.getClaim()
            val property = claim.getMainSnak().getPropertyId().getId
            val equivPropertySet = getEquivalentProperties(property)
            claim.getMainSnak() match {
              case mainSnak: ValueSnak => {
                val value = mainSnak.getValue

                val equivClassSet = getEquivalentClass(value)
                val receiver: WikidataCommandReceiver = new WikidataCommandReceiver
                val command: WikidataTransformationCommands = config.getCommand(property, value, equivClassSet, equivPropertySet, receiver)
                command.execute()

                val statementUri = WikidataUtil.getStatementUri(subjectUri, property, value)

                val PV = property + " " + value;
                if (duplicateList.contains(PV)) {
                  val statementUriWithHash = WikidataUtil.getStatementUriWithHash(subjectUri, property, value, statement.getStatementId.toString)
                  quads += new Quad(context.language, WikidataDuplicateIRIDataset, statementUri, wikidataSplitIri, statementUriWithHash, page.wikiPage.sourceUri, null)
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

  def getQuad(page: JsonNode, subjectUri: String,statementUri:String,map: mutable.Map[String, String]): ArrayBuffer[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    map.foreach {
      propertyValue => {
        try {
          val ontologyProperty = context.ontology.properties(propertyValue._1)
          val datatype = findType(null, ontologyProperty.range)
          if (propertyValue._2.startsWith("http:") && datatype != null && datatype.name == "xsd:string") {
            quads += new Quad(context.language, WikidataR2RErrorDataset, subjectUri, ontologyProperty, propertyValue._2.toString, page.wikiPage.sourceUri, datatype)
          } else {

            //split to literal / object datasets
            val mapDataset = if (ontologyProperty.isInstanceOf[OntologyObjectProperty]) DBpediaDatasets.WikidataR2R_objects else DBpediaDatasets.WikidataR2R_literals
            //Wikidata R2R mapping without reification
            val quad = new Quad(context.language, mapDataset, subjectUri, ontologyProperty, propertyValue._2.toString, page.wikiPage.sourceUri, datatype)
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

    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfType, rdfStatement, page.wikiPage.sourceUri)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfSubject, quad.subject, page.wikiPage.sourceUri, null)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfPredicate, quad.predicate, page.wikiPage.sourceUri, null)
    quads += new Quad(context.language, DBpediaDatasets.WikidataReifiedR2R, statementUri, rdfObject, quad.value, page.wikiPage.sourceUri, datatype)

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
                      statementUri, ontologyProperty, mappedQualifierValue._2, page.wikiPage.sourceUri, datatype)
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
        val claim = statement.getClaim()
        val property = claim.getMainSnak().getPropertyId().getId
        claim.getMainSnak() match {
          case mainSnak: ValueSnak => {
            val value = mainSnak.getValue
            val PV = property + " " + value;
            duplicateList +=PV;
            }
          case _ =>
        }
      }
    }
    duplicateList= duplicateList.diff(duplicateList.distinct).distinct
    duplicateList
  }

  private def findType(datatype: Datatype, range: OntologyType): Datatype = {
    if (datatype != null) datatype
    else if (range.isInstanceOf[Datatype]) range.asInstanceOf[Datatype]
    else null
  }

  private def getEquivalentClass(value: Value): Set[OntologyClass] = {
    var classes = Set[OntologyClass]()

    value match {
      case v: ItemIdValue => {
        val wikidataItem = WikidataUtil.getItemId(v)
          classMappings.get(wikidataItem) match {
            case Some(mappings) => classes++=mappings
            case _=>
          }
      }
      case _ =>
    }
    classes
  }

  private def readClassMappings(fileName:String): mutable.Map[String,Set[OntologyClass]] = {
    val finalMap = mutable.Map[String,Set[OntologyClass]]()

    try {
      val source = scala.io.Source.fromFile(fileName)
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
    }

    context.ontology.wikidataClassesMap.foreach {
      classMap => finalMap+=classMap._1.replace("wikidata:","")->classMap._2
    }

    return finalMap
  }

  private def getEquivalentProperties(property: String): Set[OntologyProperty] = {
    var properties = Set[OntologyProperty]()
    val prop = "wikidata:" + property
    context.ontology.wikidataPropertiesMap.foreach({ map =>
      if (map._1.matches(prop)) {
        properties ++= map._2
      }
    })
    properties
  }

  /**
   * Splits the quads to different datasets according to custom rules
   */
  private def splitDatasets(originalGraph: Seq[Quad], subjectUri : String, page: JsonNode) : Seq[Quad] = {
    val adjustedGraph = new ArrayBuffer[Quad]

    originalGraph.map(q => {
      if (q.dataset.equals(DBpediaDatasets.WikidataR2R_literals.name) || q.dataset.equals(DBpediaDatasets.WikidataR2R_objects.name)) {
        q.predicate match {

            // split type statements, some types e.g. cordinates go to separate datasets
          case "http://www.w3.org/1999/02/22-rdf-syntax-ns#type" =>
            q.value match {
              case "http://www.w3.org/2003/01/geo/wgs84_pos#SpatialThing"
                   => adjustedGraph += q.copy(dataset = DBpediaDatasets.GeoCoordinates.name)
              case _ => // This is the deafult types we get
                adjustedGraph += q.copy(dataset = DBpediaDatasets.OntologyTypes.name)
                // Generate inferred types
                context.ontology.classes.get(q.value.replace("http://dbpedia.org/ontology/", "")) match {
                  case Some(clazz) =>
                    for (cls <- clazz.relatedClasses.filter(_ != clazz))
                      adjustedGraph += new Quad(context.language, DBpediaDatasets.OntologyTypesTransitive, subjectUri, rdfType, cls.uri, page.wikiPage.sourceUri)
                  case None =>
                }
            }

          case "http://www.w3.org/2000/01/rdf-schema#subClassOf"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataR2R_ontology.name)

            // coordinates dataset
          case "http://www.w3.org/2003/01/geo/wgs84_pos#lat" | "http://www.w3.org/2003/01/geo/wgs84_pos#long" | "http://www.georss.org/georss/point"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.GeoCoordinates.name)

            //Images dataset
          case  "http://xmlns.com/foaf/0.1/thumbnail" | "http://xmlns.com/foaf/0.1/depiction" | "http://dbpedia.org/ontology/thumbnail"
                => adjustedGraph += q.copy(dataset = DBpediaDatasets.Images.name)

            // sameAs links
          case "http://www.w3.org/2002/07/owl#sameAs" =>
            // We get the commons:Creator links
            if (q.value.startsWith("http://commons.dbpedia.org")) {
              adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataNameSpaceSameAs.name)
            } else {
              adjustedGraph += q.copy(dataset = DBpediaDatasets.WikidataSameAsExternal.name)
            }

          case _ => adjustedGraph += q
        }
      } else adjustedGraph += q
    })

    adjustedGraph
  }
}