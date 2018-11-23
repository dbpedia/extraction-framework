package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.mappings.{Disambiguations, Mappings, Redirects}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

trait SparkExtractionContext {
  def ontology : Ontology

  def language : Language

  def redirects : Redirects

  def disambiguations : Disambiguations
}
