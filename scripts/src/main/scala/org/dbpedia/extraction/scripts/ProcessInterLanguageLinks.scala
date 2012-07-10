package org.dbpedia.extraction.scripts

import java.io.{File,Writer,BufferedReader}
import org.dbpedia.extraction.util.{Finder,Language,ObjectTriple,ConfigUtils}
import org.dbpedia.extraction.util.NumberUtils.{intToHex,longToHex,hexToInt,hexToLong}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichReader.toRichReader
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{Map,HashSet,HashMap}
import java.util.Arrays.{sort,binarySearch}

object ProcessInterLanguageLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && (args.length == 4 || args.length >= 6), 
      "need at least four args: " +
      "base dir, " +
      "dump file (relative to base dir, use '-' to disable), " +
      "result dataset name extension (e.g. '-chapters', use '-' for empty string), " +
      "triples file suffix (e.g. '.nt.gz'); " +
      "optional: generic domain language (e.g. 'en', use '-' to disable), " +
      "link languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val dumpFile = if (args(1) == "-") null else new File(baseDir, args(1))
    
    val extension = if (args(2) == "-") "" else args(2)
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .ttl and .nt files that use IRIs or URIs.
    // WARNING: DOES NOT WORK WITH .nq OR .tql.
    val fileSuffix = args(3)
    
    val processor = new ProcessInterLanguageLinks(baseDir, dumpFile, fileSuffix, extension)
    
    // if no languages are given, read dump file
    if (args.length == 4) {
      require(dumpFile != null, "no dump file and no languages given")
      processor.readDump()
    }
    else {
      // Language using generic domain (usually en)
      val generic = if (args(4) == "-") null else Language(args(4))
      
      // Use all remaining args as keys or comma or whitespace separated lists of keys
      val languages = ConfigUtils.languages(baseDir, args.drop(5))
      
      processor.setLanguages(languages, generic)
      processor.readTriples()
      processor.sortLinks()
      if (dumpFile != null) processor.writeDump()
    }
    
    processor.writeTriples()
  }
  
}

class ProcessInterLanguageLinks(baseDir: File, dumpFile: File, fileSuffix: String, extension: String) {

  val uriPrefix = "http://"
  val uriPath = "/resource/"

  // value copied from InterLanguageLinksExtractor.scala
  val interLinkUri = DBpediaNamespace.ONTOLOGY.append("wikiPageInterLanguageLink")
  val sameAsUri = RdfNamespace.OWL.append("sameAs")
  val seeAlsoUri = RdfNamespace.RDFS.append("seeAlso")

  val interLinkExtension = DBpediaDatasets.InterLanguageLinks.name.replace('_', '-')
  val sameAsExtension = interLinkExtension+"-same-as"+extension
  val seeAlsoExtension = interLinkExtension+"-see-also"+extension
    
  /*
  
  Algorithm:
  
  Each URI is a combination of language code and title string. There are only ~9 million 
  unique title strings in the top ~100 languages, so we save space by building an index of title 
  strings and using 24 bits (enough for ~16 million titles) of the index number instead of the 
  title string. We use 8 bits (enough for 256 languages) of the language index instead of the
  language code. Taken together, these 32 bits fit into an Int. The upper 8 bits are the language
  code, the lower 24 bits the title code. -1 is used as the null value.
  
  A link from a page to another page is represented by a Long value which contains the
  concatenation of the Int values for the page URIs: the upper 32 bits contain the 'from'
  URI, the lower 32 bits contain the 'to' URI. All links are stored in one huge array.
  To find an inverse link, we simply swap the upper and lower 32 bits and search the array 
  for the result. To speed up this search, we sort the array and use binary search.
  
  Limitations caused by this bit layout:
  - at most 256 languages (2^8). We currently use 111.
  - at most ~16 million unique titles (2^24). There currently are ~9 million.
  
  If we use more languages and break these limits, we'll probably need to change the algorithm,
  which won't be easy without letting the memory requirements explode. See below.
  
  The current code throws ArrayIndexOutOfBoundsExceptions if there are more than 2^8 languages
  or more than 2^24 titles.
  
  Arbitrary limitations:
  - at most ~270 million links (2^28). There currently are ~200 million.
  
  If we break that limit, we can simply increase the array size below and give the JVM more heap.
  
  The current code throws an ArrayIndexOutOfBoundsException if there are more than 2^28 links.
  
  Note: In the extremely unlikely case that there are exactly 2^8 languages and exactly 2^24 titles, 
  the 'last' URI will be coded as -1, but -1 is also used as the null value. Strange errors will occur.
  
  ==================================================================================================
  
  Possible data structure for up to 1024 (2^10) languages and ~130 million (2^27) unique titles:
  
  Each 'from' language has its own link array. Each link still takes 64 bits, but the top 27 bits 
  are the 'from' title, the middle 10 bits are the 'to' language, and the bottom 27 bits are the 
  'to' title. 
  
  */
  
  /** language code -> language. built in setLanguages() or readDump(), used everywhere. */
  private var languages: Array[Language] = null
  /** language code -> dump directory date. built by readTriples() or readDump(), used by writeTriples() or writeDump() */
  private var dates: Array[String] = null
  /** language code -> domain. built by readTriples() or readDump(), used by writeTriples() or writeDump() */
  private var domains: Array[String] = null
  /** domain -> language code. built and used by readTriples(). */
  private var domainKeys: Map[String, Int] = null
  
  /** title code -> title. built by readTriples() or readDump(), used by writeTriples() or writeDump() */
  private var titles: Array[String] = null
  /** title -> title code. built and used by readTriples(). */
  private var titleKeys: Map[String, Int] = null
  /** number of titles in array */
  private var titleCount = 0
  
  /** list of link codes. built by readTriples() or readDump(), used by writeTriples() or writeDump() */
  private var links: Array[Long] = null
  /** number of links in array. used by writeTriples() or writeDump() */
  private var linkCount = 0
  
  /**
   * Build domain index. Must be called before readTriples(). 
   * Initializes the following fields: languages, dates, domains, domainKeys
   */
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
  
  /**
   * Find file in dump directories. Side effect: store date for given language in dates array,
   * if not already set and auto is true.
   * @param part file name part, e.g. interlanguage-links-see-also-chapters
   */
  private def find(langCode: Int, part: String, auto: Boolean = false): File = {
    val finder = new Finder[File](baseDir, languages(langCode))
    val name = part+fileSuffix
    var date = dates(langCode)
    if (date == null) {
      if (! auto) throw new IllegalStateException("date unknown for language "+languages(langCode).wikiCode)
      date = finder.dates(name).last
      dates(langCode) = date
    }
    finder.file(date, name)
  }
  
  /**
   * Read links from interlanguage-links triple files. Accesses (and in the end nulls) titleKeys
   * and domainKeys, so it must be called after setLanguages() and must not be called twice.
   * Must be called before writeTriples() if readDump() is not called.
   * Initializes the following fields: titles, titleKeys, titleCount, links, linkCount
   * Clears the following fields: titleKeys, domainKeys
   */
  def readTriples(): Unit = {
    
    // Enough space for ~16 million titles. The languages with 10000+ articles have ~9 million titles.
    titles = new Array[String](1 << 24)
    titleKeys = new HashMap[String, Int]()
    titleCount = 0
    
    // Enough space for ~270 million links. The languages with 10000+ articles have ~200 million links.
    // TODO: either grow the array as needed, or use a list of arrays with e.g. 2^20 items each and
    // create a new array when needed. The single growing array is simpler, but it means that while
    // copying from the old to the new array we need twice the space. With the list of arrays, 
    // there is no copying, but access is slower, and we have to re-implement sort and binary search.
    links = new Array[Long](1 << 28)
    linkCount = 0
    
    var lineCount = 0
    val start = System.nanoTime
    
    for (langCode <- 0 until languages.length) {
      val wikiCode = languages(langCode).wikiCode
      val file = find(langCode, interLinkExtension, true)
      
      println(wikiCode+": reading "+file+" ...")
      val langStart = System.nanoTime
      var langLineCount = lineCount
      var langLinkCount = linkCount
      readLines(file) { line =>
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
      logRead(wikiCode, lineCount - langLineCount, linkCount - langLinkCount, langStart)
      logRead("total", lineCount, linkCount, start)
    }
    
    // garbage collector
    titleKeys = null
    domainKeys = null
  }

  /**
   * Look up language and title in domain and title indexes. Side effect: store title in index
   * if not already set.
   */
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
    println(name+": read "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  /**
   * Sort links so that binary search can be used. Must be called before writeDump() or writeTriples().
   */
  def sortLinks() {
    var sortStart = System.nanoTime
    println("sorting "+linkCount+" links...")
    sort(links, 0, linkCount)
    println("sorted "+linkCount+" links in "+prettyMillis((System.nanoTime - sortStart) / 1000000))
  }
  
  /**
   * Close writer for current language if one is given, open writer for next language if one is given.
   * @param writer may be null
   * @param langCode may be -1, in which case this method will return null
   * @param name file name part, e.g. interlanguage-links-see-also-chapters
   * @return writer for given language, or null if no language was given
   */
  private def switchDataset(writer: Writer, langCode: Int, name: String): Writer = {
    
    if (writer != null) {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
      writer.close()
    }
    
    if (langCode != -1) {
      val writer = write(find(langCode, name))
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      writer
    }
    else {
      null
    }
  }
  
  /**
   * Write triple to given dataset writer.
   * Accesses the following fields: titles, domains
   */
  private def writeTriple(writer: Writer, predUri: String, link: Long): Unit = {
    // get subject from upper 32 bits
    val subj = (link >>> 32).toInt
    // get object from lower 32 bits
    val obj = (link & 0xFFFFFFFFL).toInt
    writer.write("<"+uriPrefix+domain(subj)+uriPath+title(subj)+"> <"+predUri+"> <"+uriPrefix+domain(obj)+uriPath+title(obj)+"> .\n")
  }
  
  /** get domain from upper 8 bits of uri. */
  @inline private def domain(uri: Int): String = domains(uri >>> 24)
  
  /** get title from lower 24 bits of uri. */
  @inline private def title(uri: Int): String = titles(uri & 0xFFFFFF)
  
  /**
   * Write links to interlanguage-links-same-as and interlanguage-links-see-also triple files.
   * Must be called after either readDump() or readTriples() and sortLinks().
   * Accesses the following fields: titles, domains, links, linkCount
   */
  def writeTriples() {
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
        // get subject language from top 8 bits
        subjLang = (link >>> 56).toInt
      }
      
      if (subjLang != lastLang) {
        
        if (lastLang != -1) {
          logWrite(languages(lastLang).wikiCode, index - indexStart, sameAsCount - sameAsStart, seeAlsoCount - seeAlsoStart, nanosStart)
        }
        
        sameAs = switchDataset(sameAs, subjLang, sameAsExtension)
        seeAlso = switchDataset(seeAlso, subjLang, seeAlsoExtension)
        
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
    val micros = (System.nanoTime - start) / 1000
    println(name+": wrote "+total+" links: "+sameAs+" sameAs ("+(100F*sameAs/total)+"%), "+seeAlso+" seeAlso ("+(100F*seeAlso/total)+"%) in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / total)+" micros per link)")
  }
  
  /**
   * Write dump file. Must be called after readTriples() and sortLinks().
   * Accesses the following fields: languages, dates, domains, titles, titleCount, links, linkCount
   */
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
        if (index % 10000000 == 0) logDump("wrote", index, linkStart)
      }
      logDump("wrote", index, linkStart)
    }
    finally writer.close()
    println("wrote dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - writeStart) / 1000000))
  }
  
  /**
   * Read dump file. Must be called before writeTriples() if readTriples() is not called.
   * Initializes the following fields: languages, dates, domains, titles, titleCount, links, linkCount
   */
  def readDump(): Unit = {
    
    val readStart = System.nanoTime
    println("reading dump file "+dumpFile+" ...")
    val reader = new BufferedReader(read(dumpFile))
    try {
      var index = 0
      
      println("reading languages, dates  and domains...")
      val count = hexToInt(reader.readLine())
      languages = new Array[Language](count)
      dates = new Array[String](count)
      domains = new Array[String](count)
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
        if (index % 10000000 == 0) logDump("read", index, linkStart)
      }
      logDump("read", index, linkStart)
    }
    finally reader.close()
    println("read dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - readStart) / 1000000))
  }
  
  private def logDump(did: String, count: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    println(did+" "+count+" of "+linkCount+" links ("+(100F * count / linkCount)+"%) in "+prettyMillis(micros / 1000))
  }

}
