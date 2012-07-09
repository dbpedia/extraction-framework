package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,ConfigUtils,ObjectTriple,Language}
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichReader.toRichReader
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,Set,HashMap,HashSet}
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
    
    val languages = ConfigUtils.languages(baseDir, args.drop(6))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      val mapper = new MapSubjectUris(baseDir, language, suffix)
      mapper.readMaps(maps, domain)
    }
    
  }
  
}

class MapSubjectUris(baseDir: File, language: Language, suffix: String) {
  
  private val finder = new Finder[File](baseDir, language)
  
  private var date: String = null
  
  private var uriMap: Map[String, Set[String]] = null
  
  private def find(part: String): File = {
    val name = part + suffix
    if (date == null) date = finder.dates(name).last
    finder.file(date, name)
  }
      
  def readMaps(maps: Array[String], domain: String): Unit = {
    uriMap = new HashMap[String, Set[String]]()
    for (map <- maps) {
      val file = find(map)
      println(language.wikiCode+": reading "+file+" ...")
      var lineCount = 0
      var mapCount = 0
      val start = System.nanoTime
      readLines(file) { line =>
        line match {
          case ObjectTriple(subjUri, predUri, objUri) => {
            if (objUri.startsWith(domain)) {
              uriMap.getOrElseUpdate(subjUri, new HashSet[String]()) += objUri
              mapCount += 1
            }
          }
          case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
        }
        lineCount += 1
        if (lineCount % 2000000 == 0) logRead(language.wikiCode, lineCount, start)
      }
      logRead(language.wikiCode, lineCount, start)
      println(language.wikiCode+": found "+mapCount+" URI mappings")
    }
    
    val file = find("bla")
    println("writing "+file+" ...")
    val writer = write(file)
    try {
      for ((subjUri, objUri) <- uriMap) {
        writer.write("<"+subjUri+"> <http://www.w3.org/2002/07/owl#sameAs> <"+objUri+"> .\n")
      }
    }
    finally writer.close()
  }
  
  private def logRead(name: String, lines: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": read "+lines+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}