package org.dbpedia.extraction.dump

import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.spark.sql.SparkSession
import org.dbpedia.extraction.config.Config

object TestConfig {
  val date: String = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

  //Workaround to get resource files in Scala 2.11
  val classLoader: ClassLoader = getClass.getClassLoader
  val mappingsConfig = new Config(classLoader.getResource("extraction-configs/mappings.extraction.minidump.properties").getFile)
  val genericConfig = new Config(classLoader.getResource("extraction-configs/generic-spark.extraction.minidump.properties").getFile)
  val nifAbstractConfig = new Config(classLoader.getResource("extraction-configs/extraction.nif.abstracts.properties").getFile)
  val plainAbstractConfig = new Config(classLoader.getResource("extraction-configs/extraction.plain.abstracts.properties").getFile)
  val wikidataConfig = new Config(classLoader.getResource("extraction-configs/wikidata.extraction.properties").getFile)
  val minidumpDir = new File(classLoader.getResource("minidumps").getFile)

  val minidumpURL: URL = classLoader.getResource("mini-enwiki.xml.bz2")
  val ciTestFile: String = classLoader.getResource("ci-tests/dbpedia-specific-ci-tests.ttl").getFile
  val XSDCITestFile: String = classLoader.getResource("ci-tests/xsd_ci-tests.ttl").getFile
  val ciTestModel: Model = ModelFactory.createDefaultModel()

  /**
   * NEEDED for SHACL
   */
  val defaultTestGroup = "PRODUCTIVE"
  val dumpDirectory = new File(mappingsConfig.dumpDir, s"")
  //  val dumpDirectory =     new File(mappingsConfig.dumpDir, s"enwiki/$date/")
  val dbpedia_ontologyFile: String = classLoader.getResource("dbpedia.owl").getFile
  val custom_SHACL_testFolder: String = classLoader.getResource("shacl-tests").getFile
  /**
   * SPARK
   */
  val sparkSession: SparkSession = SparkSession.builder()
    .appName("Minidump Tests")
    .master("local[*]")
    .config("spark.ui.enabled", "false")
    .config("hadoop.home.dir", "./target/minidumptest/hadoop-tmp")
    .config("spark.local.dir", "./target/minidumptest/spark-tmp")
    .config("spark.locality.wait", "0")
    .getOrCreate()
  sparkSession.sparkContext.setLogLevel("WARN")
}
