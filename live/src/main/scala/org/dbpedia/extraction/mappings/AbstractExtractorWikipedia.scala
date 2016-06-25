package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls

/**
  * Overwrites the apiUrl property for the AbstractExtractor in the core module to work with configurable Wikipedias.
  * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
  * date 21.05.2016.
  */
class AbstractExtractorWikipedia(
  context : {
    def ontology : Ontology
    def language : Language
  })
  extends AbstractExtractor (context)
{

  override def apiUrl: String = "https://" + context.language.wikiCode + ".wikipedia.org/w/api.php"
}
