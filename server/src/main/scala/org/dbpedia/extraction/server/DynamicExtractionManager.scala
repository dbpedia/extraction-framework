package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology

/**
 * Loads all extraction context parameters (ontology pages, mapping pages, ontology) at start-up independently of which extractors are chosen.
 * Is able to update the ontology and the mappings.
 */
class DynamicExtractionManager(languages : Set[Language], extractors : List[Class[Extractor]]) extends ExtractionManager(languages, extractors)
{
    @volatile private var _ontologyPages : Map[WikiTitle, WikiPage] = loadOntologyPages

    @volatile private var _mappingPages : Map[Language, Map[WikiTitle, WikiPage]] = loadMappingPages

    @volatile private var _ontology : Ontology = loadOntology

    @volatile private var _extractors : Map[Language, Extractor] = loadExtractors


    def extractor(language : Language) = _extractors(language)

    def ontology = _ontology

    def ontologyPages = _ontologyPages

    def ontologyPages_= (pages : Map[WikiTitle, WikiPage])
    {
        _ontologyPages = pages
        _ontology = loadOntology
        _extractors = loadExtractors
    }

    def mappingPages(language : Language) = _mappingPages(language)

    def updateMappingPage(page : WikiPage, language : Language)
    {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) + ((page.title, page)))
        _extractors = _extractors.updated(language, loadExtractor(language))
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) - title)
        _extractors = _extractors.updated(language, loadExtractor(language))
    }

}