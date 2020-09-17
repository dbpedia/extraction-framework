package org.dbpedia.extraction.dump

import java.io.{File, FileInputStream, FileOutputStream}
import java.text.SimpleDateFormat
import java.util
import java.util.Calendar
import java.util.concurrent.ConcurrentLinkedQueue

import org.aksw.rdfunit.RDFUnit
import org.aksw.rdfunit.enums.TestCaseExecutionType
import org.aksw.rdfunit.io.reader.{RdfModelReader, RdfStreamReader}
import org.aksw.rdfunit.io.writer.RdfResultsWriterFactory
import org.aksw.rdfunit.model.interfaces.results.TestExecution
import org.aksw.rdfunit.model.interfaces.{TestCase, TestSuite}
import org.aksw.rdfunit.sources.{SchemaSource, SchemaSourceFactory, TestSourceBuilder}
import org.aksw.rdfunit.tests.generators.{ShaclTestGenerator, TestGeneratorFactory}
import org.aksw.rdfunit.validate.wrappers.RDFUnitStaticValidator
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.dump.extract.ConfigLoader
import org.dbpedia.extraction.util.MappingsDownloader.apiUrl
import org.dbpedia.extraction.util.{Language, OntologyDownloader, WikiDownloader}
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.validation.construct.report.ReportWriter
import org.dbpedia.validation.construct.report.formats.ReportFormat
import org.dbpedia.validation.construct.tests.TestSuiteFactory
//import org.dbpedia.validation.construct.tests.executors.DumpExecutor
import org.dbpedia.validation.construct.tests.suites.NTripleTestSuite
import org.scalatest.{BeforeAndAfterAll, FunSuite}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class MinidumpTests extends FunSuite with BeforeAndAfterAll {

  /**
    * in src/test/resources/
    */
  val date = new SimpleDateFormat("yyyyMMdd").format(Calendar.getInstance().getTime)

  //Workaround to get resource files in Scala 2.11
  val classLoader = getClass.getClassLoader
  val mappingsConfig = new Config(classLoader.getResource("mappings.extraction.minidump.properties").getFile)
  val genericConfig = new Config(classLoader.getResource("generic-spark.extraction.minidump.properties").getFile)
  val nifAbstractConfig = new Config(classLoader.getResource("extraction.nif.abstracts.properties").getFile)
  val wikidataConfig = new Config(classLoader.getResource("wikidata.extraction.properties").getFile)
  val minidumpDir = new File(classLoader.getResource("minidumps").getFile)

  val minidumpURL = classLoader.getResource("mini-enwiki.xml.bz2")
  val ciTestFile = classLoader.getResource("dbpedia-specific-ci-tests.ttl").getFile
  val XSDCITestFile = classLoader.getResource("xsd_ci-tests.ttl").getFile
  val ciTestModel: Model = ModelFactory.createDefaultModel()

  /**
    * NEEDED for SHACL
    */
  val dumpDirectory = new File(mappingsConfig.dumpDir, s"")
  //  val dumpDirectory =     new File(mappingsConfig.dumpDir, s"enwiki/$date/")
  val dbpedia_ontologyFile = classLoader.getResource("dbpedia.owl").getFile
  val custom_SHACL_testFile = classLoader.getResource("custom-shacl-tests.ttl").getFile

  /**
    * SPARK
    */
  val sparkSession: SparkSession = SparkSession.builder()
    .appName("Minidump Tests")
    .master("local[*]")
    .config("hadoop.home.dir", "./target/minidumptest/hadoop-tmp")
    .config("spark.local.dir", "./target/minidumptest/spark-tmp")
    .config("spark.locality.wait", "0")
    .getOrCreate()

  override def beforeAll() {
    //  def excludeBeforeAll() {

    /**
      * check ttl file for CI here
      */

    sparkSession.sparkContext.setLogLevel("WARN")

    println("Loading triggers and validators")

    RDFDataMgr.read(ciTestModel, new FileInputStream(ciTestFile), RDFLanguages.TURTLE)

    // copy dumps

    minidumpDir.listFiles().foreach(f => {
      val wikiMasque = f.getName + "wiki"
      val targetDir = new File(mappingsConfig.dumpDir, s"${wikiMasque}/$date/")
      // create directories
      targetDir.mkdirs()
      FileUtils.copyFile(
        new File(f + "/wiki.xml.bz2"),
        new File(targetDir, s"${wikiMasque}-$date-pages-articles-multistream.xml.bz2")
      )
    })

    /**
      * download ontology
      *
      * cd core;
      * mvn scala:run -Dlauncher="download-mappings";
      */
       println("Download ontology")
       val dumpFile = new File("../ontology.xml")
       val owlFile = new File("../ontology.owl")
       val version = "1.0"
       org.dbpedia.extraction.util.OntologyDownloader.download(dumpFile)
       val ontology = OntologyDownloader.load(dumpFile)
       org.dbpedia.extraction.util.OntologyDownloader.save(ontology, version, owlFile)

    /**
      * download mappings
      *
      * cd core;
      * mvn scala:run -Dlauncher="download-ontology";
      */
        println("Download mappings")
        val dir = new File("../mappings")
        // don't use mkdirs, that often masks mistakes.
        require(dir.isDirectory || dir.mkdir, "directory ["+dir+"] does not exist and cannot be created")
        Namespace.mappings.values.par.foreach { namespace =>
          val file = new File(dir, namespace.name(Language.Mappings).replace(' ','_')+".xml")
          val nanos = System.nanoTime
          println("downloading mappings from "+apiUrl+" to "+file)
          new WikiDownloader(apiUrl).download(file, namespace)
          println("downloaded mappings from "+apiUrl+" to "+file+" in "+((System.nanoTime - nanos) / 1000000000F)+" seconds")
        }

    println("Extracting Minidump")
    /**
      * mappings extraction
      * nifAbstract extraction
      */
    val jobsRunning = new ConcurrentLinkedQueue[Future[Unit]]()
    println("-- wikidata")
    extract(wikidataConfig, jobsRunning)
    println("-- mappings")
    extract(mappingsConfig, jobsRunning)
    println("-- nifAbstract")
    extract(nifAbstractConfig, jobsRunning)
    println("-- generic")
    extractSpark(genericConfig, jobsRunning)

    def extractSpark(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]) = {
      /**
        * generic extraction
        */
      println("-- generic")
      val configLoader = new ConfigLoader(config)

      // Run extraction jobs
      configLoader.getSparkExtractionJobs.foreach(job => {
        try {
          job.run(sparkSession, config)
        } catch {
          case ex: Throwable =>
            ex.printStackTrace()
        }
      })
      jobsRunning.clear()
    }

    def extract(config: Config, jobsRunning: ConcurrentLinkedQueue[Future[Unit]]) = {
      val configLoader = new ConfigLoader(config)
      val parallelProcesses = if (config.runJobsInParallel) config.parallelProcesses else 1

      //Execute the extraction jobs one by one
      for (job <- configLoader.getExtractionJobs) {
        while (jobsRunning.size() >= parallelProcesses) {
          Thread.sleep(1000)
        }

        val future = Future {
          job.run()
        }
        jobsRunning.add(future)
        future.onComplete {
          case Failure(f) => throw f
          case Success(_) => jobsRunning.remove(future)
        }
      }
    }

    while (jobsRunning.size() > 0) {
      Thread.sleep(1000)
    }

    jobsRunning.clear()

    // create test reports directory
    new File("./target/testreports/").mkdirs()

  }

  test("IRI Coverage Tests") {

    val SQLContext: SQLContext = sparkSession.sqlContext

    val testFiles = Array(ciTestFile, XSDCITestFile)

    val testModel = ModelFactory.createDefaultModel()
    testFiles.foreach(testFile => testModel.read(testFile))

    val testSuite = TestSuiteFactory.create(testModel, TestSuiteFactory.TestSuiteType.NTriples).asInstanceOf[NTripleTestSuite]

    val testScores = testSuite.test(s"${mappingsConfig.dumpDir.getAbsolutePath}/*/$date/*.ttl.bz2")(SQLContext)

    new File("target/testreports/").mkdirs()
    val htmlOS = new FileOutputStream(s"./target/testreports/minidump.html", false)
    ReportWriter.write("DIEF Minidump NTriple Test Cases", testScores(0), testSuite, ReportFormat.HTML, htmlOS)
    htmlOS.close()
    println("Wrote: " + s"./target/testreports/minidump.html")
  }

  test("Minidumps with RDFUnit/SHACL") {
    //      def excludeRDFUnitSHACL() {

    val (schema: SchemaSource, testSuite: TestSuite) = generateShaclTestSuite()
    val results =
      validateMinidumpWithTestSuite(schema, testSuite, TestCaseExecutionType.shaclTestCaseResult, "./target/testreports/shacl-tests.html")

    assert(results.getTestCaseResults.isEmpty)
  }


  test("Minidumps with RDFUnit/Ontology") {
    //      def excludeRDFUnitOnto() {

    val (schema: SchemaSource, testSuite: TestSuite) = generateOntologyTestSuite
    val results =
      validateMinidumpWithTestSuite(schema, testSuite, TestCaseExecutionType.aggregatedTestCaseResult, "./target/testreports/onto-tests.html")

    // TODO assert
  }


  private def generateShaclTestSuite() = {
    val custom_SHACL_tests: Model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(custom_SHACL_tests, new FileInputStream(custom_SHACL_testFile), RDFLanguages.TURTLE)

    assert(custom_SHACL_tests.size() > 0, "size not 0")
    val schema = SchemaSourceFactory.createSchemaSourceSimple("http://dbpedia.org/shacl", new RdfModelReader(custom_SHACL_tests))

    val rdfUnit = RDFUnit.createWithOwlAndShacl
    rdfUnit.init

    val shaclTestGenerator = new ShaclTestGenerator()
    val shaclTests: util.Collection[TestCase] = shaclTestGenerator.generate(schema)
    val testSuite = new TestSuite(shaclTests)
    (schema, testSuite)
  }


  private def generateOntologyTestSuite: (SchemaSource, TestSuite) = {
    val dbpedia_ont: Model = ModelFactory.createDefaultModel()
    RDFDataMgr.read(dbpedia_ont, new FileInputStream(dbpedia_ontologyFile), RDFLanguages.RDFXML)
    assert(dbpedia_ont.size() > 0, "size not 0")

    val schema = SchemaSourceFactory.createSchemaSourceSimple("http://dbpedia.org/ontology", new RdfModelReader(dbpedia_ont))

    val rdfUnit = RDFUnit.createWithOwlAndShacl
    rdfUnit.init

    val testGenerator = TestGeneratorFactory.createAllNoCache(rdfUnit.getAutoGenerators, "./")
    val tests: java.util.Collection[TestCase] = testGenerator.generate(schema)
    val testSuite = new TestSuite(tests)
    (schema, testSuite)
  }

  private def validateMinidumpWithTestSuite(schema: SchemaSource,
                                            testSuite: TestSuite,
                                            executionType: TestCaseExecutionType,
                                            sinkFileName: String): TestExecution = {

    val filesToBeValidated = recursiveListFiles(dumpDirectory).filter(_.isFile)
      .filter(_.toString.endsWith(".ttl.bz2"))
      .toList

    // val filesToBeValidated = dumpDirectory.listFiles.filter(_.isFile).filter(_.toString.endsWith(".ttl.bz2")).toList
    //println("FILES, FILES, FILES\n"+filesToBeValidated)

    //org.apache.jena.riot.system.IRIResolver.
    val singleModel: Model = ModelFactory.createDefaultModel()
    for (file <- filesToBeValidated) {
      singleModel.add(new RdfStreamReader(new BZip2CompressorInputStream(new FileInputStream(file.getAbsolutePath)), "TURTLE").read())
      println("RDFUnit loaded: " + file)
    }

    val testSource = new TestSourceBuilder()
      .setPrefixUri("minidump", "http://dbpedia.org/minidump")
      .setInMemReader(new RdfModelReader(singleModel))
      .setReferenceSchemata(schema)
      .build()

    val results = RDFUnitStaticValidator.validate(executionType, testSource, testSuite)

    RdfResultsWriterFactory.createHtmlWriter(
      results, new FileOutputStream(sinkFileName, false)
    ).write(ModelFactory.createDefaultModel())

    results
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  override def afterAll() {
    println("Cleaning Extraction")
  }
}
