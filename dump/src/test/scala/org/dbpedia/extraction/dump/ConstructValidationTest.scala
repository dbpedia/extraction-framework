package org.dbpedia.extraction.dump

import org.apache.jena.graph
import org.apache.jena.rdf.model
import org.apache.jena.rdf.model.impl.StatementImpl
import org.apache.jena.rdf.model.StmtIterator

import java.io.{File, FileInputStream, FileOutputStream}
import org.apache.jena.rdf.model.{Alt, Bag, Literal, Model, ModelFactory, Property, RDFNode, RSIterator, ReifiedStatement, Resource, ResourceF, Statement, StmtIterator}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.apache.spark.sql.SQLContext
import org.dbpedia.extraction.dump.TestConfig.{XSDCITestFile, ciTestFile, ciTestModel, classLoader, date, mappingsConfig, sparkSession}
import org.dbpedia.extraction.dump.tags.ConstructValidationTestTag
import org.dbpedia.validation.construct.report.ReportWriter
import org.dbpedia.validation.construct.report.formats.ReportFormat
import org.dbpedia.validation.construct.tests.TestSuiteFactory
import org.dbpedia.validation.construct.tests.suites.NTripleTestSuite
import org.scalatest.{BeforeAndAfterAll, DoNotDiscover, FunSuite}
import org.apache.jena.rdf.model.RDFNode

import java.util.function.Consumer

@DoNotDiscover
class ConstructValidationTest extends FunSuite with BeforeAndAfterAll {

  override def beforeAll() {
    RDFDataMgr.read(ciTestModel, new FileInputStream(ciTestFile), RDFLanguages.TURTLE)
    // TODO move to TestConfig
    new File("./target/testreports/").mkdirs()
  }

  test("IRI Coverage Tests. Productive group tests", ConstructValidationTestTag) {
    val SQLContext: SQLContext = sparkSession.sqlContext
    val testFiles = Array(ciTestFile, XSDCITestFile)
    val testModel = ModelFactory.createDefaultModel()
    testFiles.foreach(testFile => testModel.read(testFile))
    val groupKeys = Utils.loadTestGroupsKeys(Utils.getGroup("cvTestGroup"), "cv-test-groups.csv", "no")
    val selectValues = groupKeys.map(x => s"<https://github.com/dbpedia/extraction-framework$x> ").toSet
      .mkString("\n")

    val iterator = testModel.listStatements()
    val testGeneratorURI = "http://dev.vocab.org/TestGenerator"

    while (iterator.hasNext) {
      val statement = iterator.nextStatement
      val rdfSubject = statement.getSubject.asResource()
      val rdfObject = statement.getObject
      if (rdfSubject.isURIResource && selectValues.contains(rdfSubject.getURI) && rdfObject.toString.equals(testGeneratorURI)) {
        iterator.remove()
      }
    }

    val testSuite = TestSuiteFactory.create(testModel, TestSuiteFactory.TestSuiteType.NTriples).asInstanceOf[NTripleTestSuite]

    val testScores = testSuite.test(s"${mappingsConfig.dumpDir.getAbsolutePath}/*/$date/*.ttl.bz2")(SQLContext)
    new File("target/testreports/").mkdirs()
    val htmlOS = new FileOutputStream(s"./target/testreports/minidump.html", false)
    ReportWriter.write("DIEF Minidump NTriple Test Cases", testScores(0), testSuite, ReportFormat.HTML, htmlOS)
    htmlOS.close()
    println("Wrote: " + s"./target/testreports/minidump.html")
  }
}
