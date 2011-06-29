package org.dbpedia.extraction.server

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.sources.{MemorySource, Source}

/**
 * Holds methods required by the extractors. This object is to be injected into the extractors as constructor argument.
 */
class ServerExtractionContext(lang : Language, extractionManager : ExtractionManager)
{
    def ontology : Ontology = extractionManager.ontology  //language-independent

    def language : Language = lang

    def mappingsSource : Source = extractionManager.mappingSource(lang)

    def redirects : Redirects = _redirects

    private lazy val _redirects = Redirects.load(new MemorySource, lang)

    // not needed in server: commonsSource
    // not needed in server: articlesSource
}