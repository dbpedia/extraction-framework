package org.dbpedia.extraction.scripts

import java.lang.StringBuilder
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.StringUtils.formatCurrentTimestamp
import org.dbpedia.extraction.util.RichFile.wrapFile
import IOUtils._
import java.io.File

/**
 * Maps old quads/triples to new quads/triples.
 */
object QuadMapper {
  
  /**
   * @param input dataset name
   * @param output dataset name
   * @param subjects
   * @param objects
   */
  def mapQuads(finder: DateFinder, input: String, output: String, auto: Boolean = false, required: Boolean = true)(map: Quad => Traversable[Quad]): Unit = {
    // auto only makes sense on the first call to finder.find(), afterwards the date is set
    mapQuads(finder.language.wikiCode, finder.find(input, auto), finder.find(output), required)(map)
  }
    
  /**
   * @param input dataset name
   * @param output dataset name
   * @param subjects
   * @param objects
   */
  def mapQuads(tag: String, inFile: File, outFile: File, required: Boolean)(map: Quad => Traversable[Quad]): Unit = {
    
    if (! inFile.exists()) {
      if (required) throw new IllegalArgumentException(tag+": file "+inFile+" does not exist")
      printerrln(tag+": WARNING - file "+inFile+" does not exist")
      return
    }

    printerrln(tag+": writing "+outFile+" ...")
    var mapCount = 0
    val writer = write(outFile)
    try {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      QuadReader.readQuads(tag, inFile) { old =>
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
    printerrln(tag+": mapped "+mapCount+" quads")
  }
  
}