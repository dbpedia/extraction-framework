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
import java.util.Arrays.{sort,binarySearch}

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
  
  // TODO: copy & paste in org.dbpedia.extraction.dump.sql.Import, org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
  private def getLanguages(baseDir: File, args: Array[String]): Set[Language] = {
    
    var keys = for(arg <- args; key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
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
    
    languages
  }
  
  def main(args: Array[String]) {
    
    val baseDir = new File(args(0))
    
    // suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
    val suffix = args(1)
    
    // language using generic domain (usually en)
    val generic = Language.getOrElse(args(2), null)
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = getLanguages(baseDir, args.drop(3)).toArray
    
    val domainKeys = new HashMap[String, Int]()
    for (index <- 0 until languages.length) {
      val language = languages(index)
      val domain = if (language == generic) "dbpedia.org" else language.dbpediaDomain
      domainKeys(domain) = index
    }
    
    var titleKey = 0
    val titles = new Array[String](1 << 24)
    val titleKeys = new HashMap[String, Int]()
    
    val prefix = "http://"
    
    val infix = "/resource/"
    
    def parseUri(uri: String): Int = {
      val slash = uri.indexOf('/', prefix.length)
      val domain = uri.substring(prefix.length, slash)
      domainKeys.get(domain) match {
        case Some(language) => {
          val title = uri.substring(slash + infix.length)
          
          var key = titleKeys.getOrElse(title, -1)
          if (key == -1) {
            key = titleKey
            titleKey += 1
            titleKeys(title) = key
            titles(key) = title
          }
          
          // language key in high 8 bits, title key in low 24 bits
          language << 24 | key
        }
        case None => -1
      }
    }
    
    val allStart = System.nanoTime
    var allLines = 0
    var allLinks = 0
    
    var linkKey = 0
    val links = new Array[Long](1 << 28)
    
    for (index <- 0 until languages.length) {
      val language = languages(index)
      val finder = new Finder[File](baseDir, language)
      val name = DBpediaDatasets.InterLanguageLinks.name.replace('_', '-') + suffix
      val file = finder.file(latestDate(finder, name), name)
      
      println("reading "+file+" ...")
      val langStart = System.nanoTime
      var langLines = 0
      var langLinks = 0
      val in = open(file, new FileInputStream(_), unzippers)
      try {
        for (line <- Source.fromInputStream(in, "UTF-8").getLines) {
          line match {
            case ObjectTriple(subjUri, predUri, objUri) => {
              
              val subj = parseUri(subjUri)
              if (subj >>> 24 != index) throw new Exception("subject with wrong language: " + line)
              
              // TODO: check that predUri is correct
              
              val obj = parseUri(objUri)
              // obj is -1 if its language is not used in this run
              if (obj != -1) {
                // subject in high 32 bits, object in low 32 bits
                val link = subj.toLong << 32 | obj.toLong
                links(linkKey) = link
                linkKey += 1
                
                langLinks += 1
              }
            }
            case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
          }
          langLines += 1
          if (langLines % 1000000 == 0) logLoad(language.wikiCode, langLines, langLinks, langStart)
        }
      }
      finally in.close()
      logLoad(language.wikiCode, langLines, langLinks, langStart)
      allLines += langLines
      allLinks += langLinks
      logLoad("total", allLines, allLinks, allStart)
    }
    
    var start = System.nanoTime
    println("sorting "+linkKey+" links...")
    sort(links, 0, linkKey)
    println("sorted "+linkKey+" links in "+prettyMillis((System.nanoTime - start) / 1000000))
    
    start = System.nanoTime
    var index = 0
    var sameAs = 0
    while (index < linkKey) {
      val link = links(index)
      val inverse = link >>> 32 | link << 32
      if (binarySearch(links, 0, linkKey, inverse) >= 0) sameAs += 1
      index += 1
      if (index % 10000000 == 0) logSearch(index, sameAs, start)
    }
    logSearch(index, sameAs, start)
  }
  
  private def logLoad(name: String, lines: Int, links: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": processed "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  private def logSearch(links: Int, found: Int, start: Long): Unit = {
    println("tested "+links+" links, found "+found+" inverse links in "+prettyMillis((System.nanoTime - start) / 1000000))
  }
  
}