package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.scripts.IOUtils._
import org.dbpedia.extraction.destinations.Quad
import java.io.File
import java.lang.StringBuilder

/**
 * Maps old quads/triples to new quads/triples.
 */
class QuadMapper(reader: QuadReader) {
  
  /**
   * @param input dataset name
   * @param output dataset name
   * @param subjects
   * @param objects
   */
  def mapQuads(input: String, output: String)(map: Quad => Traversable[Quad]): Unit = {
    val outFile = reader.find(output)
    println(reader.language.wikiCode+": writing "+outFile+" ...")
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
    println(reader.language.wikiCode+": found "+mapCount+" URI mappings")
  }
  
}