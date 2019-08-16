package org.dbpedia.extraction.dump

import java.io.{File, FileInputStream}
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.ConcurrentLinkedQueue

import org.apache.commons.io.FileUtils
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
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
  val date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

  //Workaround to get resource files in Scala 2.11
  val classLoader =   getClass.getClassLoader
  val mappingsConfig = new Config(classLoader.getResource("mappings.extraction.minidump.properties").getFile)
  val genericConfig = new Config(classLoader.getResource("generic-spark.extraction.minidump.properties").getFile)
  val minidumpDir = new File(classLoader.getResource("minidumps").getFile)



  val minidumpURL = classLoader.getResource("mini-enwiki.xml.bz2")
  val ciTestFile = classLoader.getResource("dbpedia-specific-ci-tests.ttl").getFile
  val ciTestModel: Model = ModelFactory.createDefaultModel()


  /**
    * NEEDED for SHACL
    */
  val dumpDirectory =     new File(mappingsConfig.dumpDir, s"enwiki/$date/")
  val dbpedia_ontologyFile = classLoader.getResource("dbpedia.owl").getFile
  val custom_SHACL_testFile = classLoader.getResource("custom-shacl-tests.ttl").getFile


  override def beforeAll() {

    /**
      * check ttl file for CI here
      */

    println("Loading triggers and validators")

    RDFDataMgr.read(ciTestModel, new FileInputStream(ciTestFile),RDFLanguages.TURTLE)



    println("Extracting Minidump")

    // copy dumps

    minidumpDir.listFiles().foreach(f=>{
      val wikiMasque =  f.getName+"wiki"
      val targetDir =     new File(mappingsConfig.dumpDir, s"${wikiMasque}/$date/")
      // create directories
      targetDir.mkdirs()
      FileUtils.copyFile(
        new File(f+"/wiki.xml.bz2"),
        new File(targetDir, s"${wikiMasque}-$date-pages-articles-multistream.xml.bz2")
      )
  })



    /**
      * mappings extraction
       */
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()

    extract(mappingsConfig,jobsRunning)
    extract(genericConfig,jobsRunning)


    def extract (config: Config, jobsRunning:ConcurrentLinkedQueue[Future[Unit]]) = {
      val configLoader = new ConfigLoader(config)
      val parallelProcesses = if(config.runJobsInParallel) config.parallelProcesses else 1

      //Execute the extraction jobs one by one
      for (job <- configLoader.getExtractionJobs) {
        while(jobsRunning.size() >= parallelProcesses){
          Thread.sleep(1000)
        }

        val future = Future{job.run()}
        jobsRunning.add(future)
        future.onComplete {
          case Failure(f) => throw f
          case Success(_) => jobsRunning.remove(future)
        }
      }


    }
    while(jobsRunning.size() > 0) {
      Thread.sleep(1000)
    }



  }




  test("IRI Coverage Tests") {


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
      pathToFlatTurtleFile =  s"${mappingsConfig.dumpDir.getAbsolutePath}/*/$date/*.ttl.bz2",
      pathToTestCases = ciTestFile

    )(sqlContext)

    println(eval.toString)

    assert(eval.subjects.proof == 1, "subjects not valid")
    assert(eval.predicates.proof == 1, "predicates not valid")
    assert(eval.objects.proof == 1, "objects not valid")
  }

  test("RDFUnit SHACL"){
    val filesToBeValidated = dumpDirectory.listFiles.filter(_.isFile).filter(_.toString.endsWith(".ttl.bz2")).toList
    println("FILES, FILES, FILES\n"+filesToBeValidated)

    val dbpedia_ont: Model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(dbpedia_ont, new FileInputStream(dbpedia_ontologyFile),RDFLanguages.RDFXML)

    val custom_SHACL_tests: Model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(custom_SHACL_tests, new FileInputStream(custom_SHACL_testFile),RDFLanguages.TURTLE)

    assert(dbpedia_ont.size()>0,"size not 0")
    assert(custom_SHACL_tests.size()>0, "size not 0")


  }

  override def afterAll() {
    println("Cleaning Extraction")
  }
}
