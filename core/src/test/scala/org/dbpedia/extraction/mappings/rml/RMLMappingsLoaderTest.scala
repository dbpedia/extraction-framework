package org.dbpedia.extraction.mappings.rml

import java.io.File
import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiPage, WikiSource, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, TemplateNode}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Testing RMLMappingsLoader
  */

@RunWith(classOf[JUnitRunner])
class RMLMappingsLoaderTest extends FlatSpec with Matchers
{

  // setting up config

  // language
  val languageEN = Language.English

  // loading ontologies
  val ontologyPath = "../ontology.xml"
  val rmlDocumentPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.rml"
  val ontologyFile = new File(ontologyPath)
  val ontologySource = XMLSource.fromFile(ontologyFile,Language.Mappings)
  val ontologyObject = new OntologyReader().read(ontologySource)

  // loading xml mapping file
  val xmlMappingPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.xml"
  val xmlMappingFile = new File(xmlMappingPath)
  val xmlMapping = XMLSource.fromFile(xmlMappingFile, Language.Mappings)

  // loading rml mapping file
  val rmlMapping = RMLParser.parseFromFile(rmlDocumentPath)

  // setting context
  val rmlContext = new {
      def ontology: Ontology = ontologyObject
      def language: Language = languageEN
      def redirects: Redirects  = null
      def mappingDoc: RMLMapping = rmlMapping
  }
  val xmlContext = new {
    def ontology: Ontology = ontologyObject
    def language: Language = languageEN
    def redirects: Redirects  = null
    def mappingPageSource: Traversable[WikiPage] = xmlMapping
  }

  // testing RMLMappingsLoader && MappingsLoader
  val rmlTemplateMappings = RMLMappingsLoader.load(rmlContext).templateMappings
  val xmlTemplateMappings = MappingsLoader.load(xmlContext).templateMappings

  // printing output of both versions
  println("**** RML MAPPINGS ****")
  TemplateMappingsPrinter.printTemplateMappings(rmlTemplateMappings)
  println("\n\n**** XML MAPPINGS ****")
  TemplateMappingsPrinter.printTemplateMappings(xmlTemplateMappings)



}
