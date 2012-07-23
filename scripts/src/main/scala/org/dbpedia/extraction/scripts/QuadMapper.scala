package org.dbpedia.extraction.scripts

import java.lang.StringBuilder
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import IOUtils._

/**
 * Maps old quads/triples to new quads/triples.
 */
class QuadMapper(finder: DateFinder) {
  
  private val reader = new QuadReader(finder)
  
  /**
   * @param input dataset name
   * @param output dataset name
   * @param subjects
   * @param objects
   */
  def mapQuads(input: String, output: String, required: Boolean = true)(map: Quad => Traversable[Quad]): Unit = {
    val wikiCode = finder.language.wikiCode
    
    val inFile = finder.find(input)
    if (! inFile.exists()) {
      if (required) throw new IllegalArgumentException(wikiCode+": file "+inFile+" does not exist")
      println(wikiCode+": WARNING - file "+inFile+" does not exist")
      return
    }

    val outFile = finder.find(output)
    println(wikiCode+": writing "+outFile+" ...")
    var mapCount = 0
    val writer = write(outFile)
    try {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      reader.readQuads(input) { old =>
        for (quad <- map(old)) {
          val sb = new StringBuilder
          sb append '<' append quad.subject append "> <" append quad.predicate append "> "
          if (quad.datatype == null) {
            sb append '<' append quad.value append "> "
          }
          else {
            sb  append '"' append quad.value append '"'
            if (quad.language != null) sb append '@' append quad.language append ' '
            else if (quad.datatype != "http://www.w3.org/2001/XMLSchema#string") sb append "^^<" append quad.datatype append "> "
          }
          if (quad.context != null) sb append '<' append quad.context append "> "
          sb append ".\n"
          writer.write(sb.toString)
          
          mapCount += 1
        }
      }
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
    }
    finally writer.close()
    println(wikiCode+": mapped "+mapCount+" quads")
  }
  
}