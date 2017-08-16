package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{SimplePropertyTemplate, Template}
import org.dbpedia.extraction.ontology.{Ontology, RdfNamespace}

/**
  * Created by wmaroy on 11.08.17.
  */
class SimplePropertyTemplateAnalyzer(ontology: Ontology) extends AbstractTemplateAnalyzer(ontology) {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.SIMPLEPROPERTYMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]

    if(ftm == null) throw new IllegalArgumentException("Invalid Simple Property definition: " + pom.resource.getURI)

    val fn = ftm.getFunction

    val ontologyParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "ontologyPropertyParameter", null)
    val propertyParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "propertyParameter", null)
    val factorParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "factorParameter", "1").toDouble
    val selectParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "selectParameter", null)
    val unitParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "unitParameter", null)
    val prefixParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "prefixParameter", null)
    val suffixParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "suffixParameter", null)
    val transformParameter = fn.constants.getOrElse(RdfNamespace.DBF.namespace + "selectParameter", null)

    val ontologyProperty = loadProperty(ontologyParameter)
    val unit = if(unitParameter != null) loadDatatype(unitParameter) else null

    SimplePropertyTemplate(propertyParameter, ontologyProperty, selectParameter, prefixParameter, suffixParameter, transformParameter, unit, factorParameter)

  }

}
