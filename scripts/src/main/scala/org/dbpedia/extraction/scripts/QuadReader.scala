package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import IOUtils._

/**
 */
object QuadReader {
  
  /**
   * @param input file name, e.g. interlanguage-links-same-as.nt.gz
   * @param proc process quad
   */
  def readQuads(finder: DateFinder, input: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    readQuads(finder.language.wikiCode, finder.find(input, auto))(proc)
  }
  
  /**
   * @param tag for logging
   * @param file input file
   * @param proc process quad
   */
  def readQuads(tag: String, file: File)(proc: Quad => Unit): Unit = {
    println(tag+": reading "+file+" ...")
    var lineCount = 0
    val start = System.nanoTime
    readLines(file) { line =>
      line match {
        case Quad(quad) => {
          proc(quad)
          lineCount += 1
          if (lineCount % 1000000 == 0) logRead(tag, lineCount, start)
        }
        case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match quad or triple syntax: " + line)
      }
    }
    logRead(tag, lineCount, start)
  }
  
  private def logRead(tag: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(tag+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}