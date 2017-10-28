package org.dbpedia.extraction.mappings

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

class AbstractExtractorWikipedia(
  context : {
    def ontology : Ontology
    def language : Language
    def configFile : Config
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  })
  extends NifExtractor (context)
{
  override val datasets = Set(DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts)
}
