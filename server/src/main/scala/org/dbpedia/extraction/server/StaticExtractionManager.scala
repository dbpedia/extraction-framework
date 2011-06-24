package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.config.ConfigException

/**
 * Lazily loads extraction context parameters when they are required, not before.
 * Is NOT able to update the ontology or the mappings.
 */
class StaticExtractionManager(languages : Set[Language], extractors : List[Class[Extractor]]) extends ExtractionManager(languages, extractors)
{
    @volatile private lazy val _ontologyPages : Map[WikiTitle, WikiPage] = loadOntologyPages

    @volatile private lazy val _mappingPages : Map[Language, Map[WikiTitle, WikiPage]] = loadMappingPages

    @volatile private lazy val _ontology : Ontology = loadOntology

    @volatile private lazy val _extractors : Map[Language, Extractor] = loadExtractors

    @volatile private lazy val _redirects = Redirects.load(new MemorySource)


    def extractor(language : Language) = _extractors(language)

    def ontology = _ontology

    def ontologyPages = _ontologyPages

    def ontologyPages_= (pages : Map[WikiTitle, WikiPage])
    {
        throw new ConfigException("updating of ontologyPages not supported with this configuration; please use DynamicExtractionManager")
    }

    def mappingPages(language : Language) = _mappingPages(language)

    def updateMappingPage(page : WikiPage, language : Language)
    {
        throw new ConfigException("updateMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        throw new ConfigException("removeMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

    def redirects = _redirects
}