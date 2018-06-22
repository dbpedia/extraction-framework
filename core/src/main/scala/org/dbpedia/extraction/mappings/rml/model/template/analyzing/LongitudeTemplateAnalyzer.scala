package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{LongitudeTemplate, Template}
import org.dbpedia.extraction.ontology.{Ontology, RdfNamespace}

/**
  * Created by wmaroy on 11.08.17.
  */
class LongitudeTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.LATITUDEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    val coordParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "coordParameter", null)
    val lonParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "lonParameter", null)
    val lonDegreesParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "lonDegreesParameter", null)
    val lonMinutesParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "lonMinutesParameter", null)
    val lonSecondsParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "lonSecondsParameter", null)
    val lonDirectionParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "lonDirectionParameter", null)

    LongitudeTemplate(coordParameter, lonParameter, lonDegreesParameter, lonMinutesParameter, lonSecondsParameter, lonDirectionParameter)

  }

}
