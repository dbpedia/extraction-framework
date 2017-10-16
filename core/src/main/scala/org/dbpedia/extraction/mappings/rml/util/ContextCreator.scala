package org.dbpedia.extraction.mappings.rml.util

import java.io.File

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.reflect.ClassTag

/**
  * Generate context for xml and rml
  */
object ContextCreator {

  private val ontologyPath = "../ontology.xml"
  private val ontologyFile = new File(ontologyPath)
  private val ontologySource = XMLSource.fromFile(ontologyFile,Language.Mappings)
  private val ontologyObject = new OntologyReader().read(ontologySource)

  def createXMLContext(pathToXML: String, lang: Language):
  {
    def ontology: Ontology
    def language: Language
    def redirects: Redirects
    def mappingPageSource: Traversable[WikiPage]
    def recorder[T: ClassTag]: ExtractionRecorder[T]
  } =
  {
    val xmlMappingFile = new File(pathToXML)
    val xmlMapping = XMLSource.fromFile(xmlMappingFile, Language.Mappings)
    new {
      def ontology: Ontology = ontologyObject

      def language: Language = lang

      def redirects: Redirects = null

      def mappingPageSource: Traversable[WikiPage] = xmlMapping

      def recorder[T: ClassTag] = null.asInstanceOf[ExtractionRecorder[T]]
    }
  }

}
