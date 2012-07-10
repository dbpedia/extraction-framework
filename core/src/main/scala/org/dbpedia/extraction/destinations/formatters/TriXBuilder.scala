package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.util.XmlUtils.escape
import UriPolicy._
import TriXBuilder._

object TriXBuilder {
  private val spaces = (2 to (6, step = 2)).map(" " * _)
}

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 * 
 * Objects of this class are not re-usable - create a new object for each triple.
 * 
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 * Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
 */
class TriXBuilder(quads: Boolean, policies: Array[Policy] = null)
extends UriTripleBuilder(policies) {
  
  private var depth = 0
  
  private val sb = new java.lang.StringBuilder
  
  // public methods implementing TripleBuilder
  
  override def start(context: String): Unit = { 
    this startTag "graph"
    if (quads) uri(context, CONTEXT)
    this startTag "triple"
  }
  
  override def uri(value: String, pos: Int): Unit = {
    this add spaces(depth) add "<uri>" escapeUri(value, pos) add "</uri>\n"
  }
  
  override def plainLiteral(value: String, lang: String): Unit = {
    this add spaces(depth) add "<plainLiteral xml:lang=\"" add(lang) add "\">"
    escape(sb, value)
    this add "</plainLiteral>\n"
  }
  
  override def typedLiteral(value: String, datatype: String): Unit = {
    this add spaces(depth) add "<typedLiteral datatype=\"" escapeUri(datatype, DATATYPE) add "\">"
    escape(sb, value)
    this add "</typedLiteral>\n"
  }
  
  override def end(context: String): Unit = {
    this endTag "triple"
    this endTag "graph" 
  }
  
  override def result = sb.toString
  
  // private helper methods
  
  private def escapeUri(str: String, pos: Int): TriXBuilder = {
    val uri = parseUri(str, pos)
    // TODO: check if uri starts with BadUri. If yes, wrap the whole triple in <!-- and --> 
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
