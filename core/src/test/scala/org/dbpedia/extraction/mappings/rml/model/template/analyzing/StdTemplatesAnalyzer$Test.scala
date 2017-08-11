package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.load.{RMLInferencer$Test}
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 11.08.17.
  */
class StdTemplatesAnalyzer$Test extends FunSuite {

  test("testAnalyze") {

    val model = RMLInferencer$Test.getInferencedMappingExampleAsRMLModel
    val tm = model.triplesMap

    StdTemplatesAnalyzer.analyze(tm)

  }

}
