package org.dbpedia.extraction.scripts

import java.io.{File,Writer}
import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.util.ConfigUtils.{loadConfig,parseLanguages,getFile,splitValue}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.{Quad,Destination,CompositeDestination,WriterDestination}
import org.dbpedia.extraction.util.Finder
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.IOUtils

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
    
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}
