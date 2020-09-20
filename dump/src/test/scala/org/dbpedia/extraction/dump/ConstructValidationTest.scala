package org.dbpedia.extraction.dump

import java.io.{File, FileInputStream, FileOutputStream}

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.spark.sql.SQLContext
import org.dbpedia.extraction.dump.TestConfig.{XSDCITestFile, ciTestFile, ciTestModel, date, mappingsConfig, sparkSession}
import org.dbpedia.extraction.dump.tags.ConstructValidationTestTag
import org.dbpedia.validation.construct.report.ReportWriter
import org.dbpedia.validation.construct.report.formats.ReportFormat
import org.dbpedia.validation.construct.tests.TestSuiteFactory
import org.dbpedia.validation.construct.tests.suites.NTripleTestSuite
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}

@DoNotDiscover
class ConstructValidationTest extends FunSuite with BeforeAndAfterAll {

  override def beforeAll() {
    RDFDataMgr.read(ciTestModel, new FileInputStream(ciTestFile), RDFLanguages.TURTLE)
    // TODO move to TestConfig
    new File("./target/testreports/").mkdirs()
  }

  test("IRI Coverage Tests", ConstructValidationTestTag) {

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
}
