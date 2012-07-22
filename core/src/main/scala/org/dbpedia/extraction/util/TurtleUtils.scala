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
    escapeTurtle(new StringBuilder, input, turtle).toString
  }
  
  /**
   * Escapes a Unicode string according to Turtle / N-Triples format.
   * @param sb must not be null
   * @param input must not be null
   * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
   * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
   */
  def escapeTurtle(sb: StringBuilder, input: String, turtle: Boolean): StringBuilder = {

    val length = input.length

    var offset = 0
    while (offset < length)
    {
      val code = input.codePointAt(offset)
      offset += Character.charCount(code)
      escapeTurtle(sb, code, turtle)
    }
    
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
    // TODO: use a lookup table for c <= 0xA0? c <= 0xFF?
         if (code == '\\') sb append "\\\\"
    else if (code == '\"') sb append "\\\""
    else if (code == '\n') sb append "\\n"
    else if (code == '\r') sb append "\\r"
    else if (code == '\t') sb append "\\t"
    else if (code >= 0x0020 && code < 0x007F) sb append code.toChar
    else if (turtle && code >= 0x00A0 && code <= 0xFFFF) sb append code.toChar
    else if (turtle && code >= 0x10000) sb appendCodePoint code
    else if (code <= 0xFFFF) appendHex(sb, 'u', code, 4)
    else appendHex(sb, 'U', code, 8)
  }

  private def appendHex(sb: StringBuilder, esc: Char, code: Int, digits: Int): StringBuilder = {
    sb append "\\" append esc
    NumberUtils.intToHex(sb, code, digits) 
  }
  
}
