package org.dbpedia.extraction.util

import java.io.{File,FileOutputStream,OutputStreamWriter}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.io.{OntologyReader,OntologyOWLWriter}
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.wikiparser.Namespace

/**
 * Download ontology classes and properties from http://mappings.dbpedia.org/ and transform them
 * into the format of the dump files (because XMLSource understands that format). Also save the
 * result as OWL.
 */
object OntologyDownloader {
  
  val apiUrl = Language.Mappings.apiUri
    
  def main(args: Array[String]) : Unit =
  {
    require(args != null && args.length == 2, "expected two arguments: target file for wikitext XML dump, target file for OWL format.")
    val dumpFile = new File(args(0))
    val owlFile = new File(args(1))
    download(dumpFile)
    val ontology = load(dumpFile)
    save(ontology, owlFile)
  }
  
  def download(dumpFile: File): Unit =
  {
    val nanos = System.nanoTime
    println("downloading ontology from "+apiUrl+" to "+dumpFile)
    new WikiDownloader(apiUrl).download(dumpFile, Namespace.OntologyClass, Namespace.OntologyProperty)
    println("downloaded ontology from "+apiUrl+" to "+dumpFile+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
  }
    
  def load(dumpFile: File): Ontology =
  {
    val nanos = System.nanoTime
    println("loading ontology from "+dumpFile)
    val ontology = new OntologyReader().read(XMLSource.fromFile(dumpFile, Language.Mappings))
    println("loaded ontology from "+dumpFile+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
    ontology
  }
  
  def save(ontology: Ontology, owlFile: File): Unit =
  {
    val nanos = System.nanoTime
    println("saving ontology to "+owlFile)
    val xml = new OntologyOWLWriter().write(ontology)
    val writer = new OutputStreamWriter(new FileOutputStream(owlFile), "UTF-8")
    try writer.write(xml.toString)
    finally writer.close()
    println("saved ontology to "+owlFile+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
  }
  
}
