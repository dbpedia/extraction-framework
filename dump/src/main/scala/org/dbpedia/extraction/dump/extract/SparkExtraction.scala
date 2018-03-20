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
    val master = Config.universalConfig.sparkMaster
    val altLocalDir = Config.universalConfig.sparkLocalDir

    val spark = SparkSession.builder()
        .appName("MainExtraction")
        .master(master)
        .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .getOrCreate()

    logger.info(s"Created Spark Session with master: $master")

    if(altLocalDir != "") {
      spark.conf.set("spark.local.dir", altLocalDir)
      logger.info(s"Set alternate spark-local-dir to: $altLocalDir")
    }

    // Create SparkContext
    val sparkContext = spark.sparkContext
    sparkContext.setLogLevel("WARN")

    // Run extraction jobs
    configLoader.getSparkExtractionJobs.foreach(_.run(spark, config))
  }
}
