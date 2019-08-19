package org.dbpedia.validation

import java.io.InputStreamReader
import java.net.URL

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.dbpedia.validation.TestCaseImpl._
import org.dbpedia.validation.TestSuiteImpl.TestSuite
import org.dbpedia.validation.TriggerImpl._

import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object TestSuiteFactory {

  val validatorRef_TestApproachSet: mutable.Map[ValidatorIRI,Array[Int]] = mutable.Map()

  var currentTestApproachID: TestApproachID = 0
  var currentTriggerID: TriggerID = 0
  var currentTestCaseID: TestCaseID = 0

  val delim = "\t"

  def loadTestSuite(testModelFilePath: String): TestSuite = {

    val testModel = ModelFactory.createDefaultModel()
    testModel.read(testModelFilePath)

    loadTestSuite(testModel)
  }

  def loadTestSuite(testModel: Model): TestSuite = {

    val testApproachCollection = loadTestApproachCollection(testModel)
    val triggerCollection = loadTriggerCollection(testModel)

    TestSuite(
      triggerCollection,
      testApproachCollection,
      currentTestApproachID,
      currentTriggerID,
      currentTestCaseID
    )
  }

  private def loadTriggerCollection( testModel: Model ): Array[Trigger] = {

    val triggerCollection = ArrayBuffer[IRITrigger]()

    /*
    generic IRI Trigger
     */
    val genericTestCases = ArrayBuffer[TestCase]()

    validatorRef_TestApproachSet("__GENERIC_IRI__").foreach(

      testApproachID => {
        genericTestCases.append( TestCase(currentTestCaseID,currentTriggerID,testApproachID))
        currentTestCaseID+=1
      }
    )

        triggerCollection.append(
          IRITrigger(
            currentTriggerID,
            Array[String](".*"),
            genericTestCases.toArray,
            "__GENERIC_IRI__",
            "generic iri trigger",
            "parse every"
          )
        )
        currentTriggerID+=1

    /*
    testModel based IRITrigger
     */
    val triggersQuery = QueryFactory.create(iriTriggerQueryStr())

    QueryExecutionFactory.create(triggersQuery, testModel).execSelect().foreach(

      triggersQuerySolution => {

        val testCases = ArrayBuffer[TestCase]()

        val triggerIRI = triggersQuerySolution.getResource("trigger").getURI
        val triggerPatterns = triggersQuerySolution.getLiteral("patterns").getLexicalForm.split(delim)
        //TODO iri label comment

        val triggeredValidatorsQuery = QueryFactory.create(triggeredValidatorsQueryStr(triggerIRI))

        QueryExecutionFactory.create(triggeredValidatorsQuery, testModel).execSelect().foreach(

          triggeredValidatorsQuerySolution => {

            val validatorNode = triggeredValidatorsQuerySolution.getResource("validator").asNode()

            if (validatorNode.isBlank) {

              validatorRef_TestApproachSet(validatorNode.getBlankNodeLabel).foreach(testApproachID => {

                testCases.append(TestCase(currentTestCaseID, currentTriggerID, testApproachID))
              })
            }
            else {

              validatorRef_TestApproachSet(validatorNode.getURI).foreach( testApproachID => {

                testCases.append(TestCase(currentTestCaseID, currentTriggerID, testApproachID))
              })
            }
            currentTestCaseID += 1
          }
        )

        triggerCollection.append( IRITrigger(currentTriggerID, triggerPatterns, testCases.toArray, triggerIRI, "", "") )
        currentTriggerID += 1
      }
    )

    triggerCollection.toArray
  }

  def loadTestApproachCollection(testModel: Model): Array[TestApproach]  = {

    val testApproachCollection = ArrayBuffer[TestApproach]()

    /*
    generic IRI testcases
     */
    testApproachCollection.append(
      GenericIRITestApproach(currentTestApproachID)
    )
    validatorRef_TestApproachSet.put("__GENERIC_IRI__",Array[Int](0))
    currentTestApproachID += 1

    /*
    model based test approaches
     */
    val validatorQuery = QueryFactory.create(iriValidatorQueryStr())

    QueryExecutionFactory.create(validatorQuery, testModel).execSelect().foreach(

      validatorQuerySolution => {

        val grouepdTestApproachIDs = ArrayBuffer[Int]()

        val validatorIRI: ValidatorIRI = {

          val validatorNode = validatorQuerySolution.get("validator").asNode()

          if( validatorNode.isBlank )
            validatorNode.getBlankNodeLabel
          else
            validatorNode.getURI
        }

        /*
        rdfs:comment
         */
        val comment = StringBuilder.newBuilder
        if( validatorQuerySolution.contains("comment") ) {
          comment.append(validatorQuerySolution.getLiteral("comment").getLexicalForm)
        }

        /*
        v:pattern
         */
        if( validatorQuerySolution.contains("patterns") ) {

          validatorQuerySolution.getLiteral("patterns").getLexicalForm.split(delim).foreach( patternString => {

            testApproachCollection.append(PatternTestApproach(currentTestApproachID, patternString))
            grouepdTestApproachIDs.append(currentTestApproachID)
            currentTestApproachID +=1
          })
        }

        /*
        v:oneOfVocab
         */
        if( validatorQuerySolution.contains("oneOfVocab") ) {

          validatorQuerySolution.getLiteral("oneOfVocab").getLexicalForm.split(delim).foreach( vocabUrl => {

            testApproachCollection.append(VocabTestApproach(currentTestApproachID, vocabUrl, getVocab(vocabUrl)))
            grouepdTestApproachIDs.append(currentTestApproachID)
            currentTestApproachID += 1
          })
        }

        /*
        v:doesNotContain
         */
        if (validatorQuerySolution.contains("doesNotContains")) {

          validatorQuerySolution.getLiteral("doesNotContains").getLexicalForm.split(delim).foreach( charSeq => {

            testApproachCollection.append(NotContainsTestApproach(currentTestApproachID, charSeq))
            grouepdTestApproachIDs.append(currentTestApproachID)
            currentTestApproachID += 1
          })
        }

        validatorRef_TestApproachSet.put(validatorIRI, grouepdTestApproachIDs.toArray)
      }
    )

    testApproachCollection.toArray
  }


  def getVocab(urlStr: String): HashSet[String] = {

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

    HashSet(properties.toArray: _*)
  }
}