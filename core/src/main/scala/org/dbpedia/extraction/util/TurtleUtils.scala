package org.dbpedia.extraction.util

import java.lang.StringBuilder

object TurtleUtils {
  
  /**
   * Escapes a Unicode string according to Turtle / N-Triples format.
   * @param input must not be null
   * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
   * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
   */
  def escapeTurtle(input: String, turtle: Boolean): String = {
    val sb = new StringBuilder
    new TurtleEscaper(sb, turtle).escapeTurtle(input)
    sb.toString
  }
  
  /**
   * Escapes a Unicode string according to Turtle / N-Triples format.
   * @param sb must not be null
   * @param input must not be null
   * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
   * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
   */
  def escapeTurtle(sb: StringBuilder, input: String, turtle: Boolean): StringBuilder = {
    new TurtleEscaper(sb, turtle).escapeTurtle(input)
    sb
  }
  
  /**
   * Escapes a Unicode code point according to Turtle / N-Triples format.
   * @param sb must not be null
   * @param c Unicode code point
   * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
   * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
   */
  def escapeTurtle(sb: StringBuilder, code: Int, turtle: Boolean): StringBuilder = {
    new TurtleEscaper(sb, turtle).escapeTurtle(code)
    sb
  }
  
}

/**
 * Escapes a Unicode string according to Turtle / N-Triples format.
 * TODO: allow StringBuilder to be null, create one if necessary.
 * @param sb must not be null
 * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
 * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
 */
class TurtleEscaper(sb: StringBuilder, turtle: Boolean) {
  
  /**
   * Escapes a Unicode string according to Turtle / N-Triples format.
   * @param input must not be null
   */
  def escapeTurtle(input: String): Unit = {
    val length = input.length
    var index = 0
    while (index < length)
    {
      val code = input.codePointAt(index)
      index += Character.charCount(code)
      escapeTurtle(code)
    }
  }
  
  /**
   * Escapes a Unicode code point according to Turtle / N-Triples format.
   * @param c Unicode code point
   */
  def escapeTurtle(code: Int) = {
    // TODO: use a lookup table for c <= 0xA0? c <= 0xFF?
         if (code == '\\') sb append "\\\\"
    else if (code == '\"') sb append "\\\""
    else if (code == '\n') sb append "\\n"
    else if (code == '\r') sb append "\\r"
    else if (code == '\t') sb append "\\t"
    else if (code >= 0x0020 && code < 0x007F) sb append code.toChar
    else if (turtle && code >= 0x00A0 && code <= 0xFFFF) sb append code.toChar
    else if (turtle && code >= 0x10000) sb appendCodePoint code
    else if (code <= 0xFFFF) appendHex('u', code, 4)
    else appendHex('U', code, 8)
  }

  private def appendHex(esc: Char, code: Int, digits: Int): StringBuilder = {
    sb append "\\" append esc
    NumberUtils.intToHex(sb, code, digits) 
  }
  
}
