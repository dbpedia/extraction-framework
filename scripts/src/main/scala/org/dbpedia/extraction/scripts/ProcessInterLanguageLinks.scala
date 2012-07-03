package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,FileInputStream,FileOutputStream,OutputStreamWriter,FileNotFoundException}
import org.dbpedia.extraction.util.{Finder,Language,ConfigUtils,WikiInfo,ObjectTriple}
import org.dbpedia.extraction.util.ConfigUtils.latestDate
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{HashSet,HashMap}
import scala.io.{Source,Codec}
import org.dbpedia.extraction.destinations.DBpediaDatasets

private class Title(val language: Language, val title: String)

object ProcessInterLanguageLinks {
  
  private val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  private val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  private def open[T](file: File, opener: File => T, wrappers: Map[String, T => T]): T = {
    val name = file.getName
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  def main(args: Array[String]) {
    
    val baseDir = new File(args(0))
    
    // suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
    val suffix = args(1)
    
    // TODO: next 30 lines copy & paste in org.dbpedia.extraction.dump.sql.Import, org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    var keys = for(arg <- args.drop(2); key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
    var languages = SortedSet[Language]()(Language.wikiCodeOrdering)
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case ConfigUtils.Range(from, to) => ranges += ConfigUtils.toRange(from, to)
      case ConfigUtils.Language(language) => languages += Language(language)
      case other => throw new Exception("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(baseDir, WikiInfo.FileName)
      
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      println("parsing "+listFile)
      val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)
      
      // for all wikis in one of the desired ranges...
      for ((from, to) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add its language
        languages += Language(wiki.language)
      }
    }
    
    val titles = new HashMap[String, String]()
    
    val domains = new HashMap[String, Language]()
    for (language <- languages) {
      // TODO: make configurable which languages use generic domain?
      val domain = if (language == Language.English) "dbpedia.org" else language.dbpediaDomain
      domains(domain) = language
    }
    
    val prefix = "http://"
    
    val infix = "/resource/"
    
    def parseUri(uri: String): Title = {
      val slash = uri.indexOf('/', prefix.length)
      val domain = uri.substring(prefix.length, slash)
      domains.get(domain) match {
        case Some(language) => {
          var title = uri.substring(slash + infix.length)
          title = titles.getOrElseUpdate(title, title)
          new Title(language, title)
        }
        case None => null
      }
    }
    
    // language -> title -> language -> title
    val map = new HashMap[Language, HashMap[String, HashMap[Language, String]]]()
  
    val allStart = System.nanoTime
    var allLines = 0L
    var allLinks = 0L
    
    for (language <- languages) {
      val finder = new Finder[File](baseDir, language)
      val name = DBpediaDatasets.InterLanguageLinks.name.replace('_', '-') + suffix
      val file = finder.file(latestDate(finder, name), name)
      
      val start = System.nanoTime
      println("reading "+file+" ...")
      var lines = 0
      var links = 0
      val in = open(file, new FileInputStream(_), unzippers)
      try {
        for (line <- Source.fromInputStream(in, "UTF-8").getLines) {
          line match {
            case ObjectTriple(subjUri, predUri, objUri) => {
              val subj = parseUri(subjUri)
              val obj = parseUri(objUri)
              if (subj != null && obj != null) {
                if (subj.language != language) throw new Exception("subject with wrong language: " + line)
                map.getOrElseUpdate(subj.language, new HashMap()).getOrElseUpdate(subj.title, new HashMap()).update(obj.language, obj.title)
                links += 1
              }
            }
            case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
          }
          lines += 1
          if (lines % 1000000 == 0) log(language.wikiCode, lines, links, start)
        }
      }
      finally in.close()
      log(language.wikiCode, lines, links, start)
      allLines += lines
      allLinks += links
      log("total", allLines, allLinks, allStart)
    }
    
  }
  
  private def log(name: String, lines: Long, links: Long, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": processed "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}