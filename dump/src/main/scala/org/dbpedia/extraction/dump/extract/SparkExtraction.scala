package org.dbpedia.extraction.dump.extract

import java.net.Authenticator

import org.apache.log4j.{Level, LogManager, Logger}
import org.apache.spark.sql.SparkSession
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

    val spark = SparkSession.builder()
        .appName("MainExtraction")
        .master("local[*]")
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .getOrCreate()

    // Create SparkContext
    val sparkContext = spark.sparkContext
    sparkContext.setLogLevel("WARN")

    // Run extraction jobs
    configLoader.getSparkExtractionJobs.foreach(_.run(spark, config))
  }
}
