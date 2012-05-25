package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language

/**
 * Convenience methods that help to unclutter code. 
 */
object QuadBuilder {

  def apply(language: Language, dataset: Dataset, predicate: OntologyProperty, datatype: Datatype = null)(subject: String, value: String, context: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
  
}
