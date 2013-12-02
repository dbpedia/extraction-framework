package org.dbpedia.extraction.scripts

import java.io.File
import scala.Console.err
import scala.collection.mutable.{ArrayBuffer,HashMap,TreeSet}
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.util.ConfigUtils.{loadConfig,parseLanguages,getString,getValue,getStrings}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.{Quad,Destination,CompositeDestination,WriterDestination}
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.IOUtils.{readLines,writer}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.util.text.uri.UriDecoder
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils

/**
 * See https://developers.google.com/freebase/data for a reference of the Freebase RDF data dumps
 */
object ProcessFreebaseLinks
{
  def main(args : Array[String]) {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0), "UTF-8")
    
    val baseDir = getValue(config, "base-dir", true)(new File(_))
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val input = getString(config, "input", true)
    val inputFile = new File(baseDir, input)
    
    val output = getString(config, "output", true)
    
    val languages = parseLanguages(baseDir, getStrings(config, "languages", ',', true))
    
    val formats = parseFormats(config, "uri-policy", "format")

    // destinations for all languages
    val destinations = new HashMap[String, Destination]() {
      // This seems to be the only way to avoid creating an Option object during map lookup.
      override def default(k: String): Destination = null
    }
    
    for (language <- languages) {
      val finder = new Finder[File](baseDir, language, "wiki")
      val date = finder.dates().last
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix)
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      destinations(language.wikiCode) = new CompositeDestination(formatDestinations.toSeq: _*)
    }
    
    val undefined = new TreeSet[String]()
    
    // I didn't find a way to create a singleton Seq without a lot of copying, so we re-use this array.
    // It's silly, but I don't want to be an accomplice in Scala's wanton disregard of efficiency.
    val quads = new ArrayBuffer[Quad](1)
    // Initialize first element
    quads += null
    
    val startNanos = System.nanoTime
    err.println("reading Freebase file "+input+", writing DBpedia files...")
    
    destinations.values.foreach(_.open())
    
    // Freebase files are huge - use Long to count lines, not Int
    var lineCount = 0L
    var linkCount = 0L
    readLines(inputFile) { line =>
      if (line != null) {
        val quad = parseLink(line)
        if (quad != null) {
          val destination = destinations(quad.language)
          if (destination != null) {
            quads(0) = quad
            destination.write(quads)
            linkCount += 1
            if (linkCount % 1000000 == 0) logRead("link", linkCount, startNanos)
          }
          else {
            undefined += quad.language
          }
        }
        lineCount += 1
        if (lineCount % 1000000 == 0) logRead("line", lineCount, startNanos)
      }
    }
    
    destinations.values.foreach(_.close())
    
    logRead("line", lineCount, startNanos)
    logRead("link", linkCount, startNanos)
    
    err.println("links for these languages were found but not written because configuration didn't include them: "+undefined.mkString("[",",","]"))
  }

/*

The lines that we are interested in look exactly like this:

ns:m.0104jp	ns:common.topic.topic_equivalent_webpage	<http://pt.wikipedia.org/wiki/Buna>.

Fields are separated by tabs, not spaces. We simply extract the MID, the language and the title,
create DBpedia and Freebase URIs and create a Quad from them.

There are similar lines that we currently don't care about:

ns:m.0104jp	ns:common.topic.topic_equivalent_webpage	<http://pt.wikipedia.org/wiki/index.html?curid=1409578>.

Wikipedia titles always start with capital letters, so we can exclude these lines because their
titles start with lower-case 'i'.

*/
  
  private val prefix = "ns:m."
  private val midStart = prefix.length
  
  private val infix = "\tns:common.topic.topic_equivalent_webpage\t<http://"
  private val infixLength = infix.length
  
  private val wikipedia = ".wikipedia.org/wiki/"
  private val wikipediaLength = wikipedia.length
  
  private val suffix = ">."
  private val suffixLength = suffix.length
  
  private val sameAs = RdfNamespace.OWL.append("sameAs")

  // store our own private copy of the mutable array
  private val replacements = WikiUtil.iriReplacements
  
  private def parseLink(line: String): Quad = {
    
    if (! line.startsWith(prefix)) return null
    
    val midEnd = line.indexOf('\t', midStart)
    if (midEnd == -1) return null
    
    if (! line.startsWith(infix, midEnd)) return null
    val langStart = midEnd + infixLength
    
    val langEnd = line.indexOf('.', langStart)
    if (langEnd == -1) return null
    
    if (! line.startsWith(wikipedia, langEnd)) return null
    val titleStart = langEnd + wikipediaLength
    
    // exclude ".../wiki/index.html?curid=..." lines
    if (line.charAt(titleStart) == 'i') return null
    
    if (! line.endsWith(suffix)) return null
    val titleEnd = line.length - suffixLength
    
    val mid = line.substring(midStart, midEnd)
    val lang = line.substring(langStart, langEnd)
    var title = line.substring(titleStart, titleEnd)
    
    // Some Freebase URIs are percent-encoded - let's decode them
    title = UriDecoder.decode(title)
    
    // Freebase has some references to sections. Hardly any use for DBpedia.
    if (title.indexOf('#') != -1) return null
    
    // Now let's re-encode the parts that we really want encoded.
    // Also, some Freebase URIs contain percent signs that we need to encode.
    title = StringUtils.escape(title, replacements)
    
    return new Quad(lang, null, "http://"+lang+".dbpedia.org/resource/"+title, sameAs, "http://rdf.freebase.com/ns/m."+mid, null, null)
  }

  private def logRead(name: String, count: Long, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println(name+"s: "+count+" in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per "+name+")")
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}
