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
    val master = config.sparkMaster
    val altLocalDir = config.sparkLocalDir

    val spark =
      if(altLocalDir != "") {
        logger.info(s"Set alternate spark-local-dir to: $altLocalDir")
        SparkSession.builder()
          .appName("MainExtraction")
          .master(master)
          .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
          .config("spark.local.dir", altLocalDir)
          .getOrCreate()
      } else {
        SparkSession.builder()
          .appName("MainExtraction")
          .master(master)
          .config("spark.locality.wait","0")
          .getOrCreate()
      }
    
    logger.info(s"Created Spark Session with master: $master")

    // Create SparkContext
    val sparkContext = spark.sparkContext
    sparkContext.setLogLevel("WARN")

    // Run extraction jobs
    configLoader.getSparkExtractionJobs.foreach(job => {
      try {
        job.run(spark, config)
      } catch {
        case ex: Throwable => ex.printStackTrace()
      }
    })
  }
}
