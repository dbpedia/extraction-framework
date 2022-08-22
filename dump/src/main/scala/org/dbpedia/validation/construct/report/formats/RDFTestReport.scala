package org.dbpedia.validation.construct.report.formats

import org.apache.jena.rdf.model.{Model, ModelFactory, ResourceFactory}
import org.apache.jena.vocabulary.RDFS
import org.dbpedia.validation.construct.model.TestScore
import org.dbpedia.validation.construct.model.triggers.Trigger
import org.dbpedia.validation.construct.model.validators.Validator


object RDFTestReport {

  type IRI = String

  class AdvancedJenaModel(m: Model) {

    def simpleStatement(s: IRI, p: IRI, o: IRI): Unit = {
      m.add(
        ResourceFactory.createStatement(
          ResourceFactory.createResource(s),
          ResourceFactory.createProperty(p),
          ResourceFactory.createResource(o)
        )
      )
    }

    def simpleLiteralStatement(s: IRI, p: IRI, o: Any): Unit = {

      m.add(
        ResourceFactory.createStatement(
          ResourceFactory.createResource(s),
          ResourceFactory.createProperty(p),
          ResourceFactory.createTypedLiteral(o)
        )
      )
    }
  }

  object ReportVocab {

    val base: String = "http://eval.dbpedia.org/"

    val Trigger: String = base + "Trigger#"
    val trigger: String = base + "trigger/"
    val TestCase: String = base + "TestCase#"
    val testCase: String = base + "testCase/"
    val TestApproach: String = base + "testapproach#"
    val testApproach: String = base + "testapproach/"
  }

  def buildRDFReport(label: String,
                     testScore: TestScore,
                     triggerCollection: Array[Trigger],
                     validatorCollection: Array[Validator]): Model = {


    implicit def toAdvancedJenaModel(m: Model): AdvancedJenaModel = new AdvancedJenaModel(m)

    val model = ModelFactory.createDefaultModel()

    triggerCollection.foreach(

      trigger => {

        val triggerPrevalence = testScore.prevalenceOfTriggers(trigger.ID)

        model.simpleLiteralStatement(trigger.iri, RDFS.label.getURI, trigger.label)
        model.simpleLiteralStatement(trigger.iri, RDFS.comment.getURI, trigger.comment)
        model.simpleLiteralStatement(trigger.iri, ReportVocab.trigger + "prevalence", triggerPrevalence)

        trigger.testCases.foreach(

          testCase => {

            val testCaseIRI = ReportVocab.TestCase + testCase.ID
            model.simpleStatement(trigger.iri, ReportVocab.trigger + "hasTestCase", testCaseIRI)

            model.simpleLiteralStatement(testCaseIRI,
              ReportVocab.testCase + "id", testCase.ID)
            model.simpleStatement(testCaseIRI,
              ReportVocab.testCase + "hasTrigger", trigger.iri)
            model.simpleStatement(testCaseIRI,
              ReportVocab.testCase + "hasApproach", ReportVocab.TestApproach + testCase.validatorID)
            model.simpleLiteralStatement(testCaseIRI,
              ReportVocab.testCase + "errors", testScore.errorsOfTestCases(testCase.ID))
          }
        )
      }
    )

    validatorCollection.foreach(
      validator => {
        val testApproachIRI = ReportVocab.TestApproach + validator.ID
        model.simpleLiteralStatement(testApproachIRI,
          ReportVocab.testApproach + "id", validator.ID)
        model.simpleLiteralStatement(testApproachIRI,
          ReportVocab.testApproach + "info", validator.info())
      }
    )
    model
  }
}
