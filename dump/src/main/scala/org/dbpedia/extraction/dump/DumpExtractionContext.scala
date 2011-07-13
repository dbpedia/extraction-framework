package org.dbpedia.extraction.dump

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.mappings.{Mappings, Redirects}

trait DumpExtractionContext
{
    def ontology : Ontology

    def commonsSource : Source

    def language : Language

    def mappingPageSource : Traversable[PageNode]

    def mappings : Mappings

    def articlesSource : Source

    def redirects : Redirects
}

