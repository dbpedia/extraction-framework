package org.dbpedia.extraction.mappings.rml

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, TemplateMappingsPrinter}
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

/**
  * Testing RMLMappingsLoader
  */

@RunWith(classOf[JUnitRunner])
class RMLMappingsLoaderTest extends FlatSpec with Matchers
{

  // language
  val languageEN = Language.English

  // xml mapping file location
  val xmlMappingPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.xml"

  // rml mapping file location
  val rmlDocumentPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.rml"

  // testing RMLMappingsLoader && MappingsLoader
  val rmlTemplateMappings = RMLMappingsLoader.load(ContextCreator.createRMLContext(rmlDocumentPath, languageEN)).templateMappings
  val xmlTemplateMappings = MappingsLoader.load(ContextCreator.createXMLContext(xmlMappingPath, languageEN)).templateMappings

  // printing output of both versions
  println("**** RML MAPPINGS ****")
  TemplateMappingsPrinter.printTemplateMappings(rmlTemplateMappings)

  println("\n\n**** XML MAPPINGS ****")
  TemplateMappingsPrinter.printTemplateMappings(xmlTemplateMappings)

}
