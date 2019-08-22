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
import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

object TestSuiteFactory {

  val testModel: Model = ModelFactory.createDefaultModel()

  val validatorRef_TestApproachSet: mutable.Map[ValidatorIRI,Array[Int]] = mutable.Map()

  var currentTestApproachID: TestApproachID = 0
  var currentTriggerID: TriggerID = 0
  var currentTestCaseID: TestCaseID = 0

  val delim = "\t"

  def loadTestSuite(testModelFilePaths: Array[String]): TestSuite = {

    currentTestApproachID = 0
    currentTriggerID = 0
    currentTestCaseID = 0

    testModelFilePaths.foreach(

      testModelFilePath => {

        testModel.read(testModelFilePath)
      }
    )

    loadTestSuite(testModel)
  }


  def loadTestSuite(testModelFilePath: String): TestSuite = {

    currentTestApproachID = 0
    currentTriggerID = 0
    currentTestCaseID = 0

    testModel.read(testModelFilePath)

    loadTestSuite(testModel)
  }

  def loadTestSuite(testModel: Model): TestSuite = {

    currentTestApproachID = 0
    currentTriggerID = 0
    currentTestCaseID = 0

    val testApproachCollection = loadTestApproachCollection(testModel)

    val triggerCollection = loadTriggerCollection(testModel, loadTestGenerator(testModel))

    TestSuite(
      triggerCollection,
      testApproachCollection,
      currentTestApproachID,
      currentTriggerID,
      currentTestCaseID
    )
  }

  private def loadTestGenerator( testModel: Model ): HashMap[TriggerIRI,Array[ValidatorIRI]] = {

    val testGeneratorQuery = QueryFactory.create(testGeneratorQueryStr)

    HashMap(

        QueryExecutionFactory.create(testGeneratorQuery,testModel).execSelect().map(

        generatorResult => {

          val triggerIRI = {

            val triggerNode = generatorResult.getResource("trigger").asNode()

            if( triggerNode.isBlank ) triggerNode.getBlankNodeLabel
            else triggerNode.getURI

          }
          val validatorIRI = {

            val validatorNode = generatorResult.getResource("validator").asNode()

            if( validatorNode.isBlank ) validatorNode.getBlankNodeLabel
            else validatorNode.getURI
          }

          triggerIRI -> validatorIRI
        }
      ).toArray.groupBy(_._1).map( entry => entry._1 -> entry._2.map(_._2) ).toArray : _*
    )
  }

  private def loadTriggerCollection( testModel: Model,
                                     generator: HashMap[TriggerIRI,Array[ValidatorIRI]] ): Array[Trigger] = {

    val triggerCollection = ArrayBuffer[Trigger]()

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
        currentTriggerID, Array[String](".*"), genericTestCases.toArray,
        "__GENERIC_IRI__", "generic iri trigger", "parses every iri"
      )
    )
    currentTriggerID+=1

    /*
    testModel based IRITrigger
     */
    val iriTriggersQuery = QueryFactory.create(iriTriggerQueryStr())

    QueryExecutionFactory.create(iriTriggersQuery, testModel).execSelect().foreach(

      triggersQuerySolution => {

        val testCases = ArrayBuffer[TestCase]()

        var isBlank = false
        val triggerIRI = {

          val triggerNode = triggersQuerySolution.getResource("trigger").asNode()

          if ( triggerNode.isBlank ) {

            isBlank = true
            triggerNode.getBlankNodeLabel
          }
          else
            triggerNode.getURI
        }

        val label = {

          if (triggersQuerySolution.contains("label")) triggersQuerySolution.getLiteral("label").getLexicalForm
          else ""
        }

        val triggerPatterns = triggersQuerySolution.getLiteral("patterns").getLexicalForm.split(delim)

        generator.getOrElse(triggerIRI,Array[ValidatorIRI]()).foreach(

          validatorIri => {
            validatorRef_TestApproachSet(validatorIri).foreach(

              testApproachID => {

                testCases.append(TestCase(currentTestCaseID, currentTriggerID, testApproachID))

                currentTestCaseID += 1
              }
            )
          }
        )

        triggerCollection.append( IRITrigger(currentTriggerID, triggerPatterns, testCases.toArray, triggerIRI, label, "") )
        currentTriggerID += 1
      }
    )

    /*
    defaultLiteralTrigger
     */
    triggerCollection.append(
      DefaultLiteralTrigger(currentTriggerID, Array[TestCase](),"__GENERIC_LITERAL__","Default Literal Trigger","")
    )
    currentTriggerID+=1

    /*
    testModel based literalTrigger
     */
    val literalTriggersQuery = QueryFactory.create(literalTriggerQueryStr())

    QueryExecutionFactory.create(literalTriggersQuery, testModel).execSelect().foreach(

      triggersQuerySolution => {

        val testCases = ArrayBuffer[TestCase]()

        var isBlank = false

        val triggerIRI = {

          val triggerNode = triggersQuerySolution.getResource("trigger").asNode()

          if ( triggerNode.isBlank ) {

            isBlank = true
            triggerNode.getBlankNodeLabel
          }
          else
            triggerNode.getURI
        }

        val label = {

          if (triggersQuerySolution.contains("label")) triggersQuerySolution.getLiteral("label").getLexicalForm
          else ""
        }

        val triggerDatatype = triggersQuerySolution.getResource("datatype").getURI

        generator.getOrElse(triggerIRI,Array[ValidatorIRI]()).foreach(

          validatorIri => {
            validatorRef_TestApproachSet(validatorIri).foreach(

              testApproachID => {

                testCases.append(TestCase(currentTestCaseID, currentTriggerID, testApproachID))

                currentTestCaseID += 1
              }
            )
          }
        )

        triggerCollection.append( LiteralTrigger(currentTriggerID, triggerDatatype, testCases.toArray, triggerIRI, label, "") )
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
    iri test approaches
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
        if( validatorQuerySolution.contains("oneOfVocabs") ) {

          validatorQuerySolution.getLiteral("oneOfVocabs").getLexicalForm.split(delim).foreach( vocabUrl => {

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

    /*
    datatypeLiteralValidator
     */
    val literalValidatorQuery = QueryFactory.create(literalValidatorQueryStr())

    QueryExecutionFactory.create(literalValidatorQuery, testModel).execSelect().foreach(

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
        if( validatorQuerySolution.contains("pattern") ) {

          val patternString = validatorQuerySolution.getLiteral("pattern").getLexicalForm

          testApproachCollection.append(DatatypeLiteralTestApproach(currentTestApproachID, patternString))
          grouepdTestApproachIDs.append(currentTestApproachID)
          currentTestApproachID +=1
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
