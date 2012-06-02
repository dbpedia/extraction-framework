package org.dbpedia.extraction.util

import javax.xml.stream.XMLEventReader
import scala.collection.Set
import scala.collection.mutable.HashSet
import org.dbpedia.extraction.util.RichStartElement.richStartElement
import java.io.IOException
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

object WikiDisambigReader {
  
  /**
   * The query for api.php, without the leading '?'.
   */
  val query = "action=parse&page=MediaWiki:Disambiguationspage&format=xml&prop=links"
    
  def read(language: Language, xml: XMLEventReader): Set[String] = new WikiDisambigReader(language, xml).read()

  def read(language: Language, xml: XMLEventAnalyzer): Set[String] = new WikiDisambigReader(language, xml).read()
}

/**
 * Reads result of the api.php query above.
 */
class WikiDisambigReader(language: Language, in: XMLEventAnalyzer) {
  
  def this(language: Language, reader: XMLEventReader) = this(language, new XMLEventAnalyzer(reader))
  
  private val prefix = Namespaces.names(language)(Namespace.Template.code)+':'
  
  /**
   * @return settings
   */
  def read(): Set[String] = {
    in.document { _ =>
      in.element("api") { _ =>
        in.ifElement("error") { error => throw new IOException(error attr "info") }
        in.element("parse") { _ =>
          in.element("links") { _ =>
            val links = new HashSet[String]()
            in.elements("pl") { pl =>
              val code = pl.attr("ns").toInt
              in.text { text =>
                if (code == Namespace.Template.code) {
                  if (! text.startsWith(prefix)) throw new IOException("Expected title starting with '"+prefix+"', found '"+text+"'")
                  links += text.substring(prefix.length)
                }
              }
            }
            links
          }
        }
      }
    }
  }
  
}