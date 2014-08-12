package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import java.io.File

/**
 * Lazily loads extraction context parameters when they are required, not before.
 * Is NOT able to update the ontology or the mappings.
 * This manager is good for testing locally.
 */
class StaticExtractionManager(
  update: (Language, Mappings) => Unit, languages : Seq[Language],
  paths: Paths,
  redirects: Map[Language, Redirects],
  mappingTestExtractors: Seq[Class[_ <: Extractor[_]]],
  /** TODO: support customExtractors here*/
  customTestExtractors: Map[Language, Seq[Class[_ <: Extractor[_]]]])
extends ExtractionManager(languages, paths, redirects, mappingTestExtractors, customTestExtractors)
{
    @volatile private lazy val _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    @volatile private lazy val _mappingPages : Map[Language, Map[WikiTitle, WikiPage]] = loadMappingPages

    @volatile private lazy val _ontology : Ontology = loadOntology

    @volatile private lazy val _mappings : Map[Language, Mappings] = loadMappings

    @volatile private lazy val _extractors : Map[Language, RootExtractor] = loadMappingTestExtractors


    def mappingExtractor(language : Language) = _extractors(language)

    def customExtractor(language : Language) = {
      throw new Exception("Custom extractor not yet implemented; please use DynamicExtractionManager")
    }

    def ontology() = _ontology

    def ontologyPages() = _ontologyPages

    def mappingPageSource(language : Language) = _mappingPages(language).values

    def mappings(language : Language) = _mappings(language)

    /**
     * Called on startup to initialize all mapping stats managers.
     */
    def updateAll() = {
      for ((language, mappings) <- _mappings) update(language, mappings)
    }
        
    def updateOntologyPage(page : WikiPage)
    {
        throw new Exception("updating of ontologyPages not supported with this configuration; please use DynamicExtractionManager")
    }

    def removeOntologyPage(title : WikiTitle)
    {
        throw new Exception("removing of ontologyPages not supported with this configuration; please use DynamicExtractionManager")
    }

    def updateMappingPage(page : WikiPage, language : Language)
    {
        throw new Exception("updateMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        throw new Exception("removeMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

}