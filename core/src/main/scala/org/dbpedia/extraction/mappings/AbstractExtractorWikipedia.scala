package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Config, Language}

import scala.language.reflectiveCalls

/**
 * User: Dimitris Kontokostas
 * Description
 * Created: 5/19/14 9:21 AM
 */
@deprecated("see AbstractExtractor.scala")
class AbstractExtractorWikipedia(
  context : {
    def ontology : Ontology
    def language : Language
    def configFile : Config
  })
  extends AbstractExtractor (context)
{

  //TODO remove this class
  //override def apiUrl = new URL("https://" + context.language.wikiCode + ".wikipedia.org/w/api.php")
}
