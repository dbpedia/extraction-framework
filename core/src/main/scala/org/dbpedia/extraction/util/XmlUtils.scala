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
  
  def escape(target: StringBuilder, str: String) = StringUtils.escape(target, str, escapes)
    
}