package org.dbpedia.extraction.util

import java.lang.StringBuilder

object XmlUtils {
  
  private val escapes = {
    // see scala.xml.Utility.escape for this list of chars
    val escapes = new Array[String](64)
    for (ch <- '\0' until ' ') escapes(ch) = ""
    escapes('<') = "&lt;"
    escapes('>') = "&gt;"
    escapes('&') = "&amp;"
    escapes('"') = "&quot;"
    escapes('\n') = "\n"
    escapes('\r') = "\r"
    escapes('\t') = "\t"
    escapes
  }
  
  /**
   * Does the same thing as scala.xml.Utility.escape, but probably faster.
   * TODO: micro-benchmark to check if and how much this method actually is faster. 
   */
  def escape(target: StringBuilder, str: String) = StringUtils.escape(target, str, escapes)
    
}