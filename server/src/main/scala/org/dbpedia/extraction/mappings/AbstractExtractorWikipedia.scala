package org.dbpedia.extraction.mappings

import scala.xml.XML
import scala.io.Source
import scala.language.reflectiveCalls
import java.io.InputStream
import java.net.URL
import java.util.logging.Logger
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.util.text.html.HtmlCoder
import org.dbpedia.util.text.ParseExceptionIgnorer

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

  override def apiUrl: String = "http://" + context.language.wikiCode + ".wikipedia.org/w/api.php"
}
