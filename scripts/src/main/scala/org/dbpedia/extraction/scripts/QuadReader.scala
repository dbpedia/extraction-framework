package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import IOUtils._

/**
 */
class QuadReader(finder: DateFinder) {
  
  /**
   * @param input file name part, e.g. interlanguage-links-same-as
   * @param proc process quad
   */
  def readQuads(input: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    val file = finder.find(input, auto)
    println(finder.language.wikiCode+": reading "+file+" ...")
    var lineCount = 0
    val start = System.nanoTime
    readLines(file) { line =>
      line match {
        case Quad(quad) => {
          proc(quad)
          lineCount += 1
          if (lineCount % 1000000 == 0) logRead(lineCount, start)
        }
        case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match quad or triple syntax: " + line)
      }
    }
    logRead(lineCount, start)
  }
  
  private def logRead(lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(finder.language.wikiCode+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}