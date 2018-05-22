package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.mappings.{Disambiguations, Redirects}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

trait SparkExtractionContext {
  def ontology : Ontology

  def language : Language

  def redirects : Redirects

  def disambiguations : Disambiguations
}
