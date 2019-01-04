package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, WriterDestination}
import org.dbpedia.extraction.util.IOUtils.writer
import org.dbpedia.extraction.util.{Finder, Language, SimpleWorkers}
import java.net.URI

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.ontology.RdfNamespace

object CreateIriSameAsUriLinks {
  
  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = new Config(args(0))
    
    val baseDir = config.dumpDir
    if (! baseDir.exists) throw error("dir "+baseDir+" does not exist")
    
    val input = config.inputDatasets.head
    
    val output = config.outputDataset.head
    
    val languages = config.languages

    val policies = config.policies
    val formats = config.formats

    val sameAs = RdfNamespace.OWL.append("sameAs")
    
    // We really want to saturate CPUs and disk, so we use 50% more workers than CPUs
    val workers = SimpleWorkers(1.5, 1.0) { language: Language =>
      
      val finder = new Finder[File](baseDir, language, "wiki")
      val date = finder.dates().last
      
      val inputFile = finder.file(date, input).get
      
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = finder.file(date, output+'.'+suffix).get
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      val destination = new CompositeDestination(formatDestinations: _*)

      new QuadMapper().mapQuads(language, inputFile, destination, required = true) { quad =>
        val iri = quad.subject
        val uri = URI.create(iri).toASCIIString //in this case we actually want to use an URI not an IRI
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