package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,Language,StringPlusser}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichReader.toRichReader
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,Set,HashMap,MultiMap}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader,BufferedReader,FileNotFoundException}
import org.dbpedia.extraction.destinations.Quad
import java.lang.StringBuilder
import MapUris._

object MapUris {
  
  /** do not map URIs, just copy them */
  val DONT_MAP = 0
  
  /** map URIs, just copy the ones that do not have a mapping */
  val KEEP_UNKNOWN = 1
  
  /** map URIs, discard triples that do not have a mapping */
  val DISCARD_UNKNOWN = 2
}

/**
 * Maps old URIs in triple files to new URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 *   - only triples whose subject and object URIs match a certain filter
 */
class MapUris(baseDir: File, language: Language, suffix: String) {
  
  private val finder = new Finder[File](baseDir, language)
  
  private var date: String = null
  
  private val uriMap = new HashMap[String, Set[String]]() with MultiMap[String, String]
  
  private def find(part: String): File = {
    val name = part + suffix
    if (date == null) date = finder.dates(name).last
    finder.file(date, name)
  }
      
  /**
   * @param map file name part, e.g. interlanguage-links-same-as
   * @param accept test subjectUri, predicateUri, objectUri and decide if the triple should be used
   */
  def readMap(map: String, accept: (String, String, String) => Boolean): Unit = {
    val file = find(map)
    println(language.wikiCode+": reading "+file+" ...")
    var lineCount = 0
    var mapCount = 0
    val start = System.nanoTime
    readLines(file) { line =>
      line match {
        case Quad(quad) if (quad.datatype == null) => {
          if (accept(quad.subject, quad.predicate, quad.value)) {
            uriMap.addBinding(quad.subject, quad.value)
            mapCount += 1
          }
        }
        case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
      }
      lineCount += 1
      if (lineCount % 1000000 == 0) logRead(language.wikiCode, lineCount, start)
    }
    logRead(language.wikiCode, lineCount, start)
    println(language.wikiCode+": found "+mapCount+" URI mappings")
  }
  
  /**
   * @param input dataset name
   * @param output dataset name
   * @param subjectConf one of DONT_MAP, KEEP_UNKNOWN, DISCARD_UNKNOWN
   * @param objectConf one of DONT_MAP, KEEP_UNKNOWN, DISCARD_UNKNOWN
   */
  def mapInput(input: String, output: String, subjects: Int, objects: Int): Unit = {
    val inFile = find(input)
    val outFile = find(output)
    println(language.wikiCode+": reading "+inFile+" ...")
    println(language.wikiCode+": writing "+outFile+" ...")
    var lineCount = 0
    var mapCount = 0
    val start = System.nanoTime
    val writer = write(outFile)
    try {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      readLines(inFile) { line =>
        line.trim match {
          case Quad(quad) => {
            val subjUris = getUris(quad.subject, subjects)
            val objUris = if (quad.datatype == null) getUris(quad.value, objects) else List(null) // null won't be used
            for (subjUri <- subjUris; objUri <- objUris) {
              
              val sb = new StringBuilder
              sb append '<' append subjUri append "> <" append quad.predicate append "> "
              if (quad.datatype == null) {
                sb append '<' append objUri append "> "
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
          case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match triple / quad syntax: " + line)
        }
        lineCount += 1
        if (lineCount % 1000000 == 0) logRead(language.wikiCode, lineCount, start)
      }
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
    }
    finally writer.close()
    logRead(language.wikiCode, lineCount, start)
    println(language.wikiCode+": found "+mapCount+" URI mappings")
  }
  
  private def getUris(uri: String, conf: Int): Traversable[String] = {
    if (conf == DONT_MAP) return List(uri)
    val uris = uriMap.get(uri)
    if (uris.isDefined) uris.get
    else if (conf == KEEP_UNKNOWN) List(uri)
    else List()
  }
  
  private def logRead(name: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}