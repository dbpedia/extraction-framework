package org.dbpedia.extraction.live.extractor

import org.dbpedia.extraction.sources.Source
import java.io.File
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.mappings.{Extractor, ExtractionContext, Redirects}

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 9, 2010
 * Time: 1:38:46 PM
 * This object is used in live extraction to perform some start up steps e.g. loading the ontology and the mapping
 * i.e. loading the data that should only be used once
 */

object LiveExtractor
{
    private val redirectsCacheFile = new File("src/main/resources/redirects.cache")
    private var ontology : Ontology = null;
    private var redirects : Redirects = null;
    private var MainContext : ExtractionContext = null;
    /**
     * Creates a new extractor.
     *
     * @param mappingsSource Source containing the mapping definitions
     * @param commonsSource Source containing the pages from Wikipedia Commons
     * @param articlesSource Source containing all articles
     * @param extractors List of extractor classes to be instantiated
     * @param language The language
     * @return The extractor
     */

    //This method loads the ontology, and it is placed in a separate function in order to call it only once in the
    //the beginning of the extraction process
    //@param ontologySource Source containing the ontology definitions
    def loadOntology(ontologySource: Source)
      {
        ontology = new OntologyReader().read(ontologySource)
      }


    //This function loads the redirects
    def loadRedirects(articlesSource : Source)
      {
         //redirects = Redirects.load(redirectsCacheFile, articlesSource)
        redirects = Redirects.loadFromSource(articlesSource)
      }

    //This function builds the extraction context in the beginning in order to speed up the process of live extraction
    def makeExtractionContext(mappingsSource : Source, commonsSource : Source, articlesSource : Source, language : Language)
      {
        MainContext = new ExtractionContext(ontology, language, redirects, mappingsSource, commonsSource, articlesSource)
      }


  def load(ontologySource : Source, mappingsSource : Source, commonsSource : Source, articlesSource : Source,
             extractors : List[Class[Extractor]], language : Language) : List[Extractor] =
    {
      val extractorInstances = extractors.map(_.getConstructor(classOf[ExtractionContext]).newInstance(MainContext))

      return extractorInstances;
    }

}