package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import scala.Console.err

/**
 */
object QuadReader {
  
  /**
   * @param input file name, e.g. interlanguage-links-same-as.nt.gz
   * @param proc process quad
   */
  def readQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    readQuads(finder.language.wikiCode, finder.byName(input, auto))(proc)
  }

  /**
    * @param pattern regex of filenemes
    * @param proc process quad
    */
  def readQuadsOfMultipleFiles[T <% FileLike[T]](finder: DateFinder[T], pattern: String, auto: Boolean = false)(proc: Quad => Unit): Unit = {
    for(file <- finder.byPattern(pattern, auto))
      readQuads(finder.language.wikiCode, file)(proc)
  }

  def readSortedQuads[T <% FileLike[T]](finder: DateFinder[T], input: String, auto: Boolean = false)(proc: Traversable[Quad] => Unit): Unit = {
    val lastSubj = ""
    var seq = List[Quad]()
    readQuads(finder, input, auto) { quad =>
      if(lastSubj != quad.subject)
      {
        proc(seq)
        seq = List[Quad](quad)
      }
      else{
        seq.::(quad)
      }
    }
    proc(seq)
  }

  /**
   * @param tag for logging
   * @param file input file
   * @param proc process quad
   */
  def readQuads(tag: String, file: FileLike[_])(proc: Quad => Unit): Unit = {
    var lineCount = 0
    val start = System.nanoTime
    val dataset = "(?<=(.*wiki-\\d{8}-))([^\\.]+)".r.findFirstIn(file.toString) match {
      case Some(x) => x
      case None => null
    }
      err.println(tag+": reading "+file+" ...")
      IOUtils.readLines(file) { line =>
        line match {
          case null => // ignore last value
          case Quad(quad) => {
            val copy = quad.copy (
              dataset = dataset
            )
            proc(copy)
            lineCount += 1
            if (lineCount % 100000 == 0) logRead("dataset: " + dataset + " " + tag, lineCount, start)
          }
          case str => if (str.nonEmpty && !str.startsWith("#")) throw new IllegalArgumentException("line did not match quad or triple syntax: " + line)
        }
      }
    logRead("dataset: " + dataset + " " + tag, lineCount, start)
  }
  
  private def logRead(tag: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    err.println(tag+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}