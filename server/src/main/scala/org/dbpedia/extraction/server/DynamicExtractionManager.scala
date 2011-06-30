package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

/**
 * Loads all extraction context parameters (ontology pages, mapping pages, ontology) at start-up independently of which extractors are chosen.
 * Is able to update the ontology and the mappings.
 */
class DynamicExtractionManager(languages : Set[Language], extractors : List[Class[Extractor]]) extends ExtractionManager(languages, extractors)
{
    @volatile private var _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    @volatile private var _mappingPages : Map[Language, Map[WikiTitle, PageNode]] = loadMappingPages

    @volatile private var _ontology : Ontology = loadOntology

    @volatile private var _extractors : Map[Language, Extractor] = loadExtractors


    def extractor(language : Language) = _extractors(language)

    def ontology = _ontology

    def ontologyPages = _ontologyPages

    def updateOntologyPage(page : WikiPage)
    {
        _ontologyPages = _ontologyPages.updated(page.title, parser(page))
        _ontology = loadOntology
        _extractors = loadExtractors
    }

    def removeOntologyPage(title : WikiTitle)
    {
        _ontologyPages = _ontologyPages - title
        _ontology = loadOntology
        _extractors = loadExtractors
    }

    def mappingPageSource(language : Language) = _mappingPages(language).values

    def updateMappingPage(page : WikiPage, language : Language)
    {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) + ((page.title, parser(page))))
        _extractors = _extractors.updated(language, loadExtractor(language))
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) - title)
        _extractors = _extractors.updated(language, loadExtractor(language))
    }

}