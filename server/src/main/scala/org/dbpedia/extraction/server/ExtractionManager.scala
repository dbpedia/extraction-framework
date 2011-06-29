package org.dbpedia.extraction.server

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import xml.Elem
import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.destinations.{Graph, Destination}
import org.dbpedia.extraction.sources.{WikiSource, Source, MemorySource, WikiPage}
import java.net.URL
import org.dbpedia.extraction.wikiparser.{WikiParser, WikiTitle}
import org.dbpedia.extraction.mappings._

/**
 * Base class for extraction managers.
 * Subclasses can either support updating the ontology and/or mappings,
 * or they can support lazy loading of context parameters.
 */

abstract class ExtractionManager(languages : Set[Language], extractors : List[Class[Extractor]])
{
    private val logger = Logger.getLogger(classOf[ExtractionManager].getName)


    def extractor(language : Language) : Extractor

    def ontology : Ontology

    def ontologyPages : Map[WikiTitle, WikiPage]

    def ontologyPages_= (pages : Map[WikiTitle, WikiPage])

    def mappingPages(language : Language) : Map[WikiTitle, WikiPage]

    def updateMappingPage(page : WikiPage, language : Language)

    def removeMappingPage(title : WikiTitle, language : Language)

    def mappingSource(language : Language) : Source = new MemorySource(mappingPages(language).values.toList)  // standard implementation


    def extract(source : Source, destination : Destination, language : Language)
    {
        val graph = source.map(WikiParser())
                          .map(extractor(language))
                          .foldLeft(new Graph())(_ merge _)

        destination.write(graph)
    }

    def validateMapping(mappingsSource : Source, language : Language) : Elem =
    {
        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        Logger.getLogger(MappingsLoader.getClass.getName).addHandler(logHandler)

        //Load mappings
        MappingsLoader.load(new ServerExtractionContext(language, this))

        //Unregister xml log handler
        Logger.getLogger(MappingsLoader.getClass.getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }

    def validateOntologyPages(newOntologyPages : List[WikiPage] = List()) : Elem =
    {
        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        Logger.getLogger(classOf[OntologyReader].getName).addHandler(logHandler)

        val updatedOntologyPages = (ontologyPages ++ newOntologyPages.map(page => (page.title, page))).values

        //Load ontology
        new OntologyReader().read(new MemorySource(updatedOntologyPages.toList))

        //Unregister xml log handler
        Logger.getLogger(classOf[OntologyReader].getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }


    protected def loadOntologyPages =
    {
        logger.info("Loading ontology pages")
        WikiSource.fromNamespaces(namespaces = Set(WikiTitle.Namespace.OntologyClass, WikiTitle.Namespace.OntologyProperty),
                                  url = new URL("http://mappings.dbpedia.org/api.php"),
                                  language = Language.Default )
        .map(page => (page.title, page)).toMap
    }

    protected def loadMappingPages =
    {
        languages.map(lang => (lang, loadMappingsPages(lang))).toMap
    }

    protected def loadMappingsPages(language : Language) : Map[WikiTitle, WikiPage] =
    {
        val mappingNamespace = WikiTitle.Namespace.mappingNamespace(language)
                               .getOrElse(throw new IllegalArgumentException("No mapping namespace for language " + language))

        WikiSource.fromNamespaces(namespaces = Set(mappingNamespace),
                                  url = new URL("http://mappings.dbpedia.org/api.php"),
                                  language = Language.Default )
        .map(page => (page.title, page)).toMap
    }

    protected def loadOntology : Ontology =
    {
        new OntologyReader().read(new MemorySource(ontologyPages.values.toList))
    }

    protected def loadExtractors =
    {
        languages.map(lang => (lang, loadExtractor(lang))).toMap
    }

    protected def loadExtractor(language : Language) =
    {
        val context = new ServerExtractionContext(language, this)
        Extractor.load(extractors, context)
    }

}