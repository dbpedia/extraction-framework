package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import java.net.{URI,URISyntaxException}
import scala.xml.Utility.escape

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXBuilder(iri: Boolean)
{
  private var depth = 0
  
  private val spaces = (2 to (6, step = 2)).map(" " * _)
  
  private val sb = new StringBuilder
  
  def uri(value: String): Unit = {
    this add spaces(depth) add "<uri>" escapeUri value add "</uri>\n"
  }
  
  def literal(value: String, datatype: Datatype, language: Language): Unit = {
    this add spaces(depth) add "<typedLiteral datatype=\"" escapeUri datatype.uri add "\">"
    escape(value, sb) 
    this add "</typedLiteral>\n"
  }
  
  override def toString: String = sb.toString
  
  private def escapeUri(str: String): TriXBuilder = {
    val value = try {
      val uri = new URI(str)
      if (iri) uri.toString else uri.toASCIIString
    } catch {
      // TODO: we could exclude the whole triple (insert <!-- at start and append --> at end, 
      // but take care that we do it only once), but currently this class is only used during testing 
      // and it's probably better to have these errors visible
      case usex: URISyntaxException => "BAD URI: "+usex.getMessage() 
    }
    escape(value, sb)    
    this
  }
      
  /**
   * print spaces, print tag, increase depth
   */
  def startTag(name: String): Unit = {
    this add spaces(depth) add '<' add name add ">\n"
    depth += 1
  }
  
  /**
   * decrease depth, print spaces, print tag
   */
  def endTag(name: String): Unit = {
    depth -= 1
    this add spaces(depth) add "</" add name add ">\n"
  }
  
  private def add(s: String): TriXBuilder = { 
    sb append s
    this 
  }
  
  private def add(c: Char): TriXBuilder = { 
    sb append c
    this 
  }
  
}
