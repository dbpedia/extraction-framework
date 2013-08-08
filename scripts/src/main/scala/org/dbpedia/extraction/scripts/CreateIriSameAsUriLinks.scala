package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.ConfigUtils.{loadConfig,parseLanguages,getString,getValue,getStrings}
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.destinations.{Quad,Destination,CompositeDestination,WriterDestination}
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.util.Finder
import java.net.URI
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.util.SimpleWorkers
import org.dbpedia.extraction.util.Language

object CreateIriSameAsUriLinks {
  
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = loadConfig(args(0), "UTF-8")
    
    val baseDir = getValue(config, "base-dir", true)(new File(_))
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val input = getString(config, "input", true)
    
    val output = getString(config, "output", true)
    
    val languages = parseLanguages(baseDir, getStrings(config, "languages", ',', true))
    
    val formats = parseFormats(config, "uri-policy", "format")

    val sameAs = RdfNamespace.OWL.append("sameAs")
    
    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val workers = SimpleWorkers(1.5, 1.0) { language: Language =>
      
      val finder = new Finder[File](baseDir, language, "wiki")
      val date = finder.dates().last
      
      val inputFile = finder.file(date, input)
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix)
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      val destination = new CompositeDestination(formatDestinations.toSeq: _*)
      
      QuadMapper.mapQuads(language.wikiCode, inputFile, destination, true) { quad =>
        val iri = quad.subject
        val uri = new URI(iri).toASCIIString
        if (uri == iri) List.empty
        else List(new Quad(null, null, iri, sameAs, uri, null, null: String))
      }
    }
    
    workers.start()
    
    for (language <- languages) workers.process(language)
    
    workers.stop()

  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
    
}