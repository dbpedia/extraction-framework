package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import org.dbpedia.extraction.mappings.rml.model.resource.RMLPredicateObjectMap
import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 11.08.17.
  */
object IntermediateTemplateAnalyzer extends TemplateAnalyzer{

  override def apply(pom: RMLPredicateObjectMap): Template = {
    val ptm = pom.objectMap.parentTriplesMap
    val templates = StdTemplatesAnalyzer.analyze(ptm)
    null
  }

}
