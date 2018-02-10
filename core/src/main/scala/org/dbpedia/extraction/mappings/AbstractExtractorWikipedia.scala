package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{Config, ExtractionRecorder}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
 * User: Dimitris Kontokostas
 * Description
 * Created: 5/19/14 9:21 AM
 */
@SoftwareAgentAnnotation(classOf[AbstractExtractorWikipedia], AnnotationType.Extractor)
class AbstractExtractorWikipedia(
  context : {
    def ontology : Ontology
    def language : Language
    def configFile : Config
  })
  extends NifExtractor (context)
{
  override val datasets = Set(DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts)
}
