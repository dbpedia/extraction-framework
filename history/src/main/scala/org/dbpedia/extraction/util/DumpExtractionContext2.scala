package org.dbpedia.extraction.util

import org.dbpedia.extraction.mappings.{Disambiguations, Mappings, Redirects, Redirects2}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source2
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPageWithRevisions

/**
 * TODO: remove this class. Different extractors need different resources. We should use some kind
 * of dependency injection (not necessarily a framework, Scala should be flexible enough). That
 * would also make configuration much easier and more flexible. No more loading of classes by name.
 * 
 * Problems with the current approach:
 * - unflexible
 * - we lose static type safety because of 
 *   - reflection when the extractor objects are created
 *   - structural types in extractor constructors
 */
trait DumpExtractionContext2
{
    def ontology : Ontology


    def language : Language


    def articlesSource : Source2

    def redirects : Redirects2

    def disambiguations : Disambiguations
}
