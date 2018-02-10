package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{PageNode, PropertyNode}

import scala.reflect.ClassTag

@SoftwareAgentAnnotation(classOf[InfoboxExtractor], AnnotationType.Extractor)
class InfoboxExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
  }
) extends ExtendedInfoboxExtractor(context) {


  override val datasets = Set(DBpediaDatasets.InfoboxProperties, DBpediaDatasets.InfoboxTest, DBpediaDatasets.InfoboxPropertyDefinitions)

  /**
    * Will try to execute every parser for the given property node.
    * Depending on the switch parameter, this will return either
    * a List of the first successful result or a list of all successful parse results
    *
    * @param node - the property node to parse
    * @return
    */
  override protected def extractValue(node: PropertyNode) = super.extractValue(node, extractOnlyFirstSuccessfulResult = true)

  override def extract(node: PageNode, subjectUri: String) =
    super.extract(node, subjectUri)
}
