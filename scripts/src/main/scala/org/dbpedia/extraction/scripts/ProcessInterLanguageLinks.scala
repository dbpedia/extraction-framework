package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader,BufferedReader,FileNotFoundException}
import org.dbpedia.extraction.util.{Finder,Language,ConfigUtils,WikiInfo,ObjectTriple}
import org.dbpedia.extraction.util.NumberUtils.{intToHex,longToHex,hexToInt,hexToLong}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichBufferedReader.toRichBufferedReader
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{Map,HashSet,HashMap}
import scala.io.{Source,Codec}
import java.util.Arrays.{sort,binarySearch}

private class Title(val language: Language, val title: String)

object ProcessInterLanguageLinks {
  
  // TODO: copy & paste in org.dbpedia.extraction.dump.sql.Import, org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
  private def getLanguages(baseDir: File, args: Array[String]): Array[Language] = {
    
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
    
    languages.toArray
  }
  
  def main(args: Array[String]) {
    
    require(args != null && (args.length == 5 || args.length >= 7), "need at least five args: base dir, dump file (relative to base dir, use '-' to disable), sameAs dataset name, seeAlso dataset name, triples file suffix; optional: generic domain language (use '-' to disable), link languages")
    
    val baseDir = new File(args(0))
    
    val dumpFile = if (args(1) == "-") null else new File(baseDir, args(1))
    
    val sameAsDataset = args(2)
    val seeAlsoDataset = args(3)
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .ttl and .nt files that use IRIs or URIs.
    // WARNING: DOES NOT WORK WITH .nq OR .tql.
    val fileSuffix = args(4)
    
    var processor: ProcessInterLanguageLinks = null
    
    // if no languages are given, read dump file
    if (args.length == 5) {
      require(dumpFile != null, "no dump file and no languages given")
      processor = new ProcessInterLanguageLinks(baseDir, dumpFile, fileSuffix, sameAsDataset, seeAlsoDataset)
      processor.readDump()
    }
    else {
      // Language using generic domain (usually en)
      val generic = if (args(5) == "-") null else Language(args(5))
      
      // Use all remaining args as keys or comma or whitespace separated lists of keys
      val languages = getLanguages(baseDir, args.drop(6))
      
      processor = new ProcessInterLanguageLinks(baseDir, dumpFile, fileSuffix, sameAsDataset, seeAlsoDataset)
      processor.setLanguages(languages, generic)
      processor.readLinks()
      processor.sortLinks()
      if (dumpFile != null) processor.writeDump()
    }
    
    processor.writeLinks()
  }
}

class ProcessInterLanguageLinks(baseDir: File, dumpFile: File, fileSuffix: String, sameAsDataset: String, seeAlsoDataset: String) {

  val uriPrefix = "http://"
  val uriPath = "/resource/"

  // value copied from InterLanguageLinksExtractor.scala
  val interLinkUri = DBpediaNamespace.ONTOLOGY.append("wikiPageInterLanguageLink")
  
  val sameAsUri = RdfNamespace.OWL.append("sameAs")
  val seeAlsoUri = RdfNamespace.RDFS.append("seeAlso")

  /*
  
  Algorithm:
  
  Each URI is a combination of language code and title string. There are only ~9 million 
  unique title strings in the top ~100 languages, so we save space by building an index of title 
  strings and using 24 bits (enough for ~16 million titles) of the index number instead of the 
  title string. We use 8 bits (enough for 256 languages) of the language index instead of the
  language code. Taken together, these 32 bits fit into an Int. The upper 8 bits are the language
  code, the lower 24 bits the title code. -1 is used as the null value.
  
  A link from a page to another page is represented by a Long value which contains the
  concatenation of the Int values for the page titles: the upper 32 bits contain the 'from'
  title, the lower 32 bits contain the 'to' title. All links are stored in one huge array.
  To find an inverse link, we simply swap the upper and lower 32 bits and search the array 
  for the result. To speed up this search, we sort the array and use binary search.
  
  Limitations caused by this bit layout:
  - at most 256 languages (2^8). We currently use 111.
  - at most ~16 million unique titles (2^24). There currently are ~9 million.
  
  If we use more languages and break these limits, we'll probably need to change the algorithm,
  which won't be easy without letting the memory requirements explode.
  
  The current code throws ArrayIndexOutOfBoundsExceptions if there are more than 2^8 languages
  or more than 2^24 titles.
  
  Arbitrary limitations:
  - at most ~270 million links (2^28). There currently are ~200 million.
  
  If we break that limit, we can simply increase the array size below and give the JVM more heap.
  
  The current code throws an ArrayIndexOutOfBoundsException if there are more than 2^28 links.
  
  Note: In the extremely unlikely case that there are exactly 2^8 languages and exactly 2^24 titles, 
  the 'last' URI will be coded as -1, but -1 is also used as the null value. Strange errors will occur.
  
  */
  
  var languages: Array[Language] = null
  var dates: Array[String] = null
  
  var domains: Array[String] = null
  var domainKeys: Map[String, Int] = null
  
  var titles: Array[String] = null
  var titleKeys: Map[String, Int] = null
  var titleCount = 0
  
  var links: Array[Long] = null
  var linkCount = 0
  
  def setLanguages(languages: Array[Language], generic: Language) {
    
    if (languages.length > (1 << 8)) throw new ArrayIndexOutOfBoundsException("got "+languages.length+" languages, can't handle more than "+(1 << 8))
    this.languages = languages
    dates = new Array[String](languages.length)
    domains = new Array[String](languages.length)
    domainKeys = new HashMap[String, Int]()
    
    for (langCode <- 0 until languages.length) {
      val language = languages(langCode)
      val domain = if (language == generic) "dbpedia.org" else language.dbpediaDomain
      domains(langCode) = domain
      domainKeys(domain) = langCode
    }
  }
  
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
  
  private def output(file: File) = open(file, new FileOutputStream(_), zippers)
  
  private def input(file: File) = open(file, new FileInputStream(_), unzippers)
  
  private def write(file: File) = new OutputStreamWriter(output(file), Codec.UTF8)
  
  private def read(file: File) = new BufferedReader(new InputStreamReader(input(file), Codec.UTF8))
  
  /**
   * side effect: find date for given language if not already set in dates array and auto is true
   */
  private def find(langCode: Int, dataset: String, auto: Boolean = false): File = {
    val finder = new Finder[File](baseDir, languages(langCode))
    val name = dataset.replace('_', '-') + fileSuffix
    var date = dates(langCode)
    if (date == null) {
      if (! auto) throw new IllegalStateException("date unknown for language "+languages(langCode).wikiCode)
      date = finder.dates(name).last
      dates(langCode) = date
    }
    finder.file(date, name)
  }
  
  def readLinks(): Unit = {
    
    // Enough space for ~16 million titles. The languages with 10000+ articles have ~9 million titles.
    titles = new Array[String](1 << 24)
    titleKeys = new HashMap[String, Int]()
    titleCount = 0
    
    // Enough space for ~270 million links. The languages with 10000+ articles have ~200 million links.
    links = new Array[Long](1 << 28)
    linkCount = 0
    
    var lineCount = 0
    val start = System.nanoTime
    
    for (langCode <- 0 until languages.length) {
      val wikiCode = languages(langCode).wikiCode
      val file = find(langCode, DBpediaDatasets.InterLanguageLinks.name, true)
      
      println(wikiCode+": reading "+file+" ...")
      val langStart = System.nanoTime
      var langLineCount = lineCount
      var langLinkCount = linkCount
      val reader = read(file)
      try {
        for (line <- reader) {
          line match {
            case ObjectTriple(subjUri, predUri, objUri) => {
              
              val subj = parseUri(subjUri)
              // subj is -1 if its domain was not recognized (generic vs specific?)
              require (subj != -1 && subj >>> 24 == langCode, "subject has wrong language - expected "+wikiCode+", found "+(if (subj == -1) "none" else languages(subj >>> 24).wikiCode)+": "+line)
              
              require (predUri == interLinkUri, "wrong property - expected "+interLinkUri+", found "+predUri+": "+line)
              
              val obj = parseUri(objUri)
              // obj is -1 if its language is not used in this run
              if (obj != -1) {
                // subject in high 32 bits, object in low 32 bits
                val link = subj.toLong << 32 | obj.toLong
                
                // Note: If there are more than 2^28 links, this will throw an ArrayIndexOutOfBoundsException                
                links(linkCount) = link
                linkCount += 1
              }
            }
            case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
          }
          lineCount += 1
          if ((lineCount - langLineCount) % 1000000 == 0) logRead(wikiCode, lineCount - langLineCount, linkCount - langLinkCount, langStart)
        }
      }
      finally reader.close()
      logRead(wikiCode, lineCount - langLineCount, linkCount - langLinkCount, langStart)
      logRead("total", lineCount, linkCount, start)
    }
  }

  def parseUri(uri: String): Int = {
    val slash = uri.indexOf('/', uriPrefix.length)
    require(uri.startsWith(uriPath, slash), "bad URI, expected path '"+uriPath+"': "+uri)
    val domain = uri.substring(uriPrefix.length, slash)
    domainKeys.get(domain) match {
      case None => -1 // language not in list, ignore this uri
      case Some(language) => {
        val title = uri.substring(slash + uriPath.length)
        
        var key = titleKeys.getOrElse(title, -1)
        if (key == -1) {
          key = titleCount
          
          // Note: If there are more than 2^24 titles, this will throw an ArrayIndexOutOfBoundsException
          titles(key) = title
          titleKeys(title) = key
          
          titleCount += 1
        }
        
        // Language key in high bits, title key in low bits. 
        // No need to mask title key - it is less than 2^24.
        language << 24 | key
      }
    }
  }
  
  private def logRead(name: String, lines: Int, links: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(name+": processed "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  def sortLinks() {
    var sortStart = System.nanoTime
    println("sorting "+linkCount+" links...")
    sort(links, 0, linkCount)
    println("sorted "+linkCount+" links in "+prettyMillis((System.nanoTime - sortStart) / 1000000))
  }
  
  private def switchDataset(writer: Writer, langCode: Int = -1, dataset: String = null): Writer = {
    
    if (writer != null) {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
      writer.close()
    }
    
    if (langCode != -1) {
      val writer = write(find(langCode, dataset))
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      writer
    }
    else {
      null
    }
  }
  
  private def writeTriple(writer: Writer, predUri: String, link: Long): Unit = {
    
    // get subject from upper 32 bits
    val subj = (link >>> 32).toInt
    // get domain from upper 8 bits
    val subjDomain = domains(subj >>> 24)
    // get title from lower 24 bits
    val subjTitle = titles(subj & 0xFFFFFF)
    
    // get object from lower 32 bits
    val obj = (link & 0xFFFFFFFFL).toInt
    // get domain from upper 8 bits
    val objDomain = domains(obj >>> 24)
    // get title from lower 24 bits
    val objTitle = titles(obj & 0xFFFFFF)
    
    writer.write("<"+uriPrefix+subjDomain+uriPath+subjTitle+"> <"+predUri+"> <"+uriPrefix+objDomain+uriPath+objTitle+">\n")
  }
  
  def writeLinks() {
    println("writing "+linkCount+" links...")
    
    var lastLang = -1
    
    var nanos = System.nanoTime  // all languages
    var nanosStart = 0L          // current language
    
    var sameAsCount = 0  // all languages
    var sameAsStart = 0  // current language
    var sameAs: Writer = null
    
    var seeAlsoCount = 0  // all languages
    var seeAlsoStart = 0  // current language
    var seeAlso: Writer = null
    
    var index = 0       // all languages
    var indexStart = 0  // current language
    while (true) {
      
      var link = -1L
      var subjLang = -1
      
      if (index < linkCount) {
        link = links(index)
        // get subject language from upper 8 bits
        subjLang = (link >>> 56).toInt
      }
      
      if (subjLang != lastLang) {
        
        if (lastLang != -1) {
          logWrite(languages(lastLang).wikiCode, index - indexStart, sameAsCount - sameAsStart, seeAlsoCount - seeAlsoStart, nanosStart)
        }
        
        sameAs = switchDataset(sameAs, subjLang, sameAsDataset)
        seeAlso = switchDataset(seeAlso, subjLang, seeAlsoDataset)
        
        if (subjLang == -1) {
          logWrite("total", index, sameAsCount, seeAlsoCount, nanos)
          return
        }
        
        lastLang = subjLang
        nanosStart = System.nanoTime
        indexStart = index
        sameAsStart = sameAsCount
        seeAlsoStart = seeAlsoCount
      }
      
      // look for inverse link (subject and object bits switched)
      val bidi = binarySearch(links, 0, linkCount, link << 32 | link >>> 32) >= 0
      if (bidi) {
        writeTriple(sameAs, sameAsUri, link)
        sameAsCount += 1
      }
      else {
        writeTriple(seeAlso, seeAlsoUri, link)
        seeAlsoCount += 1
      }
        
      index += 1
      if (index % 10000000 == 0) logWrite("total", index, sameAsCount, seeAlsoCount, nanos)
    }
  }
  
  private def logWrite(name: String, total: Int, sameAs: Int, seeAlso: Int, start: Long): Unit = {
    println(name+": wrote "+total+" links, "+sameAs+" sameAs ("+(100F*sameAs/total)+"%), "+seeAlso+" seeAlso ("+(100F*seeAlso/total)+"%) in "+prettyMillis((System.nanoTime - start) / 1000000))
  }
  
  private def writeDump(): Unit = {
    
    val writeStart = System.nanoTime
    println("writing dump file "+dumpFile+" ...")
    val writer = write(dumpFile)
    try {
      var index = 0
      
      println("writing languages, dates and domains...")
      val count = languages.length
      writer.write(intToHex(count, 8)+"\n")
      index = 0
      while (index < count) {
        writer.write(languages(index).wikiCode+"\t"+dates(index)+"\t"+domains(index)+"\n")
        index += 1
      }
      
      println("writing titles...")
      writer.write(intToHex(titleCount, 8)+"\n")
      index = 0
      while (index < titleCount) {
        writer.write(titles(index)+"\n")
        index += 1
      }
      
      val linkStart = System.nanoTime
      println("writing "+linkCount+" links...")
      writer.write(intToHex(linkCount, 8)+"\n")
      index = 0
      while (index < linkCount) {
        writer.write(longToHex(links(index), 16)+"\n")
        index += 1
        if (index % 10000000 == 0) logDumpLinks("wrote", index, linkCount, linkStart)
      }
      logDumpLinks("wrote", index, linkCount, linkStart)
    }
    finally writer.close()
    println("wrote dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - writeStart) / 1000000))
  }
  
  def readDump(): Unit = {
    
    val readStart = System.nanoTime
    println("reading dump file "+dumpFile+" ...")
    val reader = read(dumpFile)
    try {
      var index = 0
      
      println("reading languages, dates  and domains...")
      val count = hexToInt(reader.readLine())
      domains = new Array[String](count)
      languages = new Array[Language](count)
      dates = new Array[String](count)
      index = 0
      while (index < count) {
        val line = reader.readLine()
        val parts = line.split("\t", 3)
        languages(index) = Language(parts(0))
        dates(index) = parts(1)
        domains(index) = parts(2)
        index += 1
      }
      
      println("reading titles...")
      titleCount = hexToInt(reader.readLine())
      titles = new Array[String](titleCount)
      index = 0
      while (index < titleCount) {
        titles(index) = reader.readLine()
        index += 1
      }
      
      val linkStart = System.nanoTime
      linkCount = hexToInt(reader.readLine())
      println("reading "+linkCount+" links...")
      links = new Array[Long](linkCount)
      index = 0
      while (index < linkCount) {
        links(index) = hexToLong(reader.readLine())
        index += 1
        if (index % 10000000 == 0) logDumpLinks("read", index, linkCount, linkStart)
      }
      logDumpLinks("read", index, linkCount, linkStart)
    }
    finally reader.close()
    println("read dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - readStart) / 1000000))
  }
  
  private def logDumpLinks(did: String, index: Int, count: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(did+" "+index+" of "+count+" links ("+(100F * index / count)+"%) in "+prettyMillis(micros / 1000))
  }

}
