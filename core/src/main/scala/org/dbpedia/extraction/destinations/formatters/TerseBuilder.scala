package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.NumberUtils.toHex
import java.net.{URI,URISyntaxException}

/**
 * Helps to build one triple/quad line in Turtle/Turtle-Quads/N-Triples/N-Quads format.
 */
class TerseBuilder(iri: Boolean, quads: Boolean, turtle: Boolean) extends UriTripleBuilder(iri) {
  
  // Scala's StringBuilder doesn't have appendCodePoint
  private val sb = new java.lang.StringBuilder
  
  override def start(context: String): Unit = { 
    /* nothing to do */ 
  }
  
  override def badUri(badUris: Int): Unit = {
    // this triple is unusable, comment it out (but only once)
    if (badUris == 1) sb.insert(0, "# ")
  }
  
  override def uri(str: String): Unit = {
    // Note: If a bad uri contains ">", the line cannot be parsed - but it's broken anyway.
    // For Turtle, we could fix this by escaping ">" as "\>", but for N-Triples, we can't.
    this add '<' escape parseUri(str) add "> "
  }
  
  /**
   * @param datatype must not be null
   */
  override def string(value: String, lang: String): Unit = {
    this add '"' escape value add '"' add '@' add lang add ' '
  }
  
  /**
   * @param datatype must not be null
   */
  override def value(value: String, datatype: String): Unit = {
    this add '"' escape value add '"' add "^^" uri datatype
  }
  
  override def end(context: String): Unit = {
    if (quads) uri(context)

    // use UNIX EOL. N-Triples and Turtle don't care:
    // http://www.w3.org/TR/rdf-testcases/#eoln and http://www.w3.org/TR/turtle/#term-turtle2-WS
    // and it's probably better to be consistent instead of using the EOL of the platform
    // where the file was generated. These files are moved around a lot anyway.
    this add ".\n"
  }
  
  override def toString: String = {
    sb toString
  }
  
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
  private def escape(input: String): TerseBuilder =
  {
    val length = input.length
    
    var offset = 0
    while (offset < length)
    {
      val c = input.codePointAt(offset)
      offset += Character.charCount(c)

      // TODO: use a lookup table for c <= 0xA0? c <= 0xFF?
           if (c == '\\') sb append "\\\\"
      else if (c == '\"') sb append "\\\""
      else if (c == '\n') sb append "\\n"
      else if (c == '\r') sb append "\\r";
      else if (c == '\t') sb append "\\t"
      else if (c >= 0x0020 && c < 0x007F) sb append c.toChar
      else if (turtle && c >= 0x00A0 && c <= 0xFFFF) sb append c.toChar
      else if (turtle && c >= 0x10000) sb appendCodePoint c
      else if (c <= 0xFFFF) appendHex(c, 'u', 4)
      else appendHex(c, 'U', 8)
    }
    
    this
  }
  
  private def appendHex(c: Int, u: Char, d: Int) {
    sb append "\\" append u
    toHex(sb, c, d) 
  }
  
}
