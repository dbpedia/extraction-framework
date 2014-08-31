package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.{OntologyProperty, Ontology}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.destinations.{Quad, DBpediaDatasets}
import org.dbpedia.extraction.wikiparser.{JsonNode}
import org.wikidata.wdtk.datamodel.interfaces._
import collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.collection.JavaConversions._

/**
* wikidata to dbpedia mapping for  1 to 1 (replace case)
* on the form of
* value triples:
* <http://wikidata.dbpedia.org/resouce/Qxxx> <http://dbpedia.org/ontology/Y> "value"
* URI triples
* <http://wikidata.dbpedia.org/resouce/Qxx> <http://dbpedia.org/ontology/Zy> <http://data.dbpedia.org/resource/Qzz>
*
*/
class WikidataMappedFactsExtractor(
                         context : {
                           def ontology : Ontology
                           def language : Language
                         }
                         )
  extends JsonNodeExtractor
{

  // this is where we will store the output
  override val datasets = Set(DBpediaDatasets.WikidataMappedFacts)
  val commonMediaFilesProperties = List("P10","P109","P117","P14","P15","P154","P158","P18","P181","P207","P242","P367",
    "P368","P41","P443","P491","P51","P623","P692","P94")

  override def extract(page : JsonNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    // This array will hold all the triples we will extract
    val quads = new ArrayBuffer[Quad]()

    for ((statementGroup) <- page.wikiDataItem.getStatementGroups) {
      val claim = statementGroup.getStatements().get(0).getClaim()
      val property = claim.getMainSnak().getPropertyId().toString().replace("(PropertyId)", "")

      claim.getMainSnak() match {
        case mainSnak: ValueSnak => {
          mainSnak.getValue() match {
            case value: ItemIdValue => {
              getDBpediaSameasProperties(property).foreach{ dbProp =>
                val fact = value.toString().replace("(ItemId)", "")
                quads += new Quad(context.language, DBpediaDatasets.WikidataMappedFacts, subjectUri, dbProp, fact, page.wikiPage.sourceUri, null)
              }
            }
            case value:StringValue => {
              val fact = value.toString.replace("(String)","").trim()
              val propID=property.replace("http://data.dbpedia.org/resource/","")
              getDBpediaSameasProperties(property).foreach{ dbProp =>
                if (commonMediaFilesProperties.contains(propID)){
                  val fileURI = "http://commons.wikimedia.org/wiki/File:" + fact.toString.replace(" ","_")
                  quads += new Quad(null, DBpediaDatasets.WikidataMappedFacts, subjectUri,dbProp.uri,fileURI, page.wikiPage.sourceUri, null)
                } else {
                  quads += new Quad(null, DBpediaDatasets.WikidataMappedFacts, subjectUri,dbProp, fact, page.wikiPage.sourceUri)
                }
              }
            }
            case value:TimeValue => {
              val fact = value.getYear+"-"+value.getMonth+"-"+value.getDay
              getDBpediaSameasProperties(property).foreach { dbProp =>
                quads += new Quad(context.language, DBpediaDatasets.WikidataMappedFacts, subjectUri, dbProp,fact, page.wikiPage.sourceUri)
              }
            }
            case value: QuantityValue => {
              val fact = value.getNumericValue.toString
              getDBpediaSameasProperties(property).foreach { dbProp =>
                quads += new Quad(context.language, DBpediaDatasets.WikidataMappedFacts, subjectUri, dbProp,fact, page.wikiPage.sourceUri, context.ontology.datatypes("xsd:float"))
              }
            }
            case _ =>
          }
        }
        case _ =>
      }
    }

    quads
  }

  def getDBpediaSameasProperties(property:String) : Set[OntologyProperty] =
  {
    val p = property.replace("http://data.dbpedia.org/resource","http://wikidata.dbpedia.org/resource")
    var properties = Set[OntologyProperty]()
    context.ontology.equivalentPropertiesMap.foreach({map =>
      if (map._1.toString.matches(p)) {
        map._2.foreach { mappedProp =>
          properties += mappedProp
        }
      }
    })
    properties
  }
}


