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
 *  Combines the raw infobox and mappings extractor and tries to split the triples of the raw infobox extractor
  *  in triples that were mapped from the mappings extractors and triples that were not mapped
 */
@SoftwareAgentAnnotation(classOf[HybridRawAndMappingExtractor], AnnotationType.Extractor)
class HybridRawAndMappingExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def mappings : Mappings
    def redirects : Redirects
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  }
)
extends PageNodeExtractor {
  private val rawinfoboxExtractor = new InfoboxExtractor(context)
  private val mappingExtractor = new MappingExtractor(context)
  private val language = context.language

  private val enableMappingExtractor = Namespace.mappingLanguages.contains(language)

  override val datasets = (rawinfoboxExtractor.datasets ++ mappingExtractor.datasets) + DBpediaDatasets.InfoboxPropertiesMapped

  override def extract(page: PageNode, subjectUri: String): Seq[Quad] = {


    val mappedGraph =
      // check if the mappings exist for a language
      if (enableMappingExtractor) mappingExtractor.extract(page, subjectUri)
      else Seq.empty

    val rawGraph = rawinfoboxExtractor.extract(page, subjectUri)

    return mappedGraph ++ splitRawGraph(rawGraph, mappedGraph)
  }

  private def splitRawGraph(rawGraph: Seq[Quad], mappedGraph: Seq[Quad]): Seq[Quad] = {
    // we store an index of (infobox, property, absolute-line) for each mapped fact and split raw facts with the same index
    val mappedIndex = mappedGraph.flatMap( q => extractTemplatePropertyAndLine(q.context)).toSet

    val newRawGraph = new ArrayBuffer[Quad]
    rawGraph
      .foreach( q => {
        val tuple = extractTemplatePropertyAndLine(q.context)
        if (! q.dataset.equals(DBpediaDatasets.InfoboxProperties.encoded)) {
          newRawGraph += q.copy()
        }
        else if ( tuple.isDefined && mappedIndex.contains( tuple.get)) {
          newRawGraph += q.copy(context = q.context + "&mapped=", dataset = DBpediaDatasets.InfoboxPropertiesMapped.encoded)
        }
        else {
          newRawGraph += q.copy(context = q.context + "&unmapped=")
        }
    })

    newRawGraph
  }

  private def extractTemplatePropertyAndLine(quadContext: String): Option[Tuple3[String, String, String]] ={
    val splitted = quadContext.split('#') // go to the fragment
    if (splitted.length != 2 ) {
      None
    } else {
      // create a map from parameters
      val parametersMap = splitted(1).split('&').map(_ split "=") collect { case Array(k, v) => (k, v) } toMap

      val templateLabel = "template"
      val propertyLabel = "property"
      val lineLabel = "absolute-line"
      if (parametersMap.contains(templateLabel) && parametersMap.contains(propertyLabel) && parametersMap.contains(lineLabel)) {
        Some(
          parametersMap.get(templateLabel).get,
          parametersMap.get(propertyLabel).get,
          parametersMap.get(lineLabel).get)
      }
      else None
    }
  }
}