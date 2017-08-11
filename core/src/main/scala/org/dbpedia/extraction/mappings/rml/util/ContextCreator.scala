package org.dbpedia.extraction.mappings.rml.util

import java.io.File


import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.util.Language

/**
  * Generate context for xml and rml
  */
object ContextCreator {

  private lazy val ontologyPath = "../ontology.xml"
  private lazy val ontologyFile = new File(ontologyPath)
  private lazy val ontologySource = XMLSource.fromFile(ontologyFile,Language.Mappings)

  lazy val ontologyObject = new OntologyReader().read(ontologySource)

  def createXMLContext(pathToXML: String, lang: Language):
  {
    def ontology: Ontology
    def language: Language
    def redirects: Redirects
    def mappingPageSource: Traversable[WikiPage]
  } =
  {
    val xmlMappingFile = new File(pathToXML)
    val xmlMapping = XMLSource.fromFile(xmlMappingFile, Language.Mappings)
    new {
      def ontology: Ontology = ontologyObject

      def language: Language = lang

      def redirects: Redirects = null

      def mappingPageSource: Traversable[WikiPage] = xmlMapping
    }
  }

  def createOntologyContext(ontologyParam: Ontology = ontologyObject):
  {
    def ontology: Ontology
  } =
  {
    new {
      def ontology: Ontology = ontologyParam
    }
  }

}
