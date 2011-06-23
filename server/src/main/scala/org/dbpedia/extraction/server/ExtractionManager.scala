package org.dbpedia.extraction.server

import org.dbpedia.extraction.ontology.io.OntologyReader
import java.net.URL
import org.dbpedia.extraction.sources._
import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.wikiparser.{WikiParser, WikiTitle}
import org.dbpedia.extraction.destinations.{Destination, Graph}
import org.dbpedia.extraction.mappings._
import xml.Elem
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.ExtractionContext

class ExtractionManager(languages : Set[Language])
{
    @volatile private var _ontologyPages : Map[WikiTitle, WikiPage] = loadOntologyPages

    @volatile private var _mappingPages : Map[Language, Map[WikiTitle, WikiPage]] = loadMappingPages

    @volatile private var _ontology = loadOntology

    @volatile private var _extractors : Map[Language, Extractor] = loadExtractors

    def validateMapping(mappingsSource : Source, language : Language) : Elem =
    {
        val emptySource = new MemorySource()

        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        Logger.getLogger(MappingsLoader.getClass.getName).addHandler(logHandler)

        //Load mappings
        MappingsLoader.load(new ExtractionContext(_ontology, language, Redirects.load(emptySource), mappingsSource, emptySource, emptySource))

        //Unregister xml log handler
        Logger.getLogger(MappingsLoader.getClass.getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }

    def validateOntologyPages(newOntologyPages : List[WikiPage] = List()) : Elem =
    {
        val emptySource = new MemorySource()

        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        Logger.getLogger(classOf[OntologyReader].getName).addHandler(logHandler)

        val updatedOntologyPages = (_ontologyPages ++ newOntologyPages.map(page => (page.title, page))).values

        //Load ontology
        new OntologyReader().read(new MemorySource(updatedOntologyPages.toList))

        //Unregister xml log handler
        Logger.getLogger(classOf[OntologyReader].getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }

    def extract(source : Source, destination : Destination, language : Language) : Unit =
    {
        val graph = source.map(WikiParser())
                          .map(extractor(language))
                          .foldLeft(new Graph())(_ merge _)

        destination.write(graph)
    }

    def ontologyPages = _ontologyPages

    def ontologyPages_=(pages : Map[WikiTitle, WikiPage]) =
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

    def ontology = _ontology

    def extractor(language : Language) = _extractors(language)

    private def loadOntologyPages =
    {
        WikiSource.fromNamespaces(namespaces = Set(WikiTitle.Namespace.OntologyClass, WikiTitle.Namespace.OntologyProperty),
                                  url = new URL("http://mappings.dbpedia.org/api.php"),
                                  language = Language.Default )
        .map(page => (page.title, page)).toMap
    }

    private def loadMappingPages =
    {
        languages.map(lang => (lang, loadMappingsPages(lang))).toMap
    }

    private def loadMappingsPages(language : Language) : Map[WikiTitle, WikiPage] =
    {
        val mappingNamespace = WikiTitle.Namespace.mappingNamespace(language)
            .getOrElse(throw new IllegalArgumentException("No mapping namespace for language " + language))

        WikiSource.fromNamespaces(namespaces = Set(mappingNamespace),
                                  url = new URL("http://mappings.dbpedia.org/api.php"),
                                  language = Language.Default )
        .map(page => (page.title, page)).toMap
    }

    private def loadOntology =
    {
        new OntologyReader().read(new MemorySource(_ontologyPages.values.toList))
    }

    private def loadExtractors =
    {
        languages.map(lang => (lang, loadExtractor(lang))).toMap
    }

    private def loadExtractor(language : Language) =
    {
        val ontologySource = new MemorySource(_ontologyPages.values.toList)
        val mappingSource = new MemorySource(_mappingPages(language).values.toList)
        val emptySource = new MemorySource()

        val extractors = List(classOf[LabelExtractor].asInstanceOf[Class[Extractor]], classOf[MappingExtractor].asInstanceOf[Class[Extractor]])

        Extractor.load(ontologySource, mappingSource, emptySource, emptySource, extractors, language)
    }
}
