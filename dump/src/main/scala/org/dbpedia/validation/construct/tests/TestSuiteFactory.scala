package org.dbpedia.validation.construct.tests

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.dbpedia.validation.construct.tests.generators.NTripleTestGenerator
import org.dbpedia.validation.construct.tests.suites.{NTripleTestSuite, TestSuite}

object TestSuiteFactory {

  object TestSuiteType extends Enumeration {

    val NTriples: Value = Value
  }

  def create(testModel: Model, format: TestSuiteType.Value): TestSuite = {
    format match {
      case TestSuiteType.NTriples =>

        val generator =
          NTripleTestGenerator.loadTestGenerator(testModel)
        val (validatorCollection, validatorMap) =
          NTripleTestGenerator.generateValidators(testModel)
        val (triggerCollection, testCaseCollection) =
          NTripleTestGenerator.generateTriggerCollection(testModel, generator, validatorMap)

        new NTripleTestSuite(triggerCollection, validatorCollection, testCaseCollection)
    }
  }
}