package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.Template
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 11.08.17.
  */
class ConstantTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.CONSTANTMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null

  }

}
