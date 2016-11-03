package org.dbpedia.extraction.server

import java.io.File
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{Source, WikiPage, WikiSource, XMLSource}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.xml.Elem

/**
 * Base class for extraction managers.
 * Subclasses can either support updating the ontology and/or mappings,
 * or they can support lazy loading of context parameters.
 */

abstract class ExtractionManager(
    languages : Seq[Language],
    paths: Paths,
    redirects: Map[Language, Redirects],
    mappingTestExtractors: Seq[Class[_ <: Extractor[_]]],
    customTestExtractors: Map[Language, Seq[Class[_ <: Extractor[_]]]])
{
  self =>
    
    private val logger = Logger.getLogger(classOf[ExtractionManager].getName)

    def mappingExtractor(language : Language) : WikiPageExtractor

    def customExtractor(language : Language) : WikiPageExtractor

    def ontology() : Ontology

    def ontologyPages() : Map[WikiTitle, PageNode]

    def mappingPageSource(language : Language) : Traversable[WikiPage]

    def mappings(language : Language) : Mappings

    def updateOntologyPage(page : WikiPage)

    def removeOntologyPage(title : WikiTitle)

    def updateMappingPage(page : WikiPage, language : Language)

    def removeMappingPage(title : WikiTitle, language : Language)

    protected val disambiguations : Disambiguations = loadDisambiguations()

    /**
     * Called by server to update all users of this extraction manager.
     */
    def updateAll
    
    protected val parser = WikiParser.getInstance()

    def extract(source: Source, destination: Destination, language: Language, useCustomExtraction: Boolean = false): Unit = {
      val extract = if (useCustomExtraction) customExtractor(language) else mappingExtractor(language)
      destination.open()
      for (page <- source) destination.write(extract.extract(page))
      destination.close()
    }

    def validateMapping(mappingsPages: Traversable[WikiPage], lang: Language) : Elem =
    {
        val logger = Logger.getLogger(MappingsLoader.getClass.getName)
        
        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        logger.addHandler(logHandler)

        // context object that has only this mappingSource
        val context = new {
          val ontology = self.ontology
          val language = lang
          val redirects: Redirects = new Redirects(Map())
          val mappingPageSource = mappingsPages
          val disambiguations = self.disambiguations
        }

        //Load mappings
        val mappings = MappingsLoader.load(context)
        
        if (mappings.templateMappings.isEmpty && mappings.tableMappings.isEmpty)
          logger.severe("no mappings found")

        //Unregister xml log handler
        logger.removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }

    def validateOntologyPages(newOntologyPages : List[WikiPage] = List()) : Elem =
    {
        //Register xml log hanlder
        val logHandler = new XMLLogHandler()
        logHandler.setLevel(Level.WARNING)
        Logger.getLogger(classOf[OntologyReader].getName).addHandler(logHandler)

        val newOntologyPagesMap = newOntologyPages.map(parser).flatten.map(page => (page.title, page)).toMap
        val updatedOntologyPages = (ontologyPages ++ newOntologyPagesMap).values

        //Load ontology
        new OntologyReader().read(updatedOntologyPages)

        //Unregister xml log handler
        Logger.getLogger(classOf[OntologyReader].getName).removeHandler(logHandler)

        //Return xml
        logHandler.xml
    }


    protected def loadOntologyPages() =
    {
        val source = if (paths.ontologyFile != null && paths.ontologyFile.isFile)
        {
            logger.warning("LOADING ONTOLOGY NOT FROM SERVER, BUT FROM LOCAL FILE ["+paths.ontologyFile+"] - MAY BE OUTDATED - ONLY FOR TESTING!")
            XMLSource.fromFile(paths.ontologyFile, language = Language.Mappings)
        }
        else 
        {
            val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
            val url = paths.apiUrl
            val language = Language.Mappings
            logger.info("Loading ontology pages from URL ["+url+"]")
            WikiSource.fromNamespaces(namespaces, url, language)
        }
        
        source.map(parser).flatten.map(page => (page.title, page)).toMap
    }

    protected def loadDisambiguations() =
    {
        Disambiguations.empty()
    }

    protected def loadMappingPages(): Map[Language, Map[WikiTitle, WikiPage]] =
    {
        logger.info("Loading mapping pages")
        languages.map(lang => (lang, loadMappingPages(lang))).toMap
    }

    protected def loadMappingPages(language : Language) : Map[WikiTitle, WikiPage] =
    {
        val namespace = Namespace.mappings.getOrElse(language, throw new NoSuchElementException("no mapping namespace for language "+language.wikiCode))
        
        val source = if (paths.mappingsDir != null && paths.mappingsDir.isDirectory)
        {
            val file = new File(paths.mappingsDir, namespace.name(Language.Mappings).replace(' ','_')+".xml")
            if(!file.exists()) {
              logger.warning("MAPPING FILE [" + file + "] DOES NOT EXIST! WILL BE IGNORED")
              return Map[WikiTitle, WikiPage]()
            }
            logger.warning("LOADING MAPPINGS NOT FROM SERVER, BUT FROM LOCAL FILE ["+file+"] - MAY BE OUTDATED - ONLY FOR TESTING!")
            XMLSource.fromFile(file, language) // TODO: use Language.Mappings?
        }
        else
        {
            val url = paths.apiUrl
            WikiSource.fromNamespaces(Set(namespace), url, language) // TODO: use Language.Mappings?
        }
        
        source.map(page => (page.title, page)).toMap
    }

    protected def loadOntology() : Ontology =
    {
        new OntologyReader().read(ontologyPages.values)
    }

    protected def loadMappingTestExtractors(): Map[Language, WikiPageExtractor] =
    {
        val extractors = languages.map(lang => (lang, loadExtractors(lang, mappingTestExtractors))).toMap
        logger.info("All mapping test extractors loaded for languages "+languages.map(_.wikiCode).sorted.mkString(","))
        extractors
    }

    protected def loadCustomTestExtractors(): Map[Language, WikiPageExtractor] =
    {
      val extractors = languages.map(lang => (lang, loadExtractors(lang,customTestExtractors(lang)))).toMap
      logger.info("All custom extractors loaded for languages "+languages.map(_.wikiCode).sorted.mkString(","))
      extractors
    }

    protected def loadExtractors(lang : Language, classes: Seq[Class[_ <: Extractor[_]]]): WikiPageExtractor =
    {
        CompositeParseExtractor.load(classes,self.getExtractionContext(lang))
    }

    protected def getExtractionContext(lang: Language) = {
      new { val ontology = self.ontology
            val language = lang
            val mappings = self.mappings(lang)
            val redirects = self.redirects.getOrElse(lang, new Redirects(Map()))
            val disambiguations = self.disambiguations
      }
    }

    protected def loadMappings() : Map[Language, Mappings] =
    {
        languages.map(lang => (lang, loadMappings(lang))).toMap
    }

    protected def loadMappings(lang : Language) : Mappings =
    {
        val context = new {
          val ontology = self.ontology
          val language = lang
          val redirects: Redirects = new Redirects(Map())
          val mappingPageSource = self.mappingPageSource(lang)
          val disambiguations = self.disambiguations
        }

        MappingsLoader.load(context)
    }


}
