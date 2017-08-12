package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.load.RMLInferencer$Test
import org.dbpedia.extraction.mappings.rml.model.template.Template
import org.dbpedia.extraction.mappings.rml.util.ContextCreator
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 11.08.17.
  */
class StdTemplatesAnalyzer$Test extends FunSuite {

  test("testAnalyze") {

    val model = RMLInferencer$Test.getInferencedMappingExampleAsRMLModel
    val tm = model.triplesMap
    val ontology = ContextCreator.ontologyObject

    val analyzer : TemplatesAnalyzer = new StdTemplatesAnalyzer(ontology)
    val templates = analyzer.analyze(tm)

    templates.foreach(template => println(template))

  }

}

object StdTemplatesAnalyzer$Test {

  def getExampleTemplateSet : Set[Template] = {

    val model = RMLInferencer$Test.getInferencedMappingExampleAsRMLModel
    val tm = model.triplesMap
    val ontology = ContextCreator.ontologyObject

    val analyzer : TemplatesAnalyzer = new StdTemplatesAnalyzer(ontology)
    val templates = analyzer.analyze(tm)
    templates

  }

}
