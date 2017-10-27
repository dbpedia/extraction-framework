package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.transform.Quad

import scala.Console.err
import scala.collection.mutable.{ArrayBuffer, HashMap, TreeSet}
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.config.ConfigUtils.{getString, getStrings, getValue, loadConfig, parseLanguages}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, WriterDestination}
import org.dbpedia.extraction.util.Finder
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.IOUtils.{readLines, writer}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils
import org.dbpedia.iri.UriDecoder

/**
 * See https://developers.google.com/freebase/data for a reference of the Freebase RDF data dumps
 */
object ProcessFreebaseLinks
{
  def main(args : Array[String]) {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0))

    val baseDir = getValue(config, "base-dir", true)(new File(_))
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")

    val input = getString(config, "input", true)
    val inputFile = new File(baseDir, input)

    val output = getString(config, "output", true)

    val languages = parseLanguages(baseDir, getStrings(config, "languages", ",", true))

    val policies = parsePolicies(config, "uri-policy")
    val formats = parseFormats(config, "format", policies)

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
        val file = finder.file(date, output+'.'+suffix).get
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

We use the <http://rdf.freebase.com/key/wikipedia.XX> predicate for finding the Wikipedia page for language XX.

<http://rdf.freebase.com/ns/m.0108bjs0> <http://rdf.freebase.com/key/wikipedia.en>      "2014_Porsche_Tennis_Grand_Prix_$2013_Singles"  .
Fields are separated by an arbitrary number of spaces. We simply extract the MID, the language and the title, create
DBpedia and Freebase URIs and create a Quad from them.

*/
  
  private val part1 = "<http://rdf.freebase.com/ns/m."
  private val midStart = part1.length
  
  private val predicatePart = "<http://rdf.freebase.com/key/wikipedia."
  private val predicatePartLength = predicatePart.length

  private val preLanguageCode = "<http://"
  private val preLanguageCodeLength = preLanguageCode.length

  private val wikipediaUriInfix = ".wikipedia.org/wiki/"
  private val wikipediaUriInfixLength = wikipediaUriInfix.length
  
  private val suffix = ">"
  private val suffixLength = suffix.length

  private val lineTerminator = "."
  private val lineTerminatorLength = lineTerminator.length
  
  private val sameAs = RdfNamespace.OWL.append("sameAs")

  // store our own private copy of the mutable array
  private val replacements = WikiUtil.iriReplacements
  
  private def parseLink(line: String): Quad = {
    
    if (! line.startsWith(part1)) return null
    
    val midEnd = line.indexOf('>', midStart)
    if (midEnd == -1) return null

    val predicatePartStart = line.indexOf('<', midEnd + 1)

    if (! line.startsWith(predicatePart, predicatePartStart)) return null

    val langStart = predicatePartStart + predicatePartLength

    val langEnd = line.indexOf('>', langStart)
    if (langEnd == -1) return null

    val titleStart = line.indexOf("\"", langEnd)
    if (titleStart == -1) return null

    val titleEnd = line.indexOf("\"", titleStart + 1)
    if (titleEnd == -1) return null

    val mid = line.substring(midStart, midEnd)
    val lang = line.substring(langStart, langEnd)
    var title = unescapeMQL(line.substring(titleStart + 1, titleEnd))

    // Some Freebase URIs are percent-encoded - let's decode them
    title = UriDecoder.decode(title)
    
    // Freebase has some references to sections. Hardly any use for DBpedia.
    if (title.indexOf('#') != -1) return null
    
    // Now let's re-encode the parts that we really want encoded.
    // Also, some Freebase URIs contain percent signs that we need to encode.
    title = StringUtils.escape(title, replacements)
    
    return new Quad(lang, null, "http://"+lang+".dbpedia.org/resource/"+title, sameAs, "http://rdf.freebase.com/ns/m."+mid, null, null)
  }

  /**
   * Unescapes the given MQL escaped key and returns the resulting string.
 *
   * @param key key containing MQL escape sequences
   * @return unescaped key
   */
  private def unescapeMQL(key: String) : String = {
    val inputArray: Array[Char] = key.toCharArray
    val inputLength: Int = inputArray.length
    val res = new Array[Char](inputLength)

    var inputIndex = 0
    var outputIndex = 0

    while (inputIndex < inputLength) {
      if (inputArray(inputIndex) == '$') {
        // beginning of an MQL escape sequence
        res(outputIndex) = Integer.parseInt(key.substring(inputIndex + 1, inputIndex + 5), 16).asInstanceOf[Char]
        inputIndex += 5
        outputIndex += 1
      }
      else {
        // just copy character
        res(outputIndex) = inputArray(inputIndex)
        outputIndex += 1
        inputIndex += 1
      }
    }

    new String(res, 0, outputIndex)
  }

  private def logRead(name: String, count: Long, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println(name+"s: "+count+" in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per "+name+")")
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
}
