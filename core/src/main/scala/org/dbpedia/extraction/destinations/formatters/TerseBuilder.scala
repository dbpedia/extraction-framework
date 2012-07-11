package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.TurtleUtils.escapeTurtle
import java.net.URI
import UriPolicy._

/**
 * Helps to build one triple/quad line in Turtle/Turtle-Quads/N-Triples/N-Quads format.
 * 
 * Objects of this class are not re-usable - create a new object for each triple.
 * 
 * @param policies Mapping from URI positions (as defined in UriPolicy) to URI policy functions.
 * Must have five (UriPolicy.POSITIONS) elements. If null, URIs will not be modified.
*/
class TerseBuilder(quads: Boolean, turtle: Boolean, policies: Array[Policy] = null) 
extends UriTripleBuilder(policies) {
  
  // Scala's StringBuilder doesn't have appendCodePoint
  private val sb = new java.lang.StringBuilder
  
  override def start(context: String): Unit = { 
    /* nothing to do */ 
  }
  
  override def uri(str: String, pos: Int): Unit = {
    val uri = parseUri(str, pos)
    // If URI is bad, comment out whole triple (may happen multiple times)
    if (uri.startsWith(BadUri)) sb.insert(0, "# ")
    this add '<' escape uri add "> "
  }
  
  /**
   * @param value must not be null
   * @param lang may be null
   */
  override def plainLiteral(value: String, lang: String): Unit = {
    this add '"' escape value add '"'
    if (lang != null) this add '@' add lang
    this add ' '
  }
  
  /**
   * @param value must not be null
   * @param datatype must not be null
   */
  override def typedLiteral(value: String, datatype: String): Unit = {
    this add '"' escape value add '"'
    this add "^^" uri(datatype, DATATYPE)
  }
  
  override def end(context: String): Unit = {
    if (quads) uri(context, CONTEXT)

    // use UNIX EOL. N-Triples and Turtle don't care:
    // http://www.w3.org/TR/rdf-testcases/#eoln and http://www.w3.org/TR/turtle/#term-turtle2-WS
    // and it's probably better to be consistent instead of using the EOL of the platform
    // where the file was generated. These files are moved around a lot anyway.
    this add ".\n"
  }
  
  override def result = sb.toString
  
  private def add(s: String): TerseBuilder = { 
    sb append s
    this 
  }
  
  private def add(c: Char): TerseBuilder = { 
    sb append c
    this 
  }
  
  /**
   * Escapes a Unicode string according to N-Triples / Turtle format.
   */
  private def escape(input: String): TerseBuilder = {
    escapeTurtle(sb, input, turtle)
    this
  }
  
}
