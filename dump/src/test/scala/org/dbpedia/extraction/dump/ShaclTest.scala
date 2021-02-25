package org.dbpedia.extraction.dump

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.Properties

import org.aksw.rdfunit.RDFUnit
import org.aksw.rdfunit.enums.TestCaseExecutionType
import org.aksw.rdfunit.io.format.SerialiazationFormatFactory
import org.aksw.rdfunit.io.reader.{RdfModelReader, RdfStreamReader}
import org.aksw.rdfunit.io.writer.RdfResultsWriterFactory
import org.aksw.rdfunit.model.impl.results.DatasetOverviewResults
import org.aksw.rdfunit.model.interfaces.results.TestExecution
import org.aksw.rdfunit.model.interfaces.{TestCase, TestSuite}
import org.aksw.rdfunit.sources.{SchemaSource, SchemaSourceFactory, TestSourceBuilder}
import org.aksw.rdfunit.tests.generators.{ShaclTestGenerator, TestGeneratorFactory}
import org.aksw.rdfunit.validate.wrappers.RDFUnitStaticValidator
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.dbpedia.extraction.dump.TestConfig.{classLoader, dbpedia_ontologyFile, dumpDirectory}
import org.dbpedia.extraction.dump.tags.ShaclTestTag
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}

@DoNotDiscover
class ShaclTest extends FunSuite with BeforeAndAfterAll {

  override def beforeAll(): Unit = {
    // TODO move to TestConfig
    new File("./target/testreports/").mkdirs()
  }

  def getGroup: String = {

    // TODO read group name from 1. best way: pom.xml or 2. from command line -D.testGroup=ALL
    val resourceInputStream = Option(getClass.getClassLoader.getResourceAsStream("properties-from-pom.properties"))
    val properties = new Properties()
    resourceInputStream match {
      case Some(inputStream) => properties.load(inputStream)
      case None => return TestConfig.defaultTestGroup
    }
    val groupOption = Option(properties.getProperty("testGroup"))
    groupOption match {
      case Some(group) => group
      case None => TestConfig.defaultTestGroup
    }

  }

  test("RDFUnit with SHACL", ShaclTestTag) {
    val (schema: SchemaSource, testSuite: TestSuite) = generateShaclTestSuiteFromMultipleFiles(getGroup)
    val results =
      validateMinidumpWithTestSuite(schema, testSuite, TestCaseExecutionType.aggregatedTestCaseResult, "./target/testreports/shacl-tests.html")

    assert(results.getDatasetOverviewResults.getErrorTests == 0)
  }


//  test("RDFUnit with ontology", ShaclTestTag) {
//    val (schema: SchemaSource, testSuite: TestSuite) = generateOntologyTestSuite
//    val results =
//      validateMinidumpWithTestSuite(schema, testSuite, TestCaseExecutionType.aggregatedTestCaseResult, "./target/testreports/onto-tests.html")
//
//    // TODO assert
//  }

  def generateOntologyTestSuite: (SchemaSource, TestSuite) = {
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



  def validateMinidumpWithTestSuite(schema: SchemaSource,
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

    val mod = ModelFactory.createDefaultModel()
//    RdfResultsWriterFactory.createWriterFromFormat(new FileOutputStream(sinkFileName.replace("html","ttl"), false),SerialiazationFormatFactory.createTriG(),results).write(ModelFactory.createDefaultModel())
    RdfResultsWriterFactory.createHtmlWriter(
      results, new FileOutputStream(sinkFileName, false)
    ).write(mod)

    mod.write(System.out,"TURTLE")

    results
  }

  def generateShaclTestSuiteFromMultipleFiles(testGroup: String = TestConfig.defaultTestGroup): (SchemaSource, TestSuite) = {
    val custom_SHACL_tests: Model = ModelFactory.createDefaultModel()
    val filesToBeValidated = recursiveListFiles(new File(TestConfig.custom_SHACL_testFolder)).filter(_.isFile)
      .filter(_.toString.endsWith(".ttl"))
      .toList

    for (file <- filesToBeValidated) {
      RDFDataMgr.read(custom_SHACL_tests, new FileInputStream(file), RDFLanguages.TURTLE)
    }
    assert(custom_SHACL_tests.size() > 0, "size not 0")

    val groupKeys = loadTestGroupsKeys(testGroup,"testGroups.csv")
    assert(groupKeys.nonEmpty)
    val selectValues = groupKeys.map(x => s"<https://github.com/dbpedia/extraction-framework$x> ")
      .mkString("\n")

    val queryString =
      s"""PREFIX sh: <http://www.w3.org/ns/shacl#>
         |CONSTRUCT {
         | ?s ?p ?o .
         | ?s2 ?p2 ?o2 .
         |}
         |WHERE {
         | VALUES ?s {
         |   ${selectValues}
         | }
         | ?s ?p ?o .
         | ?s (!<>)* ?s2 . ?s2 ?p2 ?o2 .
         |}
         |""".stripMargin

    val selected_SHACL_tests = QueryExecutionFactory.create(queryString,custom_SHACL_tests).execConstruct()

    selected_SHACL_tests.write(System.out,"TURTLE")

    val schema = SchemaSourceFactory.createSchemaSourceSimple("http://dbpedia.org/shacl", new RdfModelReader(selected_SHACL_tests))

    val rdfUnit = RDFUnit.createWithOwlAndShacl
    rdfUnit.init

    val shaclTestGenerator = new ShaclTestGenerator()
    val shaclTests: java.util.Collection[TestCase] = shaclTestGenerator.generate(schema)
    val testSuite = new TestSuite(shaclTests)
    (schema, testSuite)
  }

  def loadTestGroupsKeys(group: String, path: String): Array[String] = {
    val flag = "yes"
    val filePath = classLoader.getResource(path).getFile
    val file = scala.io.Source.fromFile(filePath)

    val table: Array[Array[String]] = file.getLines().map(_.split(",")).toArray
    val columnsNames: Array[String] = table.head

    if (!columnsNames.contains(group)) {
      Array[String]()
    }
    else {
      val indexOfGroup = columnsNames.indexOf(group)
      val groupsKeys: Array[String] = table.tail.flatMap(row =>
        if (row(indexOfGroup) == flag) Array[String](row(0))
        else Array[String]())

      groupsKeys
    }
  }

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }
}
