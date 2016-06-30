package org.dbpedia.extraction.server.resources.rml

import org.dbpedia.extraction.mappings.MappingsLoader
import org.scalatest.FunSuite
import scala.language.reflectiveCalls
import org.dbpedia.extraction.mappings.rml.util.ContextCreator
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiParser, WikiTitle}

import scala.collection.mutable.ArrayBuffer

/**
  * Testing RML template mapping factory
  */
class RMLTemplateMappingFactoryTest extends FunSuite {

    test("testCreateMapping") {

      //language
      val languageEN = Language.English

      //test files
      val pathsToXml: Array[String] = Array("../core/src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.xml",
        "../core/src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_automobile_generation.xml")

      //context
      val contexts = pathsToXml.map( path => ContextCreator.createXMLContext(path, languageEN))

      //loading template mappings
      val xmlTemplateMappings = contexts.map( context => MappingsLoader.load(context))

      //wikiparser for creating a page node
      val parser = WikiParser.getInstance()

      //creating the mappings with the factory
      val factory = new RMLTemplateMappingFactory()
      var i = 0
      for(i <- 0 until contexts.size) {
        val mapping = factory.createMapping(parser(contexts(i).mappingPageSource.head).get, languageEN, xmlTemplateMappings(i))
        println("\n\n")
        println("\n\n")
        //printing mapping content
        /*
        println("N-Triples notation: ")
        println("\n\n")
        mapping.printAsNTriples
        println("\n\n")
        */
        println("Turtle notation: ")
        println("\n\n")
        mapping.printAsTurtle
        println("\n\n")
      }

  }

}