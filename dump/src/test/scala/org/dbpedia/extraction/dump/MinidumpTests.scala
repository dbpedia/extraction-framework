package org.dbpedia.extraction.dump

import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.commons.io.FileUtils
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.dump.extract.{ConfigLoader, Extraction}
import org.dbpedia.validation.ValidationExecutor
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class MinidumpTests extends FunSuite with BeforeAndAfterAll {

  /**
    * in src/test/resources/
    */
  val extractionConfig = "extraction.minidump.properties"
  val pageArticlesMinidump = "mini-enwiki.xml.bz2"
  val wikiMasque= "enwiki"

  var dumpPath = ""

  override def beforeAll() {

    println("Extracting Minidump")

    val date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

    //Workaround to get resource files in Scala 2.11
    val classLoader = getClass.getClassLoader

    val configURL = classLoader.getResource(extractionConfig)
    val config = new Config(configURL.getFile)

    val minidumpwikiDirectory = new File(config.dumpDir, s"$wikiMasque/$date/")
    dumpPath = minidumpwikiDirectory.getAbsolutePath
    minidumpwikiDirectory.mkdirs()

    val minidumpURL = classLoader.getResource(pageArticlesMinidump)

    FileUtils.copyFile(
      new File(minidumpURL.getFile),
      new File(minidumpwikiDirectory, s"$wikiMasque-$date-pages-articles-multistream.xml.bz2")
    )

    val configLoader = new ConfigLoader(config)

    val parallelProcesses = if(config.runJobsInParallel) config.parallelProcesses else 1
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    //Execute the extraction jobs one by one
    for (job <- configLoader.getExtractionJobs) {
      while(jobsRunning.size() >= parallelProcesses)
        Thread.sleep(1000)

      val future = Future{job.run()}
      jobsRunning.add(future)
      future.onComplete {
        case Failure(f) => throw f
        case Success(_) => jobsRunning.remove(future)
      }
    }

    while(jobsRunning.size() > 0)
      Thread.sleep(1000)
  }

  test("IRI Coverage Tests") {

    val classLoader = getClass.getClassLoader
    val pathToTestModelFilePath = classLoader.getResource("dbpedia-specific-ci-tests.ttl")

    val hadoopHomeDir = new File("./.haoop/")
    hadoopHomeDir.mkdirs()
    System.setProperty("hadoop.home.dir", hadoopHomeDir.getAbsolutePath)

    val sparkSession = SparkSession.builder()
      .config("hadoop.home.dir", "./.hadoop")
      .config("spark.local.dir", "./.spark")
      .appName("Test Iris").master("local[*]").getOrCreate()
    sparkSession.sparkContext.setLogLevel("WARN")

    val sqlContext: SQLContext = sparkSession.sqlContext

    val eval = ValidationExecutor.testIris(
      pathToFlatTurtleFile =  s"$dumpPath/*.ttl.bz2",
      pathToTestCases = pathToTestModelFilePath.getFile

    )(sqlContext)

    println(eval.toString)

    assert(eval.subjects.proof == 1, "subjects not valid")
    assert(eval.predicates.proof == 1, "predicates not valid")
    assert(eval.objects.proof == 1, "objects not valid")
  }

  override def afterAll() {
    println("Cleaning Extraction")
  }
}
