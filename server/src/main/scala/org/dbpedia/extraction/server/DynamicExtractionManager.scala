package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.mappings.{Mappings,Extractor}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import java.io.File
import scala.actors.Actor

/**
 * Loads all extraction context parameters (ontology pages, mapping pages, ontology) at start-up independently of which extractors are chosen.
 * Is able to update the ontology and the mappings.
 * Updates are executed in synchronized threads.
 * TODO: the synchronized blocks are too big and take too long. The computation can be done 
 * unsynchronized, just the assignment must be synchronized, and the assignment should preferably
 * be atomic, i.e. client code that uses the different fields should atomically get a holder object
 * that holds all the values. 
 */
class DynamicExtractionManager(update: (Language, Mappings) => Unit, languages : Traversable[Language], paths: Paths) 
extends ExtractionManager(languages, paths)
{
    private var _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    private var _mappingPages : Map[Language, Map[WikiTitle, PageNode]] = loadMappingPages

    private var _ontology : Ontology = loadOntology

    private var _mappings : Map[Language, Mappings] = loadMappings

    private var _extractors : Map[Language, Extractor] = loadExtractors

    def extractor(language : Language) = synchronized { _extractors(language) }

    def ontology = synchronized { _ontology }

    def ontologyPages = synchronized { _ontologyPages }

    def mappingPageSource(language : Language) = synchronized { _mappingPages(language).values }

    def mappings(language : Language) : Mappings = synchronized { _mappings(language) }

    private def asynchronous(name: String)(body: => Unit) = Actor actor synchronized { 
      val millis = System.currentTimeMillis
      body
      println(name+": "+(System.currentTimeMillis - millis)+" millis")
    }
    
    def updateAll = synchronized {
        for ((language, mappings) <- _mappings) update(language, mappings)
    }
        
    def updateOntologyPage(page : WikiPage) = asynchronous("updateOntologyPage") {
        _ontologyPages = _ontologyPages.updated(page.title, parser(page))
        _ontology = loadOntology
        _mappings = loadMappings
        _extractors = loadExtractors()
        updateAll
    }

    def removeOntologyPage(title : WikiTitle) = asynchronous("removeOntologyPage") {
        _ontologyPages = _ontologyPages - title
        _ontology = loadOntology
        _mappings = loadMappings
        _extractors = loadExtractors()
        updateAll
    }

    def updateMappingPage(page : WikiPage, language : Language) = asynchronous("updateMappingPage") {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) + ((page.title, parser(page))))
        val mappings = loadMappings(language)
        _mappings = _mappings.updated(language, mappings)
        _extractors = _extractors.updated(language, loadExtractors(language))
        update(language, mappings)
    }

    def removeMappingPage(title : WikiTitle, language : Language) = asynchronous("removeMappingPage") {
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) - title)
        val mappings = loadMappings(language)
        _mappings = _mappings.updated(language, mappings)
        _extractors = _extractors.updated(language, loadExtractors(language))
        update(language, mappings)
    }
}