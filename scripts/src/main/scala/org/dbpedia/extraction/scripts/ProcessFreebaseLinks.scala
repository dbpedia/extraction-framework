package org.dbpedia.extraction.scripts

import java.io.{File,Writer}
import scala.Console.err
import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.util.ConfigUtils.{loadConfig,parseLanguages,getFile,splitValue}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.{Quad,Destination,CompositeDestination,WriterDestination}
import org.dbpedia.extraction.util.Finder
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.ontology.RdfNamespace

/**
 * See https://developers.google.com/freebase/data for a reference of the Freebase RDF data dumps
 */
object ProcessFreebaseLinks
{
  def main(args : Array[String]) {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0), "UTF-8")
    
    val baseDir = getFile(config, "base-dir")
    if (baseDir == null) throw error("property 'base-dir' not defined.")
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val input = config.getProperty("input")
    if (input == null) throw error("property 'input' not defined.")
    val inputFile = new File(baseDir, input)
    
    val output = config.getProperty("output")
    if (output == null) throw error("property 'output' not defined.")
    
    val languages = parseLanguages(baseDir, splitValue(config, "languages", ','))
    
    val formats = parseFormats(config, "uri-policy", "format")

    // destinations for all languages
    val destinations = new Array[Destination](languages.length)
    
    var lang = 0
    while (lang < destinations.length) {
      
      val finder = new Finder[File](baseDir, languages(lang), "wiki")
      val date = finder.dates().last
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix)
        formatDestinations += new WriterDestination(writer(file), format)
      }
      destinations(lang) = new CompositeDestination(formatDestinations.toSeq: _*)
      
      lang += 1
    }
    
    val startNanos = System.nanoTime
    err.println("reading Freebase file...")
    
    // Freebase files are huge - use Long to count lines, not Int
    var lineCount = 0L
    var linkCount = 0L
    IOUtils.readLines(inputFile) { line =>
      if (line != null) {
        val quad = parseLink(line)
        if (quad != null) {
          linkCount += 1
          if (linkCount % 1000000 == 0) {
            err.println(quad)
            logRead("link", linkCount, startNanos)
          }
        }
        lineCount += 1
        if (lineCount % 1000000 == 0) logRead("line", lineCount, startNanos)
      }
    }
    logRead("line", lineCount, startNanos)
    logRead("link", linkCount, startNanos)
    
  }
  
  /*
The lines that we are interested in look like this:

ns:m.0104jp	ns:common.topic.topic_equivalent_webpage	<http://pt.wikipedia.org/wiki/Buna>.

Fields are separated by tabs, not spaces. We simply extract the MID, the language and the title.

There are similar lines that we currently don't care about:

ns:m.0104jp	ns:common.topic.topic_equivalent_webpage	<http://pt.wikipedia.org/wiki/index.html?curid=1409578>.

Wikipedia titles always start with capital letters, so we can exclude these lines because their
titles start with lower-case i.
  */
  
  val prefix = "ns:m."
  val midStart = prefix.length
  
  val infix = "\tns:common.topic.topic_equivalent_webpage\t<http://"
  val infixLength = infix.length
  
  val wikipedia = ".wikipedia.org/wiki/"
  val wikipediaLength = wikipedia.length
  
  val suffix = ">."
  val suffixLength = suffix.length
  
  val sameAs = RdfNamespace.OWL.append("sameAs")

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
    
    if (line.charAt(titleStart) == 'i') return null
    
    if (! line.endsWith(suffix)) return null
    val titleEnd = line.length - suffixLength
    
    val mid = line.substring(midStart, midEnd)
    val lang = line.substring(langStart, langEnd)
    val title = line.substring(titleStart, titleEnd)
    
    return new Quad(lang, null, "http://"+lang+".dbpedia.org/resource/"+title, sameAs, "http://rdf.freebase.com/ns/m."+mid, null, null)
  }

  private def logRead(name: String, count: Long, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println(name+"s: "+count+" in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per "+name+")")
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}
