package org.dbpedia.extraction.live.extractor

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.mappings._

import scala.reflect.ClassTag

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
           commonsSource : Source,
           extractors : List[Class[Extractor[_]]],
           language : Language) : List[Extractor[_]] =
  {

    val context = extractionContext(language, ontologySource, mappingsSource, articlesSource, commonsSource)

    // initialize extractor list
    val extractorList = extractors.map(_.getConstructor(classOf[AnyRef]).newInstance(context))

    extractorList.foreach( e => println("EXTRACTOR  = " + e.getClass))
    extractorList
  }

  /**
   * Returns an AnyRef (anonymous class) object with all necessary methods needed as extraction context.
   * The different attributes are loaded only once and only if they are required.
   *
   * IMPORTANT: the context for the live extraction does not contain a commonsSource at the moment! (e.g. for ImageExtractor)
   */
  private def extractionContext(lang : Language, _ontologySource : Source, _mappingsSource : Source, _articlesSource : Source,
                                _commonsSource : Source) =
  {
    new
      {
        def language : Language = lang

        private lazy val _ontology = new OntologyReader().read(_ontologySource)
        def ontology : Ontology = _ontology

        def mappingPageSource : Traversable[WikiPage] = _mappingsSource

        private lazy val _mappings: Mappings = MappingsLoader.load(this)
        def mappings : Mappings = _mappings

        def articlesSource : Source = _articlesSource

        private lazy val _redirects = Redirects.loadFromSource(articlesSource, language)
        def redirects : Redirects = _redirects

        //TODO automate this task
        def disambiguations : Disambiguations = new Disambiguations(Set[Long]())

        def commonsSource : Source = _commonsSource

        def recorder[T: ClassTag]: ExtractionRecorder[T] = null.asInstanceOf[ ExtractionRecorder[T]]
      }
  }

}