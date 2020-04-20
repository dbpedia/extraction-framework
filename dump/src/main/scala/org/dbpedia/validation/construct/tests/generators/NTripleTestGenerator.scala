package org.dbpedia.validation.construct.tests.generators

import java.io.InputStreamReader
import java.net.URL

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.apache.jena.riot.{RDFDataMgr, RDFLanguages}
import org.dbpedia.validation.construct.model.triggers._
import org.dbpedia.validation.construct.model.{TestCase, TestCaseType, TriggerIRI, ValidatorID, ValidatorIRI}
import org.dbpedia.validation.construct.model.validators._

import scala.collection.mutable.ArrayBuffer
import scala.collection.JavaConversions._
import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.mutable

object NTripleTestGenerator extends TestGenerator {

  private val delim = "\t"

  def loadTestGenerator(testModel: Model): HashMap[TriggerIRI, Array[ValidatorIRI]] = {

    val testGeneratorQuery = QueryFactory.create(Queries.testGeneratorQueryStr)

    HashMap(
      QueryExecutionFactory.create(testGeneratorQuery, testModel).execSelect().map(generatorResult => {
        val triggerIRI = {
          val triggerNode = generatorResult.getResource("trigger").asNode()
          if (triggerNode.isBlank) triggerNode.getBlankNodeLabel
          else triggerNode.getURI
        }
        val validatorIRI = {
          val validatorNode = generatorResult.getResource("validator").asNode()
          if (validatorNode.isBlank) validatorNode.getBlankNodeLabel
          else validatorNode.getURI
        }
        triggerIRI -> validatorIRI
      }).toArray.groupBy(_._1).map(entry => entry._1 -> entry._2.map(_._2)).toArray: _*
    )
  }

  def generateTriggerCollection(testModel: Model,
                                generator: HashMap[TriggerIRI, Array[ValidatorIRI]],
                                validatorMap: HashMap[ValidatorIRI, Array[ValidatorID]]): (Array[Trigger], Array[TestCase]) = {

    // TODO scala like without mutable variable
    var currentTestCaseID = 0
    var currentTriggerID = 0
    val triggerCollection = ArrayBuffer[Trigger]()
    val testCaseCollection = ArrayBuffer[TestCase]()

    /*
    Generic trigger
     */
    val generic_literal_testcase = TestCase(currentTestCaseID, currentTriggerID, validatorMap("#GENERIC_VALIDATOR").head, TestCaseType.GENERIC)
    triggerCollection.append(GenericLiteralTrigger(currentTriggerID, Array[TestCase](generic_literal_testcase)))
    testCaseCollection.append(generic_literal_testcase)
    currentTestCaseID += 1
    currentTriggerID += 1

    val generic_iri_testcase = TestCase(currentTestCaseID, currentTriggerID, validatorMap("#GENERIC_IRI_VALIDATOR").head, TestCaseType.GENERIC)
    triggerCollection.append(GenericIRITrigger(currentTriggerID, Array[TestCase](generic_iri_testcase)))
    testCaseCollection.append(generic_iri_testcase)
    currentTestCaseID += 1
    currentTriggerID += 1

    /*
    Custom IRI trigger
     */
    val iriTriggersQuery = QueryFactory.create(Queries.iriTriggerQueryStr())

    QueryExecutionFactory.create(iriTriggersQuery, testModel)
      .execSelect()
      .foreach(triggersQuerySolution => {

        val testCases = ArrayBuffer[TestCase]()

        val triggerIRI = {
          val triggerNode = triggersQuerySolution.getResource("trigger").asNode()
          if (triggerNode.isBlank) triggerNode.getBlankNodeLabel
          else triggerNode.getURI
        }

        val label = {
          if (triggersQuerySolution.contains("label")) triggersQuerySolution.getLiteral("label").getLexicalForm else ""
        }

        val triggerPatterns = triggersQuerySolution.getLiteral("patterns").getLexicalForm.split(delim)
        generator.getOrElse(triggerIRI, Array[ValidatorIRI]()).foreach(validatorIri => {
          validatorMap(validatorIri).foreach(testApproachID => {
            val testCase = TestCase(currentTestCaseID, currentTriggerID, testApproachID, TestCaseType.CUSTOM)
            testCases.append(testCase)
            testCaseCollection.append(testCase)
            currentTestCaseID += 1
          })
        })

        if (testCases.isEmpty) {
          val testCase = TestCase(currentTestCaseID, currentTriggerID, validatorMap("#GENERIC_VALIDATOR").head, TestCaseType.CUSTOM)
          testCases.append(testCase)
          testCaseCollection.append(testCase)
          currentTestCaseID += 1
        }

        triggerCollection.append(IRITrigger(currentTriggerID, triggerPatterns, testCases.toArray, triggerIRI, label, ""))
        currentTriggerID += 1
      })

    /*
    Custom literal trigger
     */
    val literalTriggersQuery = QueryFactory.create(Queries.literalTriggerQueryStr())

    QueryExecutionFactory.create(literalTriggersQuery, testModel).execSelect().foreach(triggersQuerySolution => {

      val testCases = ArrayBuffer[TestCase]()

      val triggerIRI = {
        val triggerNode = triggersQuerySolution.getResource("trigger").asNode()
        if (triggerNode.isBlank) triggerNode.getBlankNodeLabel
        else triggerNode.getURI
      }

      val label = {
        if (triggersQuerySolution.contains("label")) triggersQuerySolution.getLiteral("label").getLexicalForm else ""
      }

      val triggerDatatype = triggersQuerySolution.getResource("datatype").getURI

      generator.getOrElse(triggerIRI, Array[ValidatorIRI]()).foreach(validatorIri => {
        validatorMap(validatorIri).foreach(testApproachID => {
          val testCase = TestCase(currentTestCaseID, currentTriggerID, testApproachID, TestCaseType.CUSTOM)
          testCases.append(testCase)
          testCaseCollection.append(testCase)
          currentTestCaseID += 1
        })
      })

      triggerCollection.append(TypedLiteralTrigger(currentTriggerID, triggerDatatype, testCases.toArray, triggerIRI, label, ""))
      currentTriggerID += 1
    })
    (triggerCollection.toArray, testCaseCollection.toArray)
  }

  def generateValidators(testModel: Model): (Array[Validator], HashMap[ValidatorIRI, Array[ValidatorID]]) = {

    var currentValidatorID = 0
    val validatorCollection = ArrayBuffer[Validator]()
    val validatorMap = new mutable.HashMap[ValidatorIRI, Array[ValidatorID]]

    /*
    generic validators
     */
    val genericIRIValidator = GenericIRIValidator(currentValidatorID)
    validatorCollection.append(genericIRIValidator)
    validatorMap.put(genericIRIValidator.iri, Array[Int](genericIRIValidator.ID))
    currentValidatorID += 1

    val genericLiteralValidator = GenericLiteralValidator(currentValidatorID)
    validatorCollection.append(genericLiteralValidator)
    validatorMap.put(genericLiteralValidator.iri, Array[Int](genericLiteralValidator.ID))
    currentValidatorID += 1

    val genericValidator = GenericValidator(currentValidatorID)
    validatorCollection.append(genericValidator)
    validatorMap.put(genericValidator.iri, Array[Int](genericValidator.ID))
    currentValidatorID += 1

    /*
    iri validators
     */
    val validatorQuery = QueryFactory.create(Queries.iriValidatorQueryStr())

    QueryExecutionFactory.create(validatorQuery, testModel).execSelect().foreach(

      validatorQuerySolution => {

        val groupedValidators = ArrayBuffer[Int]()

        /*
        ?iri
         */
        val validatorIRI: ValidatorIRI = {
          val validatorNode = validatorQuerySolution.get("validator").asNode()
          if (validatorNode.isBlank) validatorNode.getBlankNodeLabel
          else validatorNode.getURI
        }

        /*
        rdfs:comment
         */
        val comment = StringBuilder.newBuilder
        if (validatorQuerySolution.contains("comment")) {
          comment.append(validatorQuerySolution.getLiteral("comment").getLexicalForm)
        }

        /*
        v:pattern
         */
        if (validatorQuerySolution.contains("patterns")) {

          validatorQuerySolution.getLiteral("patterns").getLexicalForm.split(delim).foreach(patternString => {

            validatorCollection.append(PatternValidator(currentValidatorID, validatorIRI, patternString))
            groupedValidators.append(currentValidatorID)
            currentValidatorID += 1
          })
        }

        /*
        v:oneOfVocab
         */
        if (validatorQuerySolution.contains("oneOfVocabs")) {

          validatorQuerySolution.getLiteral("oneOfVocabs").getLexicalForm.split(delim).foreach(vocabUrl => {

            validatorCollection.append(VocabValidator(currentValidatorID, validatorIRI, vocabUrl, getVocab(vocabUrl)))
            groupedValidators.append(currentValidatorID)
            currentValidatorID += 1
          })
        }

        /*
        v:doesNotContain
         */
        if (validatorQuerySolution.contains("doesNotContains")) {

          validatorQuerySolution.getLiteral("doesNotContains").getLexicalForm.split(delim).foreach(charSeq => {

            validatorCollection.append(NotContainsValidator(currentValidatorID, validatorIRI, charSeq))
            groupedValidators.append(currentValidatorID)
            currentValidatorID += 1
          })
        }

        validatorMap.put(validatorIRI, groupedValidators.toArray)
      }
    )

    /*
    typedLiteralValidator
     */
    val literalValidatorQuery = QueryFactory.create(Queries.literalValidatorQueryStr())

    QueryExecutionFactory.create(literalValidatorQuery, testModel).execSelect().foreach(

      validatorQuerySolution => {

        val grouepdTestApproachIDs = ArrayBuffer[Int]()

        val validatorIRI: ValidatorIRI = {
          val validatorNode = validatorQuerySolution.get("validator").asNode()
          if (validatorNode.isBlank) validatorNode.getBlankNodeLabel
          else validatorNode.getURI
        }

        /*
        rdfs:comment
         */
        val comment = StringBuilder.newBuilder
        if (validatorQuerySolution.contains("comment")) {
          comment.append(validatorQuerySolution.getLiteral("comment").getLexicalForm)
        }

        /*
        v:pattern
         */
        if (validatorQuerySolution.contains("pattern")) {

          val patternString = validatorQuerySolution.getLiteral("pattern").getLexicalForm

          validatorCollection.append(TypedLiteralValidator(currentValidatorID, validatorIRI, patternString))
          grouepdTestApproachIDs.append(currentValidatorID)
          currentValidatorID += 1
        }

        validatorMap.put(validatorIRI, grouepdTestApproachIDs.toArray)
      }
    )

    (validatorCollection.toArray, HashMap[ValidatorIRI, Array[ValidatorID]]() ++ validatorMap)
  }

  def getVocab(urlStr: String): HashSet[String] = {

    val url = new URL(urlStr)
    val reader = new InputStreamReader(url.openStream, "UTF-8")
    val model = ModelFactory.createDefaultModel()

    RDFDataMgr.read(model, reader, "urn:base", RDFLanguages.NTRIPLES)

    val query = QueryFactory.create(Queries.oneOfVocabQueryStr)
    val resultSet = QueryExecutionFactory.create(query, model).execSelect

    val properties = ArrayBuffer[String]()

    while (resultSet.hasNext) {
      properties.append(resultSet.next().getResource("property").getURI)
    }

    HashSet(properties.toArray: _*)
  }
}
