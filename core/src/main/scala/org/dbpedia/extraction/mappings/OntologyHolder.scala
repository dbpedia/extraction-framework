package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology

trait OntologyHolder {
  def ontology: Ontology
}