package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.transform.Quad

import scala.Console.err
import java.io.File

import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.config.ConfigUtils.{getString, getStrings, getValue, loadConfig, parseLanguages}
import org.dbpedia.extraction.destinations.formatters.UriPolicy._

import scala.collection.mutable.{ArrayBuffer, HashSet}
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, WriterDestination}
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.util.Finder
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.TurtleUtils
import org.dbpedia.extraction.util.StringUtils
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.iri.UriDecoder

object CreateFlickrWrapprLinks {
  
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0))
    
    val baseDir = getValue(config, "base-dir", true)(new File(_))
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val include = getStrings(config, "include-subjects", ",", true)
    val exclude = getStrings(config, "exclude-subjects", ",", true)
    
    val names = getStrings(config, "namespaces", ",", false)
    val namespaces =
      if (names.isEmpty) null
      // Special case for namespace "Main" - its Wikipedia name is the empty string ""
      else names.map(name => if (name.toLowerCase(Language.English.locale) == "main") Namespace.Main else Namespace(Language.English, name)).toSet

    val output = getString(config, "output", true)
    
    val languages = parseLanguages(baseDir, getStrings(config, "languages", ",", true))
    
    val predSuffix = getString(config, "property-suffix", true)
    
    val objPrefix = getString(config, "object-prefix", true)
    
    val generic = getValue(config, "generic", false)(Language(_))

    val policies = parsePolicies(config, "uri-policy")
    val formats = parseFormats(config, "format", policies)
    // store our own private copy of the mutable array
    val replacements = WikiUtil.iriReplacements
    
    for (language <- languages) {
      
      val finder = new Finder[File](baseDir, language, "wiki")
      val date = finder.dates().last
      
      val inPrefix = if (language == generic) "http://dbpedia.org/resource/" else language.resourceUri.append("")
      
      // we have to use the local URI for writing - formatters will make it generic if necessary
      val subjPrefix = language.resourceUri.append("")
      
      val titles = new HashSet[String]
      
      def processTitles(name: String, include: Boolean): Unit = {

        new QuadMapper().readQuads(language, finder.file(date, name).get) { quad =>
          val subject = quad.subject
          if (! subject.startsWith(inPrefix)) error("bad subject: "+subject)
          
          var title = subject.substring(inPrefix.length)
          
          title = TurtleUtils.unescapeTurtle(title)
          
          if (namespaces == null) {
            // no need to check namespace - do as little as possible, just turn URI into IRI
            title = UriDecoder.decode(title)
            title = StringUtils.escape(title, replacements)
          }
          else {
            // have to check namespace - need full WikiTitle parsing
            val wiki = WikiTitle.parse(title, language)
            if (namespaces.contains(wiki.namespace)) title = wiki.encodedWithNamespace
            else title = null
          }
          
          if (title != null) {
            if (include) titles += title
            else titles -= title
          }
        }
        
      }
      
      for (name <- include) processTitles(name, true)
      for (name <- exclude) processTitles(name, false)
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix).get
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      val destination = new CompositeDestination(formatDestinations.toSeq: _*)
      
      val pred = language.propertyUri.append(predSuffix)
      
      // I didn't find a way to create a singleton Seq without a lot of copying, so we re-use this array.
      // It's silly, but I don't want to be an accomplice in Scala's wanton disregard of efficiency.
      val quads = new ArrayBuffer[Quad](1)
      // Initialize first element
      quads += null
      
      err.println(language.wikiCode+": writing triples...")
      val startNanos = System.nanoTime
      
      destination.open()
      
      var count = 0
      for (title <- titles) {
        val subj = subjPrefix + title
        val obj = objPrefix + title
        quads(0) = new Quad(null, null, subj, pred, obj, null, null: String)
        destination.write(quads)
        count += 1
        if (count % 100000 == 0) logWrite(language.wikiCode, count, startNanos)
      }
      
      destination.close()
      
      logWrite(language.wikiCode, count, startNanos)
    }

  }
  
  private def logWrite(name: String, count: Int, startNanos: Long): Unit = {
    val micros = (System.nanoTime - startNanos) / 1000
    err.println(name+": wrote "+count+" triples in "+StringUtils.prettyMillis(micros / 1000)+" ("+(micros.toFloat / count)+" micros per triple)")
  }
  
  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}
