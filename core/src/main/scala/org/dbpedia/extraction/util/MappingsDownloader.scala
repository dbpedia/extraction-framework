package org.dbpedia.extraction.util

import java.io.File

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.wikiparser.WikiTitle.Namespace

/**
 * Download mapping pages for all namespaces from http://mappings.dbpedia.org/ and transform 
 * them into the format of the dump files (because XMLSource understands that format). 
 */
object MappingsDownloader {
  
  val apiUrl = "http://mappings.dbpedia.org/api.php"
    
  def main(args: Array[String]) : Unit =
  {
    require(args != null && args.length == 1, "expected one argument for mappings target directory")
    val dir = new File(args(0))
    
    // don't use mkdirs, that often masks mistakes. 
    require(dir.isDirectory || dir.mkdir, "directory ["+dir+"] does not exist and cannot be created")
    
    for (namespace <- Namespace.values if (namespace.toString.startsWith("Mapping"))) {
      val file = new File(dir, namespace.toString+".xml")
      val nanos = System.nanoTime
      println("downloading mappings from "+apiUrl+" to "+file)
      new WikiDownloader(apiUrl).download(file, namespace)
      println("downloaded mappings from "+apiUrl+" to "+file+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
    }
  }
}
