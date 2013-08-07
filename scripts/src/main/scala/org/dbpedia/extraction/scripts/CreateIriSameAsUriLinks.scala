package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.ConfigUtils.{loadConfig,parseLanguages,getFile,splitValue}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.destinations.{Destination,CompositeDestination,WriterDestination}
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.util.Finder

object CreateIriSameAsUriLinks {
  
  def main(args: Array[String]): Unit = {
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

    for (language <- languages) {
      
      val finder = new Finder[File](baseDir, language, "wiki")
      val date = finder.dates().last
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix)
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      val destinations = new CompositeDestination(formatDestinations.toSeq: _*)
      
      //   for each triple in input file
      //     object = new URI(subject).toASCIIString
      //     if (object != subject) 
      //     write triple(subject, sameAs, object) to destination
    }
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}