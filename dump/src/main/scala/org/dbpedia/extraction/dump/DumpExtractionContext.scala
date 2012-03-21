package org.dbpedia.extraction.dump

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.mappings.{Mappings, Redirects}

/**
 * TODO: remove this class. Different extractors need different resources. We should use some kind
 * of dependency injection (not necessarily a framework, Scala should be flexible enough). That
 * would also make configuration much easier and more flexible. No more loading of classes by name.
 */
trait DumpExtractionContext
{
    def ontology : Ontology

    // TODO: remove this, only used by ImageExtractor
    def commonsSource : Source

    def language : Language

    // TODO: remove this, only used by MappingExtractor
    def mappingPageSource : Traversable[PageNode]

    // TODO: remove this, only used by MappingExtractor
    def mappings : Mappings

    def articlesSource : Source

    def redirects : Redirects
}

