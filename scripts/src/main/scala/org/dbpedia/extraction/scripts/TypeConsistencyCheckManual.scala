package org.dbpedia.extraction.scripts

import java.io.{File, Writer}
import java.net.URL

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.scripts.TypeConsistencyCheck.{datasets, writer}
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks._

/**
  * see TypeConsistencyCheck, this is just a wrapper to invoke with custom files
  *
  * invoke with $ {type dataset} {mapping-uncleaned-dataset} {language}
  * e.g.
  * $ instance-types-2018.12.01_enwiki.ttl.bz2 mappingbased-objects-uncleaned-2018.12.01_enwiki.ttl.bz2 de
  */
object TypeConsistencyCheckManual {

  private val formats = Config.universalConfig.formats
    .filterKeys(_ == "ttl.bz2")

  def main(args: Array[String]) {


    require(args != null && args.length ==3, "Three arguments required, type dataset, uncleaned dataset and the dataset language")
    require(args(0).nonEmpty, "missing required argument: instance type dataset")
    require(args(1).nonEmpty, "missing required argument: mapping objects uncleaned dataser")
    require(args(2).nonEmpty, "missing required argument: dataset language (e.g. en, de, nl)")

    val typeDatasetFile: File = new File(args(0))
    val mappedTripleDatasetFile: File = new File(args(1))
    val language: Language = Language(args(2))

    lazy val ontology: Ontology = {
      val ontologyFile = new File("../ontology.xml")
      val ontologySource = if (ontologyFile != null && ontologyFile.isFile) {
        XMLSource.fromFile(ontologyFile, Language.Mappings)
      }
      else {
        val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
        val url = new URL(Language.Mappings.apiUri)
        WikiSource.fromNamespaces(namespaces, url, Language.Mappings)
      }

      new OntologyReader().read(ontologySource)
    }

    val destination = createDestination(mappedTripleDatasetFile.getParentFile.getAbsolutePath, formats)

    TypeConsistencyCheck.checkTypeConsistency(ontology, typeDatasetFile, mappedTripleDatasetFile, destination, language)
  }


  private def createDestination(baseFolder: String, formats: scala.collection.Map[String, Formatter]) : Destination = {
    val destination = new ArrayBuffer[Destination]()
    for ((suffix, format) <- formats) {
      val datasetDestinations = new mutable.HashMap[Dataset, Destination]()
      for (dataset <- datasets) {
        val file = new File(baseFolder + "/" + dataset.encoded.replace('_', '-') + '.' + suffix)
        datasetDestinations(dataset) = new WriterDestination(writer(file), format)
      }

      destination += new DatasetDestination(datasetDestinations)
    }
    new CompositeDestination(destination: _*)
  }

  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(file)
  }


}
