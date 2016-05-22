package org.dbpedia.extraction.mappings.rml

import java.io.File

import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.mappings.{PropertyMapping, Redirects, SimplePropertyMapping, TemplateMapping}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
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

  // setting up config

  // loading ontologies
  val ontologyPath = "../ontology.xml"
  val rmlDocumentPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/infobox_person.rml"
  val ontologyFile = new File(ontologyPath)
  val ontologySource = XMLSource.fromFile(ontologyFile,Language.Mappings)
  val ontologyObject = new OntologyReader().read(ontologySource)

  // language
  val languageEN = Language.English


  // parsing rml mapping file
  val rmlMapping = RMLParser.parseFromFile(rmlDocumentPath)

  val context = new {
      def ontology: Ontology = ontologyObject
      def language: Language = languageEN
      def redirects: Redirects  = null
      def mappingDoc: RMLMapping = rmlMapping
  }

  // testing RMLMappingsLoader
  val templateMappings = RMLMappingsLoader.load(context).templateMappings

  // printing the output of the loader
  for((k,v: TemplateMapping) <- templateMappings) {
      println("Iterating over template mappings...")
      println("TemplateMapping: " + k)
      println("\tOntology class: " + v.mapToClass.name + " ("+ v.mapToClass.uri +")")
      println("\tSimple Property Mappings: ")
      for(propertyMapping: PropertyMapping <- v.mappings) {
          val simplePropertyMapping = propertyMapping.asInstanceOf[SimplePropertyMapping]
        println("\t\tProperty: ")
          println("\t\t\tTemplate property: " + simplePropertyMapping.templateProperty)
          println("\t\t\tOntology property: " + simplePropertyMapping.ontologyProperty.name +"\t("+simplePropertyMapping.ontologyProperty.uri+")")
      }
  }


}
