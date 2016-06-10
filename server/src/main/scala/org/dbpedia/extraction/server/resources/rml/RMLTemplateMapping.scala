package org.dbpedia.extraction.server.resources.rml

import org.apache.jena.rdf.model.Model

/**
  * RML Template Mapping converted from the DBpedia mappings
  */
class RMLTemplateMapping(model: Model) extends RMLMapping {

  def write = {
    model.write(System.out, "N-TRIPLES")
  }

}
