package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{ConstantTemplate, Template}
import org.dbpedia.extraction.ontology.{Ontology, RdfNamespace}

/**
  * Created by wmaroy on 11.08.17.
  */
class ConstantTemplateAnalyzer(ontology: Ontology) extends AbstractTemplateAnalyzer(ontology) {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.CONSTANTMAPPING)

    val ontologyProperty = loadProperty(pom.rrPredicate)

    if(pom.hasFunctionTermMap) {

      val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
      val fn = ftm.getFunction

      val unitParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "unitParameter", null)
      val unit = loadDatatype(unitParameter)
      val value = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "valueParameter", null)

      ConstantTemplate(ontologyProperty, value, unit)

    } else {

      val value = pom.rrObject
      val unit = null

      ConstantTemplate(ontologyProperty, value, unit)

    }

  }

}
