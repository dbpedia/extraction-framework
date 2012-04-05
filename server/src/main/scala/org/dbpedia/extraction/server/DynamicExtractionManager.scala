package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import java.io.File

/**
 * Loads all extraction context parameters (ontology pages, mapping pages, ontology) at start-up independently of which extractors are chosen.
 * Is able to update the ontology and the mappings.
 * Updates are executed in synchronized threads. TODO: find nicer solution for the parallelized access. Use a thread-pool. Or actors?
 */
class DynamicExtractionManager(languages : Traversable[Language], extractors : List[Class[_ <: Extractor]], ontologyFile : File, mappingsDir : File) extends ExtractionManager(languages, extractors, ontologyFile, mappingsDir)
{
    @volatile private var _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    @volatile private var _mappingPages : Map[Language, Map[WikiTitle, PageNode]] = loadMappingPages

    @volatile private var _ontology : Ontology = loadOntology

    @volatile private var _mappings : Map[Language, Mappings] = loadMappings

    @volatile private var _extractors : Map[Language, Extractor] = loadExtractors


    def extractor(language : Language) = _extractors(language)

    def ontology = _ontology

    def ontologyPages = _ontologyPages

    def mappingPageSource(language : Language) = _mappingPages(language).values

    def mappings(language : Language) : Mappings = _mappings(language)

    def updateOntologyPage(page : WikiPage)
    {
        new Thread()
        {
            override def run()
            {
                _updateOntologyPage(page)
            }
        }.start()
    }

    private def _updateOntologyPage(page : WikiPage)
    {
        this.synchronized
        {
            _ontologyPages = _ontologyPages.updated(page.title, parser(page))
            _ontology = loadOntology
            _mappings = loadMappings
            _extractors = loadExtractors
        }
    }

    def removeOntologyPage(title : WikiTitle)
    {
        new Thread()
        {
            override def run()
            {
                _removeOntologyPage(title)
            }
        }.start()
    }

    private def _removeOntologyPage(title : WikiTitle)
    {
        this.synchronized
        {
            _ontologyPages = _ontologyPages - title
            _ontology = loadOntology
            _mappings = loadMappings
            _extractors = loadExtractors
        }
    }

    def updateMappingPage(page : WikiPage, language : Language)
    {
        new Thread
        {
            override def run()
            {
                _updateMappingPage(page, language)
            }
        }.start()
    }

    private def _updateMappingPage(page : WikiPage, language : Language)
    {
        this.synchronized
        {
            _mappingPages = _mappingPages.updated(language, _mappingPages(language) + ((page.title, parser(page))))
            _mappings = _mappings.updated(language, loadMapping(language))
            _extractors = _extractors.updated(language, loadExtractor(language))
        }
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        new Thread
        {
            override def run()
            {
                _removeMappingPage(title, language)
            }
        }.start()
    }

    private def _removeMappingPage(title : WikiTitle, language : Language)
    {
        this.synchronized
        {
            _mappingPages = _mappingPages.updated(language, _mappingPages(language) - title)
            _mappings = _mappings.updated(language, loadMapping(language))
            _extractors = _extractors.updated(language, loadExtractor(language))
        }
    }

}