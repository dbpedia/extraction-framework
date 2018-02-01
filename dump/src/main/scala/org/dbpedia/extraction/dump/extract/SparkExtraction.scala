package org.dbpedia.extraction.dump.extract

import java.net.Authenticator

import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.util.ProxyAuthenticator

object SparkExtraction {

  val Started = "extraction-started"

  val Complete = "extraction-complete"

  val logger: Logger = LogManager.getRootLogger

  def main(args: Array[String]): Unit = {
    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: config file name")
    Authenticator.setDefault(new ProxyAuthenticator())

    logger.setLevel(Level.INFO)

    // Load configuration
    val config = new Config(args.head)
    val configLoader = new ConfigLoader(config)

    // Create SparkConfig
    val sparkConf = new SparkConf().setAppName("Main Extraction").setMaster("local[*]")

    // Setup Serialization with Kryo
    sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    // Create SparkContext
    val sparkContext = new SparkContext(sparkConf)
    sparkContext.setLogLevel("ERROR")

    // Run extraction jobs
    configLoader.getSparkExtractionJobs.foreach(_.run(sparkContext, config))
  }
}
