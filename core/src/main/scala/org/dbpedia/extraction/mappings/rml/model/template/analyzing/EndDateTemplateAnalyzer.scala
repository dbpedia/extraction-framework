package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, StartDateTemplate, Template}
import org.dbpedia.extraction.ontology.{Ontology, RdfNamespace}

/**
  * Created by wmaroy on 11.08.17.
  */
class EndDateTemplateAnalyzer(ontology : Ontology) extends AbstractTemplateAnalyzer(ontology) {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.ENDDATEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    val ontologyProperty = loadProperty(pom.rrPredicate)
    val property = fn.references(RdfNamespace.DBF.namespace + "endDatePropertyParameter")

    EndDateTemplate(property, ontologyProperty)

  }

}
