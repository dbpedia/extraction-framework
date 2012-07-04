package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,FileInputStream,FileOutputStream,OutputStreamWriter,FileNotFoundException}
import org.dbpedia.extraction.util.{Finder,Language,ConfigUtils,WikiInfo,ObjectTriple}
import org.dbpedia.extraction.util.ConfigUtils.latestDate
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{HashSet,HashMap}
import scala.io.{Source,Codec}
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
  
  // value copied from InterLanguageLinksExtractor.scala
  private val interLinkUri = DBpediaNamespace.ONTOLOGY.append("wikiPageInterLanguageLink")
  
  private val sameAsUri = RdfNamespace.OWL.append("sameAs")
  
  private val seeAlsoUri = RdfNamespace.RDFS.append("seeAlso")
  
  def main(args: Array[String]) {
    
    /*
    
    Algorithm:
    
    Each URI is a combination of language code and title string. There are only ~9 million 
    unique title strings in the top ~100 languages, so we save space by building an index of title 
    strings and using 24 bits (enough for 16 million titles) of the index number instead of the 
    title string. We use 8 bits (enough for 255 languages) of the language index instead of the
    language code. Taken together, these 32 bits fit into an Int. The upper 8 bits are the language
    code, the lower 24 bits the title code. -1 is used as the null value.
    
    A link from a page to another page is represented by a Long value which contains the
    concatenation of the Int values for the page titles: the upper 32 bits contain the 'from'
    title, the lower 32 bits contain the 'to' title. All links are stored in one huge array.
    To find an inverse link, we simply swap the upper and lower 32 bits and search the array 
    for the result. To speed up this search, we sort the array and use binary search.
    
    Limitations caused by this bit layout:
    - at most 255 languages (2^8 - 1). We currently use 111.
    - at most ~16 million unique titles (2^24). There currently are ~9 million.
    
    If we use more languages and break these limits, we'll probably need to change the algorithm,
    which won't be easy without letting the memory requirements explode.
    
    Arbitrary limitations:
    - at most ~270 million links (2^28). There currently are ~200 million.
    
    If we break that limit, we can simply increase the array size below and give the JVM more heap.
    
    */
    
    val baseDir = new File(args(0))
    
    // suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
    val suffix = args(1)
    
    // language using generic domain (usually en)
    val generic = Language.getOrElse(args(2), null)
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = getLanguages(baseDir, args.drop(3)).toArray
    
    val domains = new Array[String](1 << 8)
    val domainKeys = new HashMap[String, Int]()
    for (index <- 0 until languages.length) {
      val language = languages(index)
      val domain = if (language == generic) "dbpedia.org" else language.dbpediaDomain
      domains(index) = domain
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
            
            // Note: If there are more than 2^24 titles, this will throw an ArrayIndexOutOfBoundsException
            titles(key) = title
            titleKeys(title) = key
            
            titleKey += 1
          }
          
          // language key in high bits, title key in low bits
          language << 24 | key
        }
        case None => -1 // ignore this uri
      }
    }
    
    val allStart = System.nanoTime
    var allLines = 0
    var allLinks = 0
    
    var linkKey = 0
    // Enough space for ~270 million links. All languages with 10000+ articles contain about 200 million links.
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
              // subj is -1 if its domain was not recognized (generic vs specific?)
              require (subj >>> 24 == index, "subject has wrong language - expected "+languages(index).wikiCode+", found "+(if (subj >>> 24 < languages.length) languages(subj >>> 24).wikiCode else "none")+": "+line)
              
              require (predUri == interLinkUri, "wrong property - expected "+interLinkUri+", found "+predUri+": "+line)
              
              val obj = parseUri(objUri)
              // obj is -1 if its language is not used in this run
              if (obj != -1) {
                // subject in high 32 bits, object in low 32 bits
                val link = subj.toLong << 32 | obj.toLong
                
                // Note: If there are more than 2^28 links, this will throw an ArrayIndexOutOfBoundsException                
                links(linkKey) = link
                linkKey += 1
                
                langLinks += 1
              }
            }
            case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
          }
          langLines += 1
          if (langLines % 1000000 == 0) logRead(language.wikiCode, langLines, langLinks, langStart)
        }
      }
      finally in.close()
      logRead(language.wikiCode, langLines, langLinks, langStart)
      allLines += langLines
      allLinks += langLinks
      logRead("total", allLines, allLinks, allStart)
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
      if (index % 10000000 == 0) logWrite(index, sameAs, start)
    }
    logWrite(index, sameAs, start)
  }
  
  private def logRead(name: String, lines: Int, links: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": processed "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  private def logWrite(links: Int, found: Int, start: Long): Unit = {
    println("tested "+links+" links, found "+found+" inverse links in "+prettyMillis((System.nanoTime - start) / 1000000))
  }
  
}