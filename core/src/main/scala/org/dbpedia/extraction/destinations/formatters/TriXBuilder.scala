package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import java.net.{URI,URISyntaxException}
import scala.xml.Utility.escape

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXBuilder(iri: Boolean, quads: Boolean) extends UriTripleBuilder(iri) {
  
  private var depth = 0
  
  private val spaces = (2 to (6, step = 2)).map(" " * _)
  
  private val sb = new StringBuilder
  
  override def start(context: String): Unit = { 
    startTag("graph")
    if (quads) uri(context)
    startTag("triple")
  }
  
  override def uri(value: String): Unit = {
    this add spaces(depth) add ("<uri>") escapeUri(value) add ("</uri>\n")
  }
  
  override def badUri(badUris: Int): Unit = {
    // TODO: we could exclude the whole triple (insert <!-- at start and append --> at end, 
    // but take care that we do it only once), but currently this class is only used during 
    // testing and it's probably better to have these errors visible
  }
  
  override def string(value: String, lang: String): Unit = {
    this add spaces(depth) add ("<plainLiteral xml:lang=\"") add(lang) add("\">")
    escape(value, sb) 
    add ("</plainLiteral>\n")
  }
  
  override def value(value: String, datatype: String): Unit = {
    this add spaces(depth) add ("<typedLiteral datatype=\"") escapeUri(datatype) add("\">")
    escape(value, sb) 
    this add ("</typedLiteral>\n")
  }
  
  override def end(context: String): Unit = {
    endTag("triple")
    endTag("graph")
  }
  
  override def toString: String = sb.toString
  
  private def escapeUri(str: String): TriXBuilder = {
    escape(parseUri(str), sb)    
    this
  }
      
  /**
   * print spaces, print tag, increase depth
   */
  private def startTag(name: String): Unit = {
    this add spaces(depth) add ('<') add(name) add (">\n")
    depth += 1
  }
  
  /**
   * decrease depth, print spaces, print tag
   */
  private def endTag(name: String): Unit = {
    depth -= 1
    this add spaces(depth) add ("</") add(name) add (">\n")
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
