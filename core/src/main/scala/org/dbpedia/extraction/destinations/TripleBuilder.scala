package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import java.net.{URI,URISyntaxException}

/**
 * Helps to build one triple/quad line in Turtle/Turtle-Quads/N-Triples/N-Quads format.
 */
class TripleBuilder(iri: Boolean, turtle: Boolean)
{
  // Scala's StringBuilder doesn't have appendCodePoint
  private val sb = new java.lang.StringBuilder
  
  def uri(str: String): Unit = {
    val value = try {
      val uri = new URI(str)
      if (iri) uri.toString else uri.toASCIIString
    } catch {
      case usex: URISyntaxException =>
        // this triple is unusable, comment it out
        sb.insert(0, "# ")
        "BAD URI: "+usex.getMessage() 
    }
    
    // Note: If the string contains ">", the line cannot be parsed. But it's broken anyway, so what.
    // For Turtle, we could escape ">" as "\>", but for N-Triples, we can't.
    this add '<' esc value add "> "
  }
  
  /**
   * @param datatype must not be null
   */
  def value(value: String, datatype: Datatype, language: Language): Unit = {
    this add '"' esc value add '"' 
    if (datatype.name == "xsd:string") this add '@' add language.isoCode add ' '
    else this add "^^" uri datatype.uri
  }
  
  def close(): Unit = {
    // use UNIX EOL. N-Triples and Turtle don't care:
    // http://www.w3.org/TR/rdf-testcases/#eoln and http://www.w3.org/TR/turtle/#term-turtle2-WS
    // and it's probably better to be consistent instead of using the EOL of the platform
    // where the file was generated. These files are moved around a lot anyway.
    this add ".\n"
  }
    
  override def toString: String = {
    sb toString
  }
  
  private def add(s: String): TripleBuilder = { 
    sb append s
    this 
  }
  
  private def add(c: Char): TripleBuilder = { 
    sb append c
    this 
  }
  
  /**
   * Escapes a Unicode string according to N-Triples / Turtle format.
   */
  private def esc(input: String): TripleBuilder =
  {
    val length = input.length
    
    var offset = 0
    while (offset < length)
    {
      val c = input.codePointAt(offset)
      offset += Character.charCount(c)

      // TODO: use a lookup table for c < 0x80
           if (c == '\\') sb append "\\\\"
      else if (c == '\"') sb append "\\\""
      else if (c == '\n') sb append "\\n"
      else if (c == '\r') sb append "\\r";
      else if (c == '\t') sb append "\\t"
      else if (c >= 0x0020 && c < 0x007F) sb append c.toChar
      else if (turtle && c >= 0x00A0 && c <= 0xFFFF) sb append c.toChar
      else if (turtle && c >= 0x10000) sb appendCodePoint c
      else this appendHex c
    }
    
    this
  }
  
  private def appendHex(c: Int) {
    // TODO: use something faster than c.toHexString.toUpperCase and "0" *.
    // Append chars to the string buffer directly. 
    val hex = c.toHexString.toUpperCase
    if (c <= 0xFFFF) sb append "\\u" append "0" * (4 - hex.length)
    else sb append "\\U" append "0" * (8 - hex.length)
    sb append hex
  }
  
}
