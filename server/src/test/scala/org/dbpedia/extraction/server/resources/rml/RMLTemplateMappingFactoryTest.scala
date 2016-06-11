package org.dbpedia.extraction.server.resources.rml

import org.dbpedia.extraction.mappings.MappingsLoader
import org.scalatest.FunSuite
import org.dbpedia.extraction.mappings.rml.util.ContextCreator
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiParser, WikiTitle}

/**
  * Testing RML template mapping factory
  */
class RMLTemplateMappingFactoryTest extends FunSuite {

    test("testCreateMapping") {

      val languageEN = Language.English
      val pathToXml = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.xml"

      val context = ContextCreator.createXMLContext(pathToXml, languageEN)
      val xmlTemplateMappings = MappingsLoader.load(context)
      val parser = WikiParser.getInstance()

      val factory = new RMLTemplateMappingFactory()
      val mapping : RMLTemplateMapping = factory.createMapping(parser(context.mappingPageSource.head).get, languageEN, xmlTemplateMappings)

      println("N-Triples notation: ")
      mapping.writeAsTriples
      println("\n\n")
      println("Turtle notation: ")
      mapping.writeAsTurtle
      println("\n\n")
      println("Pretty Turtle notation: ")
      mapping.writeAsPrettyTurtle
  }

}