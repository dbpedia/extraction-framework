package org.dbpedia.extraction.dump

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.wikiparser.PageNode

trait DumpExtractionContext
{
    def ontology : Ontology

    def commonsSource : Source

    def language : Language

    def pageNodeSource : Traversable[PageNode]

    def articlesSource : Source

    def redirects : Redirects
}

