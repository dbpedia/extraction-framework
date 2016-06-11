package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}
import collection.JavaConverters._
/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(model: Model) extends RMLMapping {

  def writeAsNTriples: Unit = {
    model.write(System.out, "N-TRIPLES")
  }

  def writeAsTurtle: Unit = {
    model.write(System.out, "TURTLE")
  }


}
