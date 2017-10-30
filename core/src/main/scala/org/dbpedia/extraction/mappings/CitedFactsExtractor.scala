package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.{postfixOps, reflectiveCalls}
import scala.reflect.ClassTag

/**
  * ewxperimental extractor that extracts infobox facts that have a citation on the same line and places the citation in the context
 */
@SoftwareAgentAnnotation(classOf[CitedFactsExtractor], AnnotationType.Extractor)
class CitedFactsExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def mappings : Mappings
    def redirects : Redirects
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  }
)
extends WikiPageExtractor {
  private val hybridInfoboxExtractor = new HybridRawAndMappingExtractor(context)
  private val citationExtractor = new CitationExtractor(context)
  private val language = context.language

  val dataset = DBpediaDatasets.CitatedFacts

  override val datasets = Set(dataset)

  override def extract(wikiPage: WikiPage, subjectUri: String): Seq[Quad] = {
    //Only extract abstracts for pages from the Main namespace
    if(wikiPage.title.namespace != Namespace.Main) return Seq.empty

    val pageNodeOption = wikiPage match{
      case page: WikiPage => WikiParser.getInstance().apply(page, context.redirects)
      case _ => throw new IllegalArgumentException("Input is no WikiPage")
    }

    if (pageNodeOption.isEmpty)
      return Seq.empty


    // map with line -> Quads for that line
    val infoboxMap =
      hybridInfoboxExtractor.extract(pageNodeOption.get, subjectUri)
        .filterNot(_.dataset.equals(DBpediaDatasets.OntologyTypes.encoded))
        .filterNot(_.dataset.equals(DBpediaDatasets.OntologyTypesTransitive.encoded))
        .filterNot(_.dataset.equals(DBpediaDatasets.InfoboxPropertyDefinitions.encoded))
        .filterNot(_.dataset.equals(DBpediaDatasets.InfoboxPropertiesMapped.encoded))
        .map(q => (getAbsoluteLineNumber(q), q))
        .groupBy(_._1)
        .mapValues(_.map(_._2))

    // no need to check articles without infobox data
    if (infoboxMap.isEmpty) return Seq.empty

    // citations Map with line -> citation (without ditation data, only links)
    val citationMap =
      citationExtractor.extract(wikiPage, subjectUri)
        .filterNot(_.dataset.equals(DBpediaDatasets.CitationData.encoded))
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
            graph += q.copy(dataset = dataset.encoded, context = c.subject)
          }
        }
      }
    }
    graph
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