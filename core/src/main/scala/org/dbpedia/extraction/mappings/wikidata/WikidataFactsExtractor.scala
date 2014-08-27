package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Dataset, Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode}
import org.wikidata.wdtk.datamodel.interfaces._
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._


/**
 * Extracts Wikidata claims
 * on the form of
 * value triples:
 * <http://wikidata.dbpedia.org/resouce/Q64> <http://www.wikidata.org/entity/P625> "33.3333333 -123.433333333"
 * URI triples
 * <http://wikidata.dbpedia.org/resouce/Q64> <http://www.wikidata.org/entity/P625> <wikidata.dbpedia.org/resource/Q223>
 *
 */
class WikidataFactsExtractor(
                              context : {
                                def ontology : Ontology
                                def language : Language
                              }
                              )
  extends JsonNodeExtractor
{
  // Here we define all the ontology predicates we will use
  private val isPrimaryTopicOf = context.ontology.properties("foaf:isPrimaryTopicOf")
  private val primaryTopic = context.ontology.properties("foaf:primaryTopic")
  private val dcLanguage = context.ontology.properties("dc:language")
  private val labelProperty = context.ontology.properties("rdfs:label")

  //Ontology predicates for geocordinates
  private val typeOntProperty = context.ontology.properties("rdf:type")
  private val latOntProperty = context.ontology.properties("geo:lat")
  private val lonOntProperty = context.ontology.properties("geo:long")
  private val pointOntProperty = context.ontology.properties("georss:point")
  private val featureOntClass = context.ontology.classes("geo:SpatialThing")

  // this is where we will store the output
  val WikidataGeoLocations = new Dataset("wikidata-geo")
  override val datasets = Set(DBpediaDatasets.WikidataFacts,WikidataGeoLocations)


  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] = {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()
    val commonMediaFilesProperties = List("P10","P109","P117","P14","P15","P154","P158","P18","P181","P207","P242","P367","P368","P41","P443","P491","P51","P623","P692","P94")

    for ((statementGroup) <- page.wikiDataItem.getStatementGroups) {
      val claim = statementGroup.getStatements().get(0).getClaim()
      val property = claim.getMainSnak().getPropertyId().toString().replace("(PropertyId)", "").replace("http://data.dbpedia.org/resource/","http://www.wikidata.org/entity/")

      claim.getMainSnak() match {
        case mainSnak:ValueSnak => {
          mainSnak.getValue() match {
            case value:ItemIdValue => {
              val fact=value.toString().replace("(ItemId)","")
              quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri, null)
            }

            case value:StringValue => {
              val propID=property.replace("http://www.wikidata.org/entity/","")
              if (commonMediaFilesProperties.contains(propID)){
                val fact = "http://commons.wikimedia.org/wiki/File:" + value.toString.replace(" ","_").replace("(String)","").trim()
                quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri,null)
              } else {
                val fact = value.toString.replace("(String)","").trim()
                quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri,context.ontology.datatypes("xsd:string"))
              }
            }

            case value:TimeValue => {
              val fact = value.getYear+"-"+value.getMonth+"-"+value.getDay
              quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri,context.ontology.datatypes("xsd:date"))

            }

            case value: GlobeCoordinatesValue => {
              quads += new Quad(context.language, WikidataGeoLocations, subjectUri, typeOntProperty, featureOntClass.uri, page.wikiPage.sourceUri)
              quads += new Quad(context.language, WikidataGeoLocations, subjectUri, latOntProperty, value.getLatitude.toString, page.wikiPage.sourceUri)
              quads += new Quad(context.language, WikidataGeoLocations, subjectUri, lonOntProperty, value.getLongitude.toString, page.wikiPage.sourceUri)
              quads += new Quad(context.language, WikidataGeoLocations, subjectUri, pointOntProperty, value.getLatitude + " " + value.getLongitude, page.wikiPage.sourceUri)
            }

            case value: QuantityValue => {
              val fact = value.getNumericValue.toString
              quads += new Quad(context.language, DBpediaDatasets.WikidataFacts, subjectUri, property, fact, page.wikiPage.sourceUri,context.ontology.datatypes("xsd:float"))

            }
            case _=>
          }
        }
        case _=>
      }
    }
    quads
  }
}
