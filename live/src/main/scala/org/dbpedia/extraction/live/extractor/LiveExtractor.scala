package org.dbpedia.extraction.live.extractor

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.mappings.{Mappings, MappingsLoader, Extractor, Redirects}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiParser}
import org.dbpedia.extraction.sources.Source
import java.io.File
import org.dbpedia.extraction.mappings.RootExtractor

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
    /**
     * Returns a list of extractors.
     *
     * (maybe it would be possible to return a CompositeExtractor here (and load it with Extractor.load) ?)
     */
    def load(ontologySource : Source,
             mappingsSource : Source,
             articlesSource : Source,
             extractors : List[Class[_ <: Extractor]],
             language : Language) : List[RootExtractor] =
    {
        val context = extractionContext(language, ontologySource, mappingsSource, articlesSource)
        extractors.map(_.getConstructor(classOf[AnyRef]).newInstance(context)).map(new RootExtractor(_))
    }

    /**
     * Returns an AnyRef (anonymous class) object with all necessary methods needed as extraction context.
     * The different attributes are loaded only once and only if they are required.
     *
     * IMPORTANT: the context for the live extraction does not contain a commonsSource at the moment! (e.g. for ImageExtractor)
     */
    private def extractionContext(lang : Language, _ontologySource : Source, _mappingsSource : Source, _articlesSource : Source) =
    {
        new
        {
            def language : Language = lang

            private lazy val _ontology = new OntologyReader().read(_ontologySource)
            def ontology : Ontology = _ontology

            private lazy val _mappingPageSource = _mappingsSource.map(WikiParser())
            def mappingPageSource : Traversable[PageNode] = _mappingPageSource

            private lazy val _mappings = MappingsLoader.load(this)
            def mappings : Mappings = _mappings

            def articlesSource : Source = _articlesSource

            // just cache them in the current directory. TODO: find solution appropriate for live extraction.
            private lazy val _redirects = Redirects.load(articlesSource, new File("redirects_"+language.filePrefix+".obj"), language)
            def redirects : Redirects = _redirects
        }
    }

}