package org.dbpedia.extraction.destinations.formatters

import java.lang.StringBuilder
import UriPolicy._

/**
 * Builds a triple for SPARUL (
 */
class SPARULBuilder (policies: Array[Policy] = null)
  extends UriTripleBuilder(policies) {

  // Scala's StringBuilder doesn't have appendCodePoint
  private val sb = new java.lang.StringBuilder

  override def start(context: String): Unit = {
    /* nothing to do */
  }

  override def uri(str: String, pos: Int): Unit = {
    val uri = parseUri(str, pos)
    // If URI is bad, comment out whole triple (may happen multiple times)
    if (uri.startsWith(BadUri)) sb.insert(0, "#")
    this add '<' escapeUri uri add "> "
  }

  /**
   * @param value must not be null
   * @param lang may be null
   */
  override def plainLiteral(value: String, lang: String): Unit = {
    this add '"' escapeString value add '"'
    if (lang != null) this add '@' add lang
    this add ' '
  }

  /**
   * @param value must not be null
   * @param datatype must not be null
   */
  override def typedLiteral(value: String, datatype: String): Unit = {
    this add '"' escapeString value add '"'
    this add "^^" uri(datatype, DATATYPE)
  }

  override def end(context: String): Unit = {
    sb append ".\n"
  }

  private def add(s: String): SPARULBuilder = {
    sb append s
    this
  }

  private def add(c: Char): SPARULBuilder = {
    sb append c
    this
  }

  override def result = sb.toString

  /**
   * Escapes a Unicode string according to N-Triples / Turtle format.
   */

  /*
  * IRI_REF 	  ::=   	'<' ([^<>"{}|^`\]-[#x00-#x20])* '>'
  * 
  * TODO: This is probably unnecessary. If the URI contains an illegal character from the list
  * above, parseUri already returned a "BAD URI" string. When that happens, the line is
  * commented out and it doesn't matter anymore if there are any invalid characters in the URI.
  * */
  private def escapeUri(input: String) : SPARULBuilder = {

    // FIXME: this should probably include square brackets - the regex in IRI_REF is confusing.
    val strip = "<>\"{}|^`\\"

    var index = 0
    while (index < input.length)
    {
      val code = input.codePointAt(index)
      index += Character.charCount(code)
      val doStrip = (strip.contains(code) || (code >= 0x0000 && code <= 0x0020))
      if (!doStrip) sb.appendCodePoint(code)
    }
    this
  }

  /*
  * STRING_LITERAL2 	  ::=   	'"' ( ([^#x22#x5C#xA#xD]) | ECHAR )* '"'
  * ECHAR 	  ::=   	'\' [tbnrf\"']
  * */

  private def escapeString(input: String): SPARULBuilder = {
    var index = 0
    while (index < input.length)
    {
      val code = input.codePointAt(index)
      index += Character.charCount(code)
      if (code == '\\') sb append "\\\\"
      else if (code == '\"') sb append "\\\""
      else if (code == '\'') sb append "\\\'"
      else if (code == '\n') sb append "\\n"
      else if (code == '\r') sb append "\\r"
      else if (code == '\t') sb append "\\t"
      else if (code == '\f') sb append "\\f"
      else if (code == '\b') sb append "\\b"
      else sb appendCodePoint code
    }
    this
  }
}
