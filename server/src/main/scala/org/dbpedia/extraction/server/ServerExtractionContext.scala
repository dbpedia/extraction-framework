package org.dbpedia.extraction.server

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.MemorySource
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.mappings.{Mappings, Redirects}

/**
 * Holds methods required by the extractors. This object is to be injected into the extractors as constructor argument.
 */
class ServerExtractionContext(lang : Language, extractionManager : ExtractionManager)
{
    def ontology : Ontology = extractionManager.ontology

    def language : Language = lang

    def redirects : Redirects = _redirects

    private lazy val _redirects = new Redirects(Map())

    def mappingPageSource : Traversable[PageNode] = extractionManager.mappingPageSource(lang)

    def mappings : Mappings = extractionManager.mappings(lang)

    // not needed in server: commonsSource
    // not needed in server: articlesSource
}