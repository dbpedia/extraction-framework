package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import be.ugent.mmlab.rml.model.RMLMapping
import org.dbpedia.extraction.mappings.rml.load.{RMLInferencer, RMLInferencer$Test}
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.util.Resources
import org.dbpedia.extraction.util.Language
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 11.08.17.
  */
class StdTemplateAnalyzer$Test extends FunSuite {

  test("testAnalyze") {

    val mapping = RMLInferencer$Test.getInferencedMappingExampleAsRMLModel
    mapping

  }

}
