package org.dbpedia.extraction.server.resources.rml

import java.io.StringWriter

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}

import collection.JavaConverters._
/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(model: Model) extends RMLMapping {

  def printAsNTriples: Unit = {
    model.write(System.out, "N-TRIPLES")
  }

  def printAsTurtle: Unit = {
    model.write(System.out, "TURTLE")
  }

  def writeAsTurtle: String = {
    val out = new StringWriter()
    model.write(out, "TURTLE")
    out.toString
  }


}
