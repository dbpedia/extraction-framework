package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language

/**
 * Convenience methods that help to unclutter code. 
 */
object QuadBuilder {

  def apply(language: Language, dataset: Dataset, predicate: OntologyProperty, datatype: Datatype) (subject: String, value: String, context: String) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
  
  def apply(language: Language, dataset: Dataset, predicate: OntologyProperty) (subject: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
  
  def stringPredicate(language: Language, dataset: Dataset, predicate: String) (subject: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
  
  def apply(language: Language, dataset: Dataset) (subject: String, predicate: String, value: String, context: String, datatype: Datatype) =
    new Quad(language, dataset, subject, predicate, value, context, datatype)
}
