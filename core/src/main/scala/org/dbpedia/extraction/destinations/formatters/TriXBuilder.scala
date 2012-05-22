package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.util.XmlUtils.escape
import java.net.URI
import UriPolicy._

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 * 
 * Objects of this class are not re-usable - create a new object for each triple.
 */
class TriXBuilder(quads: Boolean, process: Policy) 
extends UriTripleBuilder(process) {
  
  private var depth = 0
  
  private val spaces = (2 to (6, step = 2)).map(" " * _)
  
  private val sb = new java.lang.StringBuilder
  
  override def start(context: String): Unit = { 
    startTag("graph")
    if (quads) uri(context, CONTEXT)
    startTag("triple")
  }
  
  override def uri(value: String, pos: Int): Unit = {
    this add spaces(depth) add ("<uri>") escapeUri(value, pos) add ("</uri>\n")
  }
  
  override def plainLiteral(value: String, lang: String): Unit = {
    this add spaces(depth) add ("<plainLiteral xml:lang=\"") add(lang) add("\">")
    escape(sb, value)
    add ("</plainLiteral>\n")
  }
  
  override def typedLiteral(value: String, datatype: String): Unit = {
    this add spaces(depth) add ("<typedLiteral datatype=\"") escapeUri(datatype, DATATYPE) add("\">")
    escape(sb, value)
    this add ("</typedLiteral>\n")
  }
  
  override def end(context: String): Unit = {
    endTag("triple")
    endTag("graph")
  }
  
  override def toString: String = sb.toString
  
  private def escapeUri(str: String, pos: Int): TriXBuilder = {
    val uri = parseUri(str, pos)
    // TODO: check if uri starts with badUri. If yes, wrap the whole triple in <!-- and --> 
    // (but take care that we do it only once). But currently this class is only used during 
    // testing, so it's probably better to have these errors visible.
    escape(sb, uri)
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
