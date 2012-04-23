package org.dbpedia.extraction.util

import java.io.File

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.wikiparser.Namespace

/**
 * Download ontology classes and properties from http://mappings.dbpedia.org/ and transform them
 * into the format of the dump files (because XMLSource understands that format). 
 */
object OntologyDownloader {
  
  val apiUrl = "http://mappings.dbpedia.org/api.php"
    
  def main(args: Array[String]) : Unit =
  {
    require(args != null && args.length == 1, "expected one argument for ontology target file")
    val file = new File(args(0))
    download(file)
    // load it to test it
    load(file)
  }
  
  def download(file : File) : Unit =
  {
    val nanos = System.nanoTime
    println("downloading ontology from "+apiUrl+" to "+file)
    new WikiDownloader(apiUrl).download(file, Namespace.OntologyClass, Namespace.OntologyProperty)
    println("downloaded ontology from "+apiUrl+" to "+file+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
  }
    
  def load(file : File) : Unit =
  {
    val nanos = System.nanoTime
    println("loading ontology from "+file)
    new OntologyReader().read(XMLSource.fromFile(file, language = Language.Default))
    println("loaded ontology from "+file+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
  }
  
}
