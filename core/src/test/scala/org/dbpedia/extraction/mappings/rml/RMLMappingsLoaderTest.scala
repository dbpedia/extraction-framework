package org.dbpedia.extraction.mappings.rml

import java.io.File

import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.mappings.Redirects
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

  //setting up config

  //loading ontologies
  val ontologyPath = "../ontology.xml"
  val rmlDocumentPath = "src/test/resources/org/dbpedia/extraction/mappings/rml/test.rml"
  val ontologyFile = new File(ontologyPath)
  val ontologySource = XMLSource.fromFile(ontologyFile,Language.Mappings)
  val ontologyObject = new OntologyReader().read(ontologySource)
  val rmlMapping = RMLParser.parseFromFile(rmlDocumentPath)

  val context = new {
      def ontology: Ontology = ontologyObject
      def language: Language = null
      def redirects: Redirects  = null
      def mappingDoc: RMLMapping = rmlMapping
  }

  //testing RMLMappingsLoader
  RMLMappingsLoader.load(context)


}
