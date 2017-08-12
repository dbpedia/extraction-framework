package org.dbpedia.extraction.mappings.rml.model.template.json.std

import org.dbpedia.extraction.mappings.rml.model.template.analyzing.StdTemplatesAnalyzer$Test
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 12.08.17.
  */
class StdTemplatesJsonConverterTest extends FunSuite {

  test("testConvertAll") {

    val templates = StdTemplatesAnalyzer$Test.getExampleTemplateSet

    val converter = new StdTemplatesJsonConverter
    val jsonTemplate = converter.convertAll(templates)

    println(jsonTemplate)

  }

}
