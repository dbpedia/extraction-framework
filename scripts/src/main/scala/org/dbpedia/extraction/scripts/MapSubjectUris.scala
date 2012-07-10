package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,ObjectTriple,DatatypeTriple,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichReader.toRichReader
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,Set,HashMap,MultiMap}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader,BufferedReader,FileNotFoundException}

object MapSubjectUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 7, 
      "need at least seven args: " +
      "base dir, " +
      "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), "+
      "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      "result dataset name extension (e.g. '-en-uris'), "+
      "triples file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      "new URI domain (e.g. 'en.dbpedia.org', 'dbpedia.org'), " +
      "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val maps = split(args(1))
    require(maps.nonEmpty, "no mapping datasets")
    
    val inputs = split(args(2))
    require(maps.nonEmpty, "no input datasets")
    
    val extension = args(3)
    require(extension.nonEmpty, "no result name extension")
    
    val suffix = args(4)
    require(suffix.nonEmpty, "no file suffix")
    
    val domain = "http://"+args(5)+"/"
    require(! domain.equals("http:///"), "no new domain")
    
    val languages = parseLanguages(baseDir, args.drop(6))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      val mapper = new MapSubjectUris(baseDir, language, suffix)
      for (map <- maps) mapper.readMap(map, domain)
      for (input <- inputs) mapper.mapInput(input, extension)
    }
    
  }
  
}

class MapSubjectUris(baseDir: File, language: Language, suffix: String) {
  
  private val finder = new Finder[File](baseDir, language)
  
  private var date: String = null
  
  private var uriMap = new HashMap[String, Set[String]]() with MultiMap[String, String]
  
  private def find(part: String): File = {
    val name = part + suffix
    if (date == null) date = finder.dates(name).last
    finder.file(date, name)
  }
      
  def readMap(map: String, domain: String): Unit = {
    val file = find(map)
    println(language.wikiCode+": reading "+file+" ...")
    var lineCount = 0
    var mapCount = 0
    val start = System.nanoTime
    readLines(file) { line =>
      line match {
        case ObjectTriple(subjUri, predUri, objUri) => {
          if (objUri.startsWith(domain)) {
            uriMap.addBinding(subjUri, objUri)
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
  
  def mapInput(input: String, extension: String): Unit = {
    val inFile = find(input)
    val outFile = find(input+extension)
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
        line match {
          case DatatypeTriple(subjUri, predUri, value) => {
            for (mapUris <- uriMap.get(subjUri); mapUri <- mapUris) {
              // To change the subject URI, just drop everything up to the first '>'.
              // Ugly, but simple and efficient.
              // Multiple calls to write() are slightly faster than building a new string.
              val index = line.indexOf('>')
              writer.write('<')
              writer.write(mapUri)
              writer.write(line, index, line.length - index)
              writer.write('\n')
              mapCount += 1
            }
          }
          case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match datatype triple syntax: " + line)
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
  
  private def logRead(name: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}