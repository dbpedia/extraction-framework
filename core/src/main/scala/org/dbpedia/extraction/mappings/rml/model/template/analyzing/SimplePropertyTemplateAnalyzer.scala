package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap}
import org.dbpedia.extraction.mappings.rml.model.template.Template

/**
  * Created by wmaroy on 11.08.17.
  */
object SimplePropertyTemplateAnalyzer extends TemplateAnalyzer {

  def apply(pom: RMLPredicateObjectMap): Template = {

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null

  }

}
