package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls

/**
 * User: Dimitris Kontokostas
 * Description
 * Created: 5/19/14 9:21 AM
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
