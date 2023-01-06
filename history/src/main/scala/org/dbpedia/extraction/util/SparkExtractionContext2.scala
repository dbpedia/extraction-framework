package org.dbpedia.extraction.util

import org.dbpedia.extraction.mappings.{Disambiguations, Redirects, Redirects2}
import org.dbpedia.extraction.ontology.Ontology

trait SparkExtractionContext2 {
  def ontology : Ontology

  def language : Language

  def redirects : Redirects2

  def disambiguations : Disambiguations
}
