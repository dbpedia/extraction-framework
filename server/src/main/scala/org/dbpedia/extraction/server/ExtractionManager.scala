package org.dbpedia.extraction.server

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import xml.Elem
import java.util.logging.{Level, Logger}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.destinations.{Graph, Destination}
import org.dbpedia.extraction.sources.{XMLSource, WikiSource, Source, WikiPage}
import java.net.URL
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.wikiparser._
import java.io.File

/**
 * Base class for extraction managers.
 * Subclasses can either support updating the ontology and/or mappings,
 * or they can support lazy loading of context parameters.
 */

abstract class ExtractionManager(languages : Traversable[Language], files: FileParams)
{
    private val extractors = List(classOf[LabelExtractor],classOf[MappingExtractor])
    
    private val logger = Logger.getLogger(classOf[ExtractionManager].getName)


    def extractor(language : Language) : Extractor

    def ontology : Ontology

    def ontologyPages : Map[WikiTitle, PageNode]

    def mappingPageSource(language : Language) : Traversable[PageNode]

    def mappings(language : Language) : Mappings

    def updateOntologyPage(page : WikiPage)

    def removeOntologyPage(title : WikiTitle)

    def updateMappingPage(page : WikiPage, language : Language)

    def removeMappingPage(title : WikiTitle, language : Language)


    protected val parser = WikiParser()

    def extract(source : Source, destination : Destination, language : Language)
    {
        val graph = source.map(parser)
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

        // context object that has only this mappingSource
        val context = new ServerExtractionContext(language, this)
        {
            override def mappingPageSource : Traversable[PageNode] = mappingsSource.map(parser)
        }

        //Load mappings
        MappingsLoader.load(context)

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

        val newOntologyPagesMap = newOntologyPages.map(parser).map(page => (page.title, page)).toMap
        val updatedOntologyPages = (ontologyPages ++ newOntologyPagesMap).values

        //Load ontology
        new OntologyReader().read(updatedOntologyPages)

        //Unregister xml log handler
        Logger.getLogger(classOf[OntologyReader].getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }


    protected def loadOntologyPages =
    {
        val source = if (files.ontologyFile != null && files.ontologyFile.isFile)
        {
            logger.warning("LOADING ONTOLOGY NOT FROM SERVER, BUT FROM LOCAL FILE ["+files.ontologyFile+"] - MAY BE OUTDATED - ONLY FOR TESTING!")
            XMLSource.fromFile(files.ontologyFile, language = Language.Default)
        }
        else 
        {
            val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
            val url = Server.wikiApiUrl
            val language = Language.Default
            logger.info("Loading ontology pages from URL ["+url+"]")
            WikiSource.fromNamespaces(namespaces, url, language)
        }
        
        source.map(parser).map(page => (page.title, page)).toMap
    }

    protected def loadMappingPages =
    {
        logger.info("Loading mapping pages")
        languages.map(lang => (lang, loadMappingsPages(lang))).toMap
    }

    protected def loadMappingsPages(language : Language) : Map[WikiTitle, PageNode] =
    {
        val namespace = Namespace.mappings.getOrElse(language, throw new NoSuchElementException("no mapping namespace for language "+language.wikiCode))
        
        val source = if (files.mappingsDir != null && files.mappingsDir.isDirectory)
        {
            val file = new File(files.mappingsDir, namespace.getName(Language.Default).replace(' ','_')+".xml")
            logger.warning("LOADING MAPPINGS NOT FROM SERVER, BUT FROM LOCAL FILE ["+file+"] - MAY BE OUTDATED - ONLY FOR TESTING!")
            XMLSource.fromFile(file, language = language)
        }
        else
        {
            val url = Server.wikiApiUrl
            val language = Language.Default
            WikiSource.fromNamespaces(Set(namespace), url, language)
        }
        
        source.map(parser).map(page => (page.title, page)).toMap
    }

    protected def loadOntology : Ontology =
    {
        new OntologyReader().read(ontologyPages.values)
    }

    protected def loadExtractors(): Map[Language, Extractor] =
    {
        try languages.map(lang => (lang, loadExtractors(lang))).toMap
        finally logger.info("All extractors loaded for languages "+languages.mkString(", "))
    }

    protected def loadExtractors(language : Language): Extractor =
    {
        val context = new ServerExtractionContext(language, this)
        Extractor.load(extractors, context)
    }

    protected def loadMappings() : Map[Language, Mappings] =
    {
        languages.map(lang => (lang, loadMappings(lang))).toMap
    }

    protected def loadMappings(language : Language) : Mappings =
    {
        val context = new ServerExtractionContext(language, this)
        MappingsLoader.load(context)
    }


}