package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls

/**
 * User: Dimitris Kontokostas
 * Description
 * Created: 5/19/14 9:21 AM
 */

class HtmlAbstractExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def configFile : Config
  })
  extends NifExtractor (context)
{
  override val datasets = Set(DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts)
}
