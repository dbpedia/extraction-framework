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
      else if (c <= 0xFFFF) appendHex(sb, c, 'u', 4)
      else appendHex(sb, c, 'U', 8)
    }
    
    sb
  }
  
  private def appendHex(sb: StringBuilder, c: Int, u: Char, d: Int) {
    sb append "\\" append u
    NumberUtils.toHex(sb, c, d) 
  }
  
}
