package org.dbpedia.extraction.dump.extract

import java.net.Authenticator

import org.apache.spark.{SparkConf, SparkContext}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.mappings.{CompositeParseExtractor, Extractor}
import org.dbpedia.extraction.ontology.datatypes.EnumerationDatatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyEntity, RdfNamespace}
import org.dbpedia.extraction.util.{Language, ProxyAuthenticator}
import org.dbpedia.extraction.wikiparser.WikiPage

/**
 * Dump extraction script.
 */
object Extraction {
  
  val Started = "extraction-started"

  val Complete = "extraction-complete"

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")
    Authenticator.setDefault(new ProxyAuthenticator())

    // Load configuration
    val config = new Config(args.head)
    val configLoader = new ConfigLoader(config)

    val parallelProcesses = if(config.runJobsInParallel) config.parallelProcesses else 1

    // Create SparkConfig
    val sparkConf = new SparkConf().setAppName("Main Extraction").setMaster(s"local[$parallelProcesses]")
    sparkConf.set("spark.eventLog.enabled","false")

    // Setup Serialization with Kryo
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    // Create SparkContext
    val sparkContext = new SparkContext(sparkConf)
    sparkContext.setLogLevel("WARN")

    // Run extraction jobs
    configLoader.getExtractionJobs.foreach(_.run(sparkContext, config))
  }
}
