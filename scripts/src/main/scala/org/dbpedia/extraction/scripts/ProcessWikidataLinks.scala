package org.dbpedia.extraction.scripts

import java.io.{File, Writer}
import java.util.Arrays.{copyOf, sort}

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, WriterDestination}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.scripts.ProcessWikidataLinks._
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.config.ConfigUtils.{getString, getStrings, getValue, loadConfig, parseLanguages}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{Finder, IOUtils, Language, SimpleWorkers}
import org.dbpedia.extraction.util.StringUtils.prettyMillis

import scala.Console.err
import scala.collection.Map
import scala.collection.mutable.{ArrayBuffer, HashMap}

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
 * Example call:
 * 
 * ../run ProcessWikidataLinks process.wikidata.links.properties
 *
 */
object ProcessWikidataLinks {
  
  def main(args: Array[String]) {
    
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0))
    
    val baseDir = getValue(config, "base-dir", true)(new File(_))
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val input = getString(config, "input", true)
    
    val output = getString(config, "output", true)
    
    val languages = parseLanguages(baseDir, getStrings(config, "languages", ",", true))

    val policies = parsePolicies(config, "uri-policy")
    val formats = parseFormats(config, "format", policies)
    
    val processor = new ProcessWikidataLinks(baseDir)
    processor.setLanguages(languages)
    processor.readLinks(input)
    processor.writeTriples(output, formats)
  }
  
  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
  private val ITEM_BITS = 27
  private val ITEM_MASK = (1 << ITEM_BITS) - 1 // 27 1-bits
  private val LANG_BITS = 10
  private val LANG_MASK = (1 << LANG_BITS) - 1 // 10 1-bits
  private val TITLE_BITS = 27
  private val TITLE_MASK = (1 << TITLE_BITS) - 1 // 27 1-bits
  
  private val LANGUAGES = (1 << LANG_BITS) - 1 // Wikidata is one 'language'
  private val LINKS = 1 << 25
  private val TITLES = 1 << 25
  
}

class ProcessWikidataLinks(baseDir: File) {

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
  
  /** language code -> language. Wikidata is first. built in setLanguages(), used everywhere. */
  private var languages: Array[Language] = null
  
  /** title code -> title. built by readLinks(), used by writeTriples() */
  private var titles: Array[String] = null
  
  /** links as codes. built by readLinks(), used by writeTriples() */
  private var links: Array[Long] = null
  
  /**
   * Build domain index. Must be called before readLinks(). Sort given array in place. 
   * Initializes the following fields: languages
   */
  def setLanguages(languages: Array[Language]) {
    
    if (languages.length > LANGUAGES) throw new ArrayIndexOutOfBoundsException("got "+languages.length+" languages, can't handle more than "+LANGUAGES)
    
    // sort alphabetically, nicer triples files
    sort(languages, Language.wikiCodeOrdering)
    
    this.languages = new Array[Language](languages.length + 1)
    this.languages(0) = Language.Wikidata
    var index = 0
    while (index < languages.length) {
      this.languages(index + 1) = languages(index)
      index += 1
    }
  }
  
  /**
   * Read links from Wikidata TSV file.
   * Must be called before writeTriples().
   * Initializes the following fields: titles, links
   */
  def readLinks(fileName: String): Unit = {
    
    val startNanos = System.nanoTime
    err.println("reading links...")
    
    // map wiki code (e.g. "enwiki") -> language code (e.g. 5)
    val langKeys = new HashMap[String, Int]().withDefaultValue(-1)
    for (index <- 1 until languages.length) {
      langKeys(languages(index).filePrefix+"wiki") = index
    }
    
    titles = new Array[String](TITLES)
    var titleCount = 0
    val titleKeys = new HashMap[String, Int]().withDefaultValue(-1)
    
    links = new Array[Long](LINKS)
    var linkCount = 0
    
    val finder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    val date = finder.dates().last
    val file = finder.file(date, fileName).get
    
    var lineCount = -1
    IOUtils.readLines(file) { line =>
      
      // process all lines but the first and after last
      if (lineCount != -1 && line != null) {
        
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
            if (titleCount % 1000000 == 0) logRead("title", titleCount, startNanos)
          }
          
          links(linkCount) = id.toLong << (LANG_BITS + TITLE_BITS) | langKey.toLong << TITLE_BITS | titleKey.toLong
          linkCount += 1
          if (linkCount % 1000000 == 0) logRead("link", linkCount, startNanos)
        }
      }
      
      lineCount += 1
      if (lineCount % 1000000 == 0) logRead("line", lineCount, startNanos)
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
    err.println(name+"s: read "+count+" in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per "+name+")")
  }
  
  /**
   * Write links to interlanguage-links-same-as and interlanguage-links-see-also triple files.
   * Must be called after readLinks() and sortLinks().
   * Accesses the following fields: titles, domains, links
   */
  def writeTriples(fileName: String, formats: Map[String, Formatter]) {
    
    err.println("writing triples...")
    val startNanos = System.nanoTime
    
    // destinations for all languages
    val destinations = new Array[Destination](languages.length)
    
    var lang = 0
    while (lang < destinations.length) {
      
      val finder = new Finder[File](baseDir, languages(lang), "wiki")
      val date = finder.dates().last
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, fileName+'.'+suffix).get
        formatDestinations += new WriterDestination(writer(file), format)
      }
      destinations(lang) = new CompositeDestination(formatDestinations.toSeq: _*)
      
      lang += 1
    }
    
    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    // and a queue that's 50% longer than necessary 
    val workers = SimpleWorkers(1.5, 1.5) { job: (Int, Seq[Quad]) =>
      destinations(job._1).write(job._2)
    }
    
    // first open all destinations
    destinations.foreach(_.open())
    // then start worker threads
    workers.start()
    
    val sameAs = RdfNamespace.OWL.append("sameAs")
    
    // URIs for current item
    val uris = new Array[String](languages.length)
    
    var currentId = -1
    var index = 0
    while (index <= links.length) {
      
      var link = -1L
      var id = -1
      if (index < links.length) {
        link = links(index)
        id = (link >>> (LANG_BITS + TITLE_BITS)).toInt
      }
      
      if (currentId != id) {
        
        // write collected links for previous id
        
        if (currentId != -1) {
          // currentId is -1 at the start of the process - nothing to do yet

          // special treatment for Wikidata (language 0)
          uris(0) = languages(0).resourceUri.append("Q"+currentId)
          val pageUri = languages(0).baseUri+"/wiki/Q"+currentId
          
          var subjLang = 0
          while (subjLang < uris.length) {
            
            val subjUri = uris(subjLang)
            if (subjUri != null) {
              val quads = new ArrayBuffer[Quad]()
              
              var objLang = 0
              while (objLang < uris.length) {
                
                val objUri = uris(objLang)
                if (objUri != null && subjLang != objLang) {
                  quads += new Quad(null, null, subjUri, sameAs, objUri, pageUri, null: String)
                }
                
                objLang += 1
              }
              
              workers.process((subjLang, quads))
            }
            
            subjLang += 1
          }
          
        }
        
        if (id == -1) {
          // id is -1 at the end of the process - nothing to do anymore
          // first wait for worker threads to finish
          workers.stop()
          // then close destinations
          destinations.foreach(_.close())
          logWrite(index, startNanos)
          return
        }
        
        // prepare to collect links for next id
        currentId = id
        
        var lang = 0
        while (lang < uris.length) {
          uris(lang) = null
          lang += 1
        }
      }
      
      val lang = (link >>> TITLE_BITS).toInt & LANG_MASK
      val title = (link & TITLE_MASK).toInt
      
      val uri = languages(lang).resourceUri.append(titles(title))
      if (uris(lang) == null) uris(lang) = uri 
      else if (uris(lang) != uri) throw new IllegalArgumentException("multiple links for item "+id+", language "+languages(lang).wikiCode+": "+uris(lang)+", "+uri)
      
      index += 1
      if (index % 100000 == 0) logWrite(index, startNanos)
    }
    
  }
  
  private def logWrite(links: Int, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println("wrote "+links+" links in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / links)+" micros per link)")
  }
  
  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }

}
