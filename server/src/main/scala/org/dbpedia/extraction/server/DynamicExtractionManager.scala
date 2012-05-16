package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.mappings.{Mappings,RootExtractor}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import java.io.File
import scala.actors.Actor

/**
 * Loads all extraction context parameters (ontology pages, mapping pages, ontology, extractors).
 * Is able to update the ontology and the mappings.
 * Updates are executed in synchronized threads.
 * 
 * TODO: the synchronized blocks are too big and take too long. The computation can be done 
 * unsynchronized, just the assignment must be synchronized, and the assignment should preferably
 * be atomic, i.e. client code that uses the different fields should atomically get a holder object
 * that holds all the values. Problem: the current class structure hardly allows this -
 * mappingPageSource is called by loadMappings in the base class, 
 * ontologyPages is called by loadOntology in the base class.
 */
class DynamicExtractionManager(update: (Language, Mappings) => Unit, languages : Traversable[Language], paths: Paths) 
extends ExtractionManager(languages, paths)
{
    // TODO: remove this field. Clients should get the ontology pages directly from the
    // mappings wiki, not from here. We don't want to keep all ontology pages in memory.
    private var _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    private var _ontology : Ontology = loadOntology

    // TODO: remove this field. Clients should get the mapping pages directly from the
    // mappings wiki, not from here. We don't want to keep all mapping pages in memory.
    private var _mappingPages : Map[Language, Map[WikiTitle, PageNode]] = loadMappingPages

    private var _mappings : Map[Language, Mappings] = loadMappings

    private var _extractors : Map[Language, RootExtractor] = loadExtractors

    def extractor(language : Language) = synchronized { _extractors(language) }

    def ontology = synchronized { _ontology }

    // TODO: remove this method, refactor base class. Clients should get the ontology pages directly 
    // from the mappings wiki, not from here. We don't want to keep all ontology pages in memory.
    def ontologyPages = synchronized { _ontologyPages }

    // TODO: remove this method, refactor base class. Clients should get the mapping pages directly 
    // from the mappings wiki, not from here. We don't want to keep all mapping pages in memory.
    def mappingPageSource(language : Language) = synchronized { _mappingPages(language).values }

    def mappings(language : Language) : Mappings = synchronized { _mappings(language) }

    // TODO: don't start a new actor for each call, start one actor when this object is loaded
    // and send messages to it.
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

    // TODO: throw exception if page did not exist?
    def removeOntologyPage(title : WikiTitle) = asynchronous("removeOntologyPage") {
        _ontologyPages = _ontologyPages - title
        _ontology = loadOntology
        _mappings = loadMappings
        _extractors = loadExtractors()
        updateAll
    }

    def updateMappingPage(page : WikiPage, language : Language) = asynchronous("updateMappingPage") {
        // TODO: use mutable maps. makes the next line simpler, and we need synchronization anyway.
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) + ((page.title, parser(page))))
        val mappings = loadMappings(language)
        _mappings = _mappings.updated(language, mappings)
        _extractors = _extractors.updated(language, loadExtractors(language))
        update(language, mappings)
    }

    // TODO: throw exception if page did not exist?
    def removeMappingPage(title : WikiTitle, language : Language) = asynchronous("removeMappingPage") {
        // TODO: use mutable maps. makes the next line simpler, and we need synchronization anyway.
        _mappingPages = _mappingPages.updated(language, _mappingPages(language) - title)
        val mappings = loadMappings(language)
        _mappings = _mappings.updated(language, mappings)
        _extractors = _extractors.updated(language, loadExtractors(language))
        update(language, mappings)
    }
}