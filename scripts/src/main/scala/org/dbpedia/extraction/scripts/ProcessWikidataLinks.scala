package org.dbpedia.extraction.scripts

import java.io.{File,OutputStreamWriter}
import scala.Console.{err,out}
import scala.collection.mutable.{Map,HashMap,ArrayBuffer}
import java.util.Arrays.{copyOf,sort}
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.StringUtils.{prettyMillis}
import org.dbpedia.extraction.ontology.{RdfNamespace,DBpediaNamespace}
import org.dbpedia.extraction.destinations.{Destination,Quad,QuadBuilder,Dataset,WriterDestination}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import ProcessWikidataLinks._
import org.dbpedia.extraction.destinations.WriterDestination
import org.dbpedia.extraction.destinations.formatters.TerseFormatter

/**
 * Generate separate triple files for each language from Wikidata link file.
 * 
 * Input format:
 * 
 * ips_row_id ips_item_id ips_site_id ips_site_page
 * 55 3596065 abwiki Џьгьарда
 * 56 3596037 abwiki Џьырхәа
 * 58 3596033 abwiki Аацы
 * ...
 * 17374035 868895 zh_yuewiki As50
 * 17374052 552300 zh_yuewiki Baa, Baa, Black Sheep
 * 17374062 813114 zh_yuewiki Beatcollective
 * ...
 * 257464664 3176059 frwiki Jeanne Granier
 * 257464665 3176059 enwiki Jeanne Granier
 * 257464677 7465026 ptwiki 36558 2000 QP105
 * 257464678 8275441 frwiki Catégorie:Îles Auckland
 * ...
 * 
 * Fields are tab-separated. Titles contain spaces. Lines are sorted by row ID. For us this means
 * they're basically random.
 * 
 * Example calls:
 * 
 * 'fr,de' mean specific languages. '-' means no language uses generic domain.
 * '-fr-de' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-fr-de.ttl.gz and enwiki-20120601-interlanguage-links-see-also-fr-de.ttl.gz
 * ../run ProcessWikidataLinks /data/dbpedia -fr-de .ttl.gz - fr,de
 *
 * '10000-' means languages by article count range. 'en' uses generic domain.
 * '-' means no file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as.ttl.gz
 * ../run ProcessWikidataLinks /data/dbpedia - .ttl.gz en 10000-
 *
 * '-' means don't write dump file.
 * ../run ProcessWikidataLinks /data/dbpedia - -fr-de .ttl.gz - fr,de
 *
 * if no languages are given, read links from dump file, not from triple files.
 * ../run ProcessWikidataLinks /data/dbpedia - .ttl.gz
 *
 * generate links for all DBpedia I18N chapters in nt format
 * '-chapters' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-chapters.ttl.gz
 * ../run ProcessWikidataLinks /data/dbpedia -chapters .nt.gz en cs,en,fr,de,el,it,ja,ko,pl,pt,ru,es
 *
 * generate links for all languages that have a namespace on mappings.dbpedia.org in nt format
 * '-mapped' is file name part, full names are e.g. enwiki-20120601-interlanguage-links-same-as-mapped.ttl.gz
 * ../run ProcessWikidataLinks /data/dbpedia -mapped .nt.gz en @mappings
 */
object ProcessWikidataLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && (args.length == 4 || args.length >= 6),
      "need at least four args: " +
      /*0*/ "base dir, " +
      /*1*/ "input file, " +
      /*2*/ "generic domain language (e.g. 'en', use '-' to disable), " +
      /*3*/ "link languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    var inputFile = new File(args(1))
    if (! inputFile.isAbsolute) inputFile = new File(baseDir, args(1))
    
    // Language using generic domain (usually en)
    val generic = if (args(2) == "-") null else Language(args(2))
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    val languages = parseLanguages(baseDir, args.drop(3))
    
    val processor = new ProcessWikidataLinks()
    processor.setLanguages(languages, generic)
    processor.readLinks(inputFile)
    processor.writeTriples()
  }
  
  private val ITEM_BITS = 27
  private val ITEM_MASK = (1 << ITEM_BITS) - 1 // 27 1-bits
  private val LANG_BITS = 10
  private val LANG_MASK = (1 << LANG_BITS) - 1 // 10 1-bits
  private val TITLE_BITS = 27
  private val TITLE_MASK = (1 << TITLE_BITS) - 1 // 27 1-bits
  
  private val LANGUAGES = 1 << LANG_BITS
  private val LINKS = 1 << 25
  private val TITLES = 1 << 25
  
}

class ProcessWikidataLinks() {

  /*
  
  Algorithm:
  
  A link is represented by Wikidata item ID, language code and title code. As of 2013-06-18,
  there are 30.7 million links, 12.6 million Wikidata items, 286 languages and 8.1 million 
  unique titles. Each link is represented by the 64 bits of a long: 27 highest bits for Wikidata
  item ID (enough for ~130 million items), 10 middle bits for language (enough for 1024 languages),
  27 lowest bits for title (enough for ~130 million titles).
  
  Limitations caused by this bit layout:
  - at most ~130 million Wikidata items (2^27). There currently are ~12.6 million.
  - at most 1024 languages (2^10). There currently are 286.
  - at most ~130 million unique titles (2^27). There currently are ~8.1 million.
  
  If we break these limits, we're in trouble.
  
  The current code throws ArrayIndexOutOfBoundsExceptions if there are more than 2^10 languages.
  
  Arbitrary limitations:
  - at most ~34 million links (2^25). There currently are ~30 million.
  - at most ~34 million unique titles (2^25).
  
  If we break these limits, we can simply increase the array sizes and give the JVM more heap.
  
  The current code throws an ArrayIndexOutOfBoundsException if there are more than 2^25 links
  for a language or more than 2^23 unique titles.
  
  TODO: it's a waste of space to store each character of each title separately. Maybe a trie 
  could reduce space requirements.
  
  */
  
  /** language code -> language. built in setLanguages(), used everywhere. */
  private var languages: Array[Language] = null
  /** language using generic IRIs (http://dbpedia.org). set in setLanguages(), used by writeTriples(). */
  private var generic: Language = null
  
  /** title code -> title. built by readLinks(), used by writeTriples() */
  private var titles: Array[String] = null
  
  /** links as codes. built by readLinks(), used by writeTriples() */
  private var links: Array[Long] = null
  
  /**
   * Build domain index. Must be called before readLinks(). 
   * Initializes the following fields: languages, dates, domains, wikiCodes
   */
  def setLanguages(languages: Array[Language], generic: Language) {
    if (languages.length > LANGUAGES) throw new ArrayIndexOutOfBoundsException("got "+languages.length+" languages, can't handle more than "+LANGUAGES)
    // sort alphabetically, nicer triples files
    sort(languages, Language.wikiCodeOrdering)
    this.languages = languages
    this.generic = generic
  }
  
  /**
   * Read links from Wikidata TSV file.
   * Must be called before writeTriples().
   * Initializes the following fields: titles, links
   */
  def readLinks(file: File): Unit = {
    
    // map wiki code (e.g. "enwiki") -> language code (e.g. 5)
    val langKeys = new HashMap[String, Int]().withDefaultValue(-1)
    for (index <- 0 until languages.length) {
      langKeys(languages(index).filePrefix+"wiki") = index
    }
    
    titles = new Array[String](TITLES)
    var titleCount = 0
    val titleKeys = new HashMap[String, Int]().withDefaultValue(-1)
    
    links = new Array[Long](LINKS)
    var linkCount = 0
    
    val startNanos = System.nanoTime
    var lineCount = -1
    
    IOUtils.readLines(file) { line =>
      
      // process all lines but the first
      if (lineCount != -1) {
        
        val parts = line.split('\t')
        require(parts.length == 4, "invalid link line: ["+line+"]")
        
        val langKey = langKeys(parts(2))
        // only parse line if language is configured
        if (langKey != -1) {
          
          val id = parts(1).toInt
          
          val title = parts(3)
          var titleKey = titleKeys(title)
          if (titleKey == -1) {
            titleKey = titleCount
            titles(titleKey) = title
            titleKeys(title) = titleKey
            titleCount += 1
          }
          
          links(linkCount) = id.toLong << (LANG_BITS + TITLE_BITS) | langKey.toLong << TITLE_BITS | titleKey.toLong
          linkCount += 1
          if (linkCount % 200000 == 0) logRead("link", linkCount, startNanos)
        }
      }
      
      lineCount += 1
      if (lineCount % 200000 == 0) logRead("line", lineCount, startNanos)
    }
    logRead("line", lineCount, startNanos)
    logRead("link", linkCount, startNanos)
    logRead("title", titleCount, startNanos)
    
    // truncate arrays to actual size
    links = copyOf(links, linkCount)
    titles = copyOf(titles, titleCount)
    
    var sortStart = System.nanoTime
    sort(links)
    err.println("sorted "+linkCount+" links in "+prettyMillis((System.nanoTime - sortStart) / 1000000))
  }

  private def logRead(name: String, count: Int, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println("read "+count+" "+name+"s in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per "+name+")")
  }
  
  /**
   * Write links to interlanguage-links-same-as and interlanguage-links-see-also triple files.
   * Must be called after readLinks() and sortLinks().
   * Accesses the following fields: titles, domains, links
   */
  def writeTriples() {
    
    err.println("writing triples...")
    
    val destinations = new Array[Destination](languages.length)
    
    var lang = 0
    while (lang < languages.length) {
      destinations(lang) = new WriterDestination(() => new OutputStreamWriter(out), new TerseFormatter(true, true))
      lang += 1
    }
    // TODO: find language folders, configure formatters
    
    destinations.foreach(_.open())
    
    val sameAs = RdfNamespace.OWL.append("sameAs")
    val resourceUris = languages.map(lang => if (lang == generic) new DBpediaNamespace("http://dbpedia.org/resource/") else lang.resourceUri)
    
    var currentId = -1
    val uris = new Array[String](languages.length)
    
    val startNanos = System.nanoTime
    
    var index = 0
    while (index <= links.length) {
      
      var link = -1L
      var id = -1
      if (index < links.length) {
        link = links(index)
        id = (link >>> (LANG_BITS + TITLE_BITS)).toInt
      }
      
      if (currentId != id) {
        
        // currentId is -1 at the start of the process - nothing to do yet
        if (currentId != -1) {

          val entityUri = "http://www.wikidata.org/entity/Q"+currentId
          val pageUri = "http://www.wikidata.org/wiki/Q"+currentId
          
          var subjLang = 0
          while (subjLang < languages.length) {
            
            val subjUri = uris(subjLang)
            if (subjUri != null) {
              
              val quads = new ArrayBuffer[Quad]()
              
              quads += new Quad(null, null, subjUri, sameAs, entityUri, pageUri, null: String)
              
              var objLang = 0
              while (objLang < languages.length) {
                
                val objUri = uris(objLang)
                if (objUri != null) {
                  
                  quads += new Quad(null, null, subjUri, sameAs, objUri, pageUri, null: String)
                }
                
                objLang += 1
              }
              
              destinations(subjLang).write(quads)
            }
            
            subjLang += 1
          }
          
        }
        
        // id is -1 at the end of the process - nothing to do anymore
        if (id == -1) {
          destinations.foreach(_.close())
          logWrite(index, startNanos)
          return;
        }
          
        var lang = 0
        while (lang < languages.length) {
          uris(lang) = null
          lang += 1
        }
        
        currentId = id
      }
      
      val lang = (link >>> TITLE_BITS).toInt & LANG_MASK
      val title = (link & TITLE_MASK).toInt
      
      val uri = resourceUris(lang).append(titles(title))
      if (uris(lang) != null) throw new IllegalArgumentException("duplicate link for item "+id+", language "+languages(lang).wikiCode+": "+uris(lang)+", "+uri)
      uris(lang) = uri
      
      index += 1
      if (index % 200000 == 0) logWrite(index, startNanos)
    }
    
  }
  
  private def logWrite(links: Int, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println("wrote "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / links)+" micros per link)")
  }
  
}
