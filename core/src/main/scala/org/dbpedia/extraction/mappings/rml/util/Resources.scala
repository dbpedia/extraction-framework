package org.dbpedia.extraction.mappings.rml.util

import java.io.InputStream

/**
  * Created by wmaroy on 11.08.17.
  */
object Resources {

  def getAsString(resourcePath : String) : String = {
    val stream : InputStream = getClass.getResourceAsStream(resourcePath)
    val string = scala.io.Source.fromInputStream( stream ).getLines.mkString("\n")
    string
  }

}
