package org.dbpedia.extraction.server.resources.rml.model

import java.io.StringWriter

import org.apache.jena.rdf.model.{Model, ModelFactory, Resource, Statement}
import org.dbpedia.extraction.wikiparser.WikiTitle

/**
  * Class that is a wrapper around a Jena model that adds behaviour
  */
class ModelWrapper() {

  val model: Model = ModelFactory.createDefaultModel()

  def printAsNTriples: Unit =
  {
    model.write(System.out, "N-TRIPLES")
  }

  def printAsTurtle: Unit =
  {
    model.write(System.out, "TURTLE")
  }

  def writeAsTurtle: String =
  {
    val out = new StringWriter()
    model.write(out, "TURTLE")
    out.toString
  }

  def writeAsNTriples: String =
  {
    val out = new StringWriter()
    model.write(out, "NTRIPLES")
    out.toString
  }

}
