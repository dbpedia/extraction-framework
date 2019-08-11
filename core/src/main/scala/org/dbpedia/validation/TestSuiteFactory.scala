package org.dbpedia.validation

import java.io.InputStreamReader
import java.net.URL

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

object TestSuiteFactory {

  val validatorReferencesToIndexMap: mutable.Map[ValidatorReference,Int] = mutable.Map()

  // TODO use InputStream
  def loadTestSuite(pathToTestCaseFile: String): TestSuite = {

    // TODO: Jena could not read https and ttl is not well formed
    //       val testCaseTTL = new URL("https://raw.githubusercontent.com/dbpedia/extraction-framework/master/new_release_based_ci_tests_draft.ttl")
    //       IOUtils.copy(testCaseTTL.openStream(), System.out)

    val testsRdfModel = ModelFactory.createDefaultModel()
    testsRdfModel.read(pathToTestCaseFile)

    // TODO: replace validatorReferencesToIndexMap By make TestSuite containing array of testCases
    //       ( testcase has idx. of trggr & val array )
    TestSuite(
      loadIriTriggers(testsRdfModel),
      loadIriValidators(testsRdfModel),
      validatorReferencesToIndexMap.toMap /*ensure immutability*/)
  }

  private def loadIriTriggers(m_tests: Model): Array[IriTrigger] = {

    val triggersQuery = QueryFactory.create(iriTriggerQueryStr())

    val triggersResultSet = QueryExecutionFactory.create(triggersQuery, m_tests).execSelect()

    val iriTriggers = ArrayBuffer[IriTrigger]()

    while (triggersResultSet.hasNext) {

      val triggerSolution = triggersResultSet.next()
      val triggerIri = triggerSolution.getResource("trigger").getURI

      val triggeredValidatorsQuery = QueryFactory.create(triggeredValidatorsQueryStr(triggerIri))
      val triggeredValidatorsResultSet = QueryExecutionFactory.create(triggeredValidatorsQuery,m_tests).execSelect()

      val validatorReferences = ArrayBuffer[ValidatorReference]()
      while (triggeredValidatorsResultSet.hasNext)
        validatorReferences.append(triggeredValidatorsResultSet.next().getResource("validator").getURI)

      iriTriggers.append(
        IriTrigger(
          triggerIri,
          triggerSolution.getLiteral("label").getLexicalForm,
          triggerSolution.getLiteral("comment").getLexicalForm,
          triggerSolution.getLiteral("patterns").getLexicalForm.split("\t"),
          validatorReferences.toArray
        )
      )
    }
    iriTriggers.toArray
  }

  def loadIriValidators(m_tests: Model): Array[IriValidator] = {

    val validatorsQuery = QueryFactory.create(iriValidatorQueryStr())
    val validatorsResultSet = QueryExecutionFactory.create(validatorsQuery, m_tests).execSelect()

    val iriValidators = ArrayBuffer[IriValidator]()
    var arrayIndexCnt = 0

    while (validatorsResultSet.hasNext) {

      val validatorSolution = validatorsResultSet.next()
      val validatorIri = validatorSolution.getResource("validator").getURI
      validatorReferencesToIndexMap.put(validatorIri,arrayIndexCnt)

      val patterns = ArrayBuffer[String]()

      if( validatorSolution.contains("doesNotContainCharacters") ) {
        val chars = validatorSolution.getLiteral("doesNotContainCharacters").getLexicalForm.split("\t")
        patterns.append(s"^[^${chars.mkString("")}]*$$")
      }

      val oneOfVocab = ArrayBuffer[String]()
      if( validatorSolution.contains("oneOfVocab") ) {
        oneOfVocab.appendAll(getVocab(validatorSolution.getResource("oneOfVocab").getURI))
      }

      if ( validatorSolution.contains("patternRegex") ) {
        val patternRegex = validatorSolution.getLiteral("patternRegex").getLexicalForm
        patterns.append(patternRegex)
      }

      iriValidators.append(
        IriValidator(
          validatorIri,
          validatorSolution.getLiteral("hasScheme").getLexicalForm,
          validatorSolution.getLiteral("hasQuery").getLexicalForm.toBoolean,
          validatorSolution.getLiteral("hasFragment").getLexicalForm.toBoolean,
          patterns.toArray,
          HashSet(oneOfVocab.toArray: _*)
        )
      )
      arrayIndexCnt += 1
    }
    iriValidators.toArray
  }

  def getVocab(urlStr: String): Array[String] = {

    val url = new URL(urlStr)
    val reader = new InputStreamReader(url.openStream, "UTF-8")
    val model =  ModelFactory.createDefaultModel()

    RDFDataMgr.read(model,reader,"urn:base",RDFLanguages.NTRIPLES)

    val query = QueryFactory.create(oneOfVocabQueryStr)
    val resultSet = QueryExecutionFactory.create(query,model).execSelect

    val properties = ArrayBuffer[String]()

    while( resultSet.hasNext ) {
      properties.append(resultSet.next().getResource("property").getURI)
    }

    properties.toArray
  }
}