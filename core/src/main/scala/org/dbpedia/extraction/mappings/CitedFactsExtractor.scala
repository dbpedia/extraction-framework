package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.language.{postfixOps, reflectiveCalls}

/**
  * ewxperimental extractor that extracts infobox facts that have a citation on the same line and places the citation in the context
 */
class CitedFactsExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def mappings : Mappings
    def redirects : Redirects
  }
)
extends WikiPageExtractor {
  private val hybridInfoboxExtractor = new HybridRawAndMappingExtractor(context)
  private val citationExtractor = new CitationExtractor(context)
  private val language = context.language

  val dataset = new Dataset("citedFacts")

  override val datasets = Set(dataset)

  override def extract(wikiPage: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {

    //Only extract abstracts for pages from the Main namespace
    if(wikiPage.title.namespace != Namespace.Main) return Seq.empty

    val pageNodeOption = WikiParser.getInstance().apply(wikiPage)
    if (pageNodeOption.isEmpty) return Seq.empty


    // map with line -> Quads for that line
    val infoboxMap =
      hybridInfoboxExtractor.extract(pageNodeOption.get, subjectUri, pageContext)
        .filterNot(_.dataset.equals(DBpediaDatasets.OntologyTypes.name))
        .filterNot(_.dataset.equals(DBpediaDatasets.OntologyTypesTransitive.name))
        .filterNot(_.dataset.equals(DBpediaDatasets.InfoboxPropertyDefinitions.name))
        .filterNot(_.dataset.equals(DBpediaDatasets.InfoboxPropertiesMapped.name))
        .map(q => (getAbsoluteLineNumber(q), q))
        .groupBy(_._1)
        .mapValues(_.map(_._2))

    // no need to check articles without infobox data
    if (infoboxMap.isEmpty) return Seq.empty

    // citations Map with line -> citation (without ditation data, only links)
    val citationMap =
      citationExtractor.extract(wikiPage, subjectUri, pageContext)
        .filterNot(_.dataset.equals(DBpediaDatasets.CitationData.name))
        .map(q => (getAbsoluteLineNumber(q), q))
        .groupBy(_._1)
        .mapValues(_.map(_._2))

    // no need to check without citation data
    if (citationMap.isEmpty) return Seq.empty

    val graph = new ArrayBuffer[Quad]
    for ((line, quads) <- infoboxMap) {
      // hack (quads.size <= 4) to exclude one line temnplates with references like {{See_also}}
      if (citationMap.contains(line) && quads.size <= 4) {
        for (c <- citationMap.get(line).get) {
          for (q <- quads) {
            graph += q.copy(dataset = dataset.name, context = c.subject)
          }
        }
      }
    }

    return graph
  }

  private def getAbsoluteLineNumber(quad: Quad): Int = {
    val index = quad.context.indexOf("#absolute-line=")
    try {
      val str = quad.context.substring(index+15)
      val endIndex = str.indexOf("&")
      val line =
        if (endIndex < 0) str.toInt
        else str.substring(0,endIndex).toInt

      line
    } catch {
      case e: NumberFormatException => -1
    }
  }


}