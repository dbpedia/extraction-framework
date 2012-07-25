package org.dbpedia.extraction.scripts

import java.io.{File,Writer,BufferedReader}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.NumberUtils.{intToHex,longToHex,hexToInt,hexToLong}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.destinations.DBpediaDatasets
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.immutable.SortedSet
import scala.collection.mutable.{Map,HashSet,HashMap}
import java.util.Arrays.{copyOf,sort,binarySearch}

/**
 * Split inter-language links into bidirectional and unidirectional links.
 * 
 * Example calls:
 * 
 * 'fr,de' mean specific languages. '-' means no language uses generic domain.
 * '-fr-de' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-fr-de.ttl.gz and enwiki-20120601-interlanguage-links-see-also-fr-de.ttl.gz
 * ../run ProcessInterLanguageLinks /data/dbpedia interlanguage-links-fr-de.txt.gz -fr-de .ttl.gz - fr,de
 *
 * '10000-' means languages by article count range. 'en' uses generic domain.
 * '-' means no file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as.ttl.gz
 * ../run ProcessInterLanguageLinks /data/dbpedia interlanguage-links-ttl.txt.gz - .ttl.gz en 10000-
 *
 * '-' means don't write dump file.
 * ../run ProcessInterLanguageLinks /data/dbpedia - -fr-de .ttl.gz - fr,de
 *
 * if no languages are given, read links from dump file, not from triple files.
 * ../run ProcessInterLanguageLinks /data/dbpedia interlanguage-links.txt.gz - .ttl.gz
 *
 * generate links for all DBpedia I18N chapters in nt format
 * '-chapters' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-chapters.ttl.gz
 * ../run ProcessInterLanguageLinks /data/dbpedia interlanguage-links-chapters-nt.txt.gz -chapters .nt.gz en cs,en,fr,de,el,it,ja,ko,pl,pt,ru,es
 *
 * generate links for all languages that have a namespace on mappings.dbpedia.org in nt format
 * '-mapped' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-mapped.ttl.gz
 * ../run ProcessInterLanguageLinks /data/dbpedia interlanguage-links-mapped-nt.txt.gz -mapped .nt.gz en @mappings
 */
object ProcessInterLanguageLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && (args.length == 4 || args.length >= 6),
      "need at least four args: " +
      /*0*/ "base dir, " +
      /*1*/ "dump file (relative to base dir, use '-' to disable), " +
      /*2*/ "result dataset name extension (e.g. '-chapters', use '-' for empty string), " +
      /*3*/ "triples file suffix (e.g. '.nt.gz'); " +
      /*4*/ "generic domain language (e.g. 'en', use '-' to disable), " +
      /*5*/ "link languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val dumpFile = if (args(1) == "-") null else new File(baseDir, args(1))
    
    val extension = if (args(2) == "-") "" else args(2)
    
    // Suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .ttl or .nt files, using IRIs or URIs.
    // WARNING: This script rejects .nq OR .tql, because it writes ONLY TRIPLES, NOT QUADS.
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
      val languages = parseLanguages(baseDir, args.drop(5))
      
      processor.setLanguages(languages, generic)
      processor.readTriples()
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
  
  Each URI is a combination of language code and title string. There are only ~12 million 
  unique title strings in the top ~100 languages, so we save space by building an index of title 
  strings and using 27 bits (enough for ~130 million titles) of the index number instead of the 
  title string. We use 10 bits (enough for 1024 languages) of the language index instead of the
  language code. Taken together, these 37 bits fit into a Long. The lowest 27 bits the title code,
  the next 10 bits are the language code. -1 is used as the null value.
  
  A link from a page to another page is represented by a Long value which contains the
  concatenation of the values for the page URIs: the upper 27 bits contain the title code
  for the 'from' URI, the next lower 10 bits contain the language code for 'to' URI, and the
  lowest 27 bits contain the title code for the 'to' URI. All links for the 'from' language
  are stored in one array. To find an inverse link, we swap the highest and lowest 27 bits,
  replace the middle 10 bits by the 'from' language, and search the array for the 'to' language
  for the result. To speed up this search, we sort the array and use binary search.
  
  Limitations caused by this bit layout:
  - at most 1024 languages (2^10). We currently use 111.
  - at most ~130 million unique titles (2^27). There currently are ~12 million.
  
  If we break these limits, we're in trouble.
  
  The current code throws ArrayIndexOutOfBoundsExceptions if there are more than 2^10 languages.
  
  Arbitrary limitations:
  - at most ~17 million links per language (2^24). English currently has ~13 million.
  - at most ~17 million unique titles (2^24). The top 100 languages currently have ~12 million.
  
  If we break these limits, we can simply increase the array sizes and give the JVM more heap.
  
  The current code throws an ArrayIndexOutOfBoundsException if there are more than 2^24 links
  for a language or more than 2^24 unique titles.
  
  Note: In the extremely unlikely case that there are exactly 2^10 languages and exactly 2^27
  unique titles, the 'last' URI will be encoded as -1, but -1 is also used as the null value. 
  Strange errors will occur.
  
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
  
  /** language code -> list of link codes. built by readTriples() or readDump(), used by writeTriples() or writeDump() */
  private var links: Array[Array[Long]] = null
  
  /**
   * Build domain index. Must be called before readTriples(). 
   * Initializes the following fields: languages, dates, domains, domainKeys
   */
  def setLanguages(languages: Array[Language], generic: Language) {
    
    if (languages.length > (1 << 10)) throw new ArrayIndexOutOfBoundsException("got "+languages.length+" languages, can't handle more than "+(1 << 10))
    this.languages = languages
    dates = new Array[String](languages.length)
    domains = new Array[String](languages.length)
    domainKeys = new HashMap[String, Int]()
    
    for (langKey <- 0 until languages.length) {
      val language = languages(langKey)
      val domain = if (language == generic) "dbpedia.org" else language.dbpediaDomain
      domains(langKey) = domain
      domainKeys(domain) = langKey
    }
  }
  
  /**
   * Find file in dump directories. Side effect: store date for given language in dates array,
   * if not already set and auto is true.
   * @param part file name part, e.g. interlanguage-links-see-also-chapters
   */
  private def find(langKey: Int, part: String, auto: Boolean = false): File = {
    val finder = new Finder[File](baseDir, languages(langKey))
    val name = part+fileSuffix
    var date = dates(langKey)
    if (date == null) {
      if (! auto) throw new IllegalStateException("date unknown for language "+languages(langKey).wikiCode)
      date = finder.dates(name).last
      dates(langKey) = date
    }
    finder.file(date, name)
  }
  
  /**
   * Read links from interlanguage-links triple files. Accesses (and in the end nulls) titleKeys
   * and domainKeys, so it must be called after setLanguages() and must not be called twice.
   * Must be called before writeTriples() if readDump() is not called.
   * Initializes the following fields: titles, titleKeys, titleCount, links
   * Clears the following fields: titleKeys, domainKeys
   */
  def readTriples(): Unit = {
    
    printerrln("reading triples...")
    
    // Enough space for ~17 million unique titles. The languages with 10000+ articles have ~12 million titles.
    titles = new Array[String](1 << 24)
    titleKeys = new HashMap[String, Int]()
    titleCount = 0
    
    links = new Array[Array[Long]](languages.length)
    
    // Enough space for ~17 million links. English has ~13 million links.
    var langLinks = new Array[Long](1 << 24)
    
    var lineTotal = 0
    var linkTotal = 0
    val startTotal = System.nanoTime
    
    for (langKey <- 0 until languages.length) {
      
      val wikiCode = languages(langKey).wikiCode
      val file = find(langKey, interLinkExtension, true)
      
      printerrln(wikiCode+": reading "+file+" ...")
      var lineCount = 0
      var linkCount = 0
      val start = System.nanoTime
      readLines(file) { line =>
        line match {
          case Quad(quad) if (quad.datatype == null && quad.context == null) => {
            
            val subj = parseUri(quad.subject)
            val subjLang = (subj >>> 27).toInt
            // subj is -1 if its domain was not recognized (problem with generic/specific domain setting?)
            require (subj != -1 && subjLang == langKey, "subject has wrong language - expected "+wikiCode+", found "+(if (subj == -1) "none" else subjLang)+": "+line)
            
            require (quad.predicate == interLinkUri, "wrong property - expected "+interLinkUri+", found "+quad.predicate+": "+line)
            
            val obj = parseUri(quad.value)
            // obj is -1 if its language is not used in this run
            if (obj != -1) {
              // subject title in high 27 bits, object language and title in low 37 bits
              val link = subj << 37 | obj
              
              // Note: If there are more than 2^24 links for a language, this will throw an ArrayIndexOutOfBoundsException
              langLinks(linkCount) = link
              linkCount += 1
              linkTotal += 1
            }
          }
          case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
        }
        lineCount += 1
        lineTotal += 1
        if (lineCount % 1000000 == 0) logRead(wikiCode, lineCount, linkCount, start)
        if (lineTotal % 10000000 == 0) logRead("total", lineTotal, linkTotal, startTotal)
      }
      logRead(wikiCode, lineCount, linkCount, start)
      
      var sortStart = System.nanoTime
      sort(langLinks, 0, linkCount)
      printerrln(wikiCode+": sorted "+linkCount+" links in "+prettyMillis((System.nanoTime - sortStart) / 1000000))
      
      // truncate array to actual size
      links(langKey) = copyOf(langLinks, linkCount)
      
    }
    logRead("total", lineTotal, linkTotal, startTotal)
    
    // garbage collector
    titleKeys = null
    domainKeys = null
  }

  /**
   * Look up language and title in domain and title indexes. Side effect: store title in index
   * if not already set.
   */
  def parseUri(uri: String): Long = {
    val slash = uri.indexOf('/', uriPrefix.length)
    require(uri.startsWith(uriPath, slash), "bad URI, expected path '"+uriPath+"': "+uri)
    val domain = uri.substring(uriPrefix.length, slash)
    domainKeys.get(domain) match {
      case None => -1 // language not in list, ignore this uri
      case Some(langKey) => {
        val title = uri.substring(slash + uriPath.length)
        
        var titleKey = titleKeys.getOrElse(title, -1)
        if (titleKey == -1) {
          titleKey = titleCount
          
          // Note: If there are more than 2^24 unique titles, this will throw an ArrayIndexOutOfBoundsException
          titles(titleKey) = title
          titleKeys(title) = titleKey
          
          titleCount += 1
        }
        
        // Title key in lowest 27 bits, language key in next higher 10 bits.
        // No need to mask title key - it is less than 2^24.
        langKey.toLong << 27 | titleKey.toLong
      }
    }
  }
  
  private def logRead(name: String, lines: Int, links: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    printerrln(name+": read "+lines+" lines, found "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
  /**
   * Open writer for next language.
   * @param langKey
   * @param name file name part, e.g. interlanguage-links-see-also-chapters
   * @return writer for given language
   */
  private def openDataset(langKey: Int, name: String): Writer = {
    val writer = write(find(langKey, name))
    // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
    writer.write("# started "+formatCurrentTimestamp+"\n")
    writer
  }
  
  /**
   * Close writer for current language
   * @param writer 
   * @param langKey may be -1, in which case this method will return null
   * @param name file name part, e.g. interlanguage-links-see-also-chapters
   * @return writer for given language, or null if no language was given
   */
  private def closeDataset(writer: Writer): Unit = {
    // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
    writer.write("# completed "+formatCurrentTimestamp+"\n")
    writer.close()
  }
  
  /**
   * Write triple to given dataset writer.
   * Accesses the following fields: titles, domains
   */
  private def writeTriple(writer: Writer, predUri: String, subjLang: Int, link: Long): Unit = {
    // get subject title from upper 27 bits
    val subjTitle = (link >>> 37).toInt
    // get object language from middle 10 bits
    val objLang = (link >>> 27).toInt & 0x3FF
    // get object title from lower 27 bits
    val objTitle = (link & 0x7FFFFFF).toInt
    writer.write("<"+uriPrefix+domains(subjLang)+uriPath+titles(subjTitle)+"> <"+predUri+"> <"+uriPrefix+domains(objLang)+uriPath+titles(objTitle)+"> .\n")
  }
  
  /**
   * Write links to interlanguage-links-same-as and interlanguage-links-see-also triple files.
   * Must be called after either readDump() or readTriples() and sortLinks().
   * Accesses the following fields: titles, domains, links
   * 
   * TODO: it would be nice if we could also produce quad files, not just triple files.
   * But: we don't want to store all context URIs in memory, so we should process the
   * input files again in this method, not just serialize the link array. We would have to
   * refactor readTriples() so we can re-use its main loop for writeTriples() as well. Only
   * the innermost part of the main loop would differ, and of course some stuff before and
   * after the main loop.
   */
  def writeTriples() {
    
    printerrln("writing triples...")
    
    var total = 0
    var sameAsTotal = 0
    var seeAlsoTotal = 0
    val startTotal = System.nanoTime
    
    for (langKey <- 0 until languages.length) {
      
      val wikiCode = languages(langKey).wikiCode
      
      var sameAsCount = 0
      var seeAlsoCount = 0
      val start = System.nanoTime
      
      val subjLang = langKey.toLong
      
      val langLinks = links(langKey)
      
      val sameAs = openDataset(langKey, sameAsExtension)
      val seeAlso = openDataset(langKey, seeAlsoExtension)
      
      var index = 0
      while (index < langLinks.length) {
        
        val link = langLinks(index)
        
        val objLang = ((link >>> 27) & 0x3FF).toInt
        
        // inverse link: subject titel and object title switched, lang changed
        val inverse = link << 37 | subjLang  << 27 | link >>> 37 
      
        // look for inverse link
        val bidi = binarySearch(links(objLang), inverse) >= 0
        if (bidi) {
          writeTriple(sameAs, sameAsUri, langKey, link)
          sameAsCount += 1
          sameAsTotal += 1
        }
        else {
          writeTriple(seeAlso, seeAlsoUri, langKey, link)
          seeAlsoCount += 1
          seeAlsoTotal += 1
        }
          
        index += 1
        total += 1
        if (index % 10000000 == 0) logWrite(wikiCode, index, sameAsCount, seeAlsoCount, start)
        if (total % 10000000 == 0) logWrite("total", total, sameAsTotal, seeAlsoTotal, startTotal)
      }
      logWrite(wikiCode, index, sameAsCount, seeAlsoCount, start)
      
      closeDataset(sameAs)
      closeDataset(seeAlso)
    }
    logWrite("total", total, sameAsTotal, seeAlsoTotal, startTotal)
  }
  
  private def logWrite(name: String, total: Int, sameAs: Int, seeAlso: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    printerrln(name+": wrote "+total+" links: "+sameAs+" sameAs ("+(100F*sameAs/total)+"%), "+seeAlso+" seeAlso ("+(100F*seeAlso/total)+"%) in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / total)+" micros per link)")
  }
  
  /**
   * Write dump file. Must be called after readTriples() and sortLinks().
   * Accesses the following fields: languages, dates, domains, titles, titleCount, links
   */
  private def writeDump(): Unit = {
    
    val writeStart = System.nanoTime
    printerrln("writing dump file "+dumpFile+" ...")
    val writer = write(dumpFile)
    try {
      var index = 0
      
      printerrln("writing languages, dates and domains...")
      val langCount = languages.length
      writer.write(intToHex(langCount, 8)+"\n")
      index = 0
      while (index < langCount) {
        writer.write(languages(index).wikiCode+"\t"+dates(index)+"\t"+domains(index)+"\n")
        index += 1
      }
      
      printerrln("writing titles...")
      writer.write(intToHex(titleCount, 8)+"\n")
      index = 0
      while (index < titleCount) {
        writer.write(titles(index)+"\n")
        index += 1
      }
      
      printerrln("writing links...")
      for (langKey <- 0 until langCount) {
        val linkStart = System.nanoTime
        val wikiCode = languages(langKey).wikiCode
        val langLinks = links(langKey)
        val linkCount = langLinks.length
        writer.write(intToHex(linkCount, 8)+"\n")
        index = 0
        while (index < linkCount) {
          writer.write(longToHex(langLinks(index), 16)+"\n")
          index += 1
          if (index % 10000000 == 0) logDump(wikiCode, "wrote", index, linkStart)
        }
        logDump(wikiCode, "wrote", index, linkStart)
      }
    }
    finally writer.close()
    printerrln("wrote dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - writeStart) / 1000000))
  }
  
  /**
   * Read dump file. Must be called before writeTriples() if readTriples() is not called.
   * Initializes the following fields: languages, dates, domains, titles, titleCount, links
   */
  def readDump(): Unit = {
    
    val readStart = System.nanoTime
    printerrln("reading dump file "+dumpFile+" ...")
    val reader = new BufferedReader(read(dumpFile))
    try {
      var index = 0
      
      printerrln("reading languages, dates  and domains...")
      val langCount = hexToInt(reader.readLine())
      languages = new Array[Language](langCount)
      dates = new Array[String](langCount)
      domains = new Array[String](langCount)
      index = 0
      while (index < langCount) {
        val line = reader.readLine()
        val parts = line.split("\t", 3)
        languages(index) = Language(parts(0))
        dates(index) = parts(1)
        domains(index) = parts(2)
        index += 1
      }
      
      printerrln("reading titles...")
      titleCount = hexToInt(reader.readLine())
      titles = new Array[String](titleCount)
      index = 0
      while (index < titleCount) {
        titles(index) = reader.readLine()
        index += 1
      }
      
      links = new Array[Array[Long]](langCount)
      for (langKey <- 0 until langCount) {
        val linkStart = System.nanoTime
        val wikiCode = languages(langKey).wikiCode
        val linkCount = hexToInt(reader.readLine())
        val langLinks = new Array[Long](linkCount)
        printerrln(wikiCode+": reading "+linkCount+" links...")
        index = 0
        while (index < linkCount) {
          langLinks(index) = hexToLong(reader.readLine())
          index += 1
          if (index % 10000000 == 0) logDump(wikiCode, "read", index, linkStart)
        }
        logDump(wikiCode, "read", index, linkStart)
        links(langKey) = langLinks
      }
    }
    finally reader.close()
    printerrln("read dump file "+dumpFile+" in "+prettyMillis((System.nanoTime - readStart) / 1000000))
  }
  
  private def logDump(name: String, did: String, index: Int, start: Long): Unit = {
    printerrln(name+": "+did+" "+index+" links in "+prettyMillis((System.nanoTime - start) / 1000000))
  }

}
