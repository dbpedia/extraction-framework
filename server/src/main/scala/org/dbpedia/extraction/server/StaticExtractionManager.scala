package org.dbpedia.extraction.server

import org.dbpedia.extraction.sources._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

/**
 * Lazily loads extraction context parameters when they are required, not before.
 * Is NOT able to update the ontology or the mappings.
 */
class StaticExtractionManager(languages : Set[Language], extractors : List[Class[Extractor]]) extends ExtractionManager(languages, extractors)
{
    @volatile private lazy val _ontologyPages : Map[WikiTitle, PageNode] = loadOntologyPages

    @volatile private lazy val _mappingPages : Map[Language, Map[WikiTitle, PageNode]] = loadMappingPages

    @volatile private lazy val _ontology : Ontology = loadOntology

    @volatile private lazy val _extractors : Map[Language, Extractor] = loadExtractors


    def extractor(language : Language) = _extractors(language)

    def ontology = _ontology

    def ontologyPages = _ontologyPages

    def updateOntologyPage(page : WikiPage)
    {
        throw new Exception("updating of ontologyPages not supported with this configuration; please use DynamicExtractionManager")
    }

    def removeOntologyPage(title : WikiTitle)
    {
        throw new Exception("removing of ontologyPages not supported with this configuration; please use DynamicExtractionManager")
    }

    def mappingPageSource(language : Language) = _mappingPages(language).values

    def updateMappingPage(page : WikiPage, language : Language)
    {
        throw new Exception("updateMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

    def removeMappingPage(title : WikiTitle, language : Language)
    {
        throw new Exception("removeMappingPage not supported with this configuration; please use DynamicExtractionManager")
    }

}