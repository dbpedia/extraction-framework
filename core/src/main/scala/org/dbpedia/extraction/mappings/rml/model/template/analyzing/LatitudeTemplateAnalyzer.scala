package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{LatitudeTemplate, Template}
import org.dbpedia.extraction.ontology.{Ontology, RdfNamespace}

/**
  * Created by wmaroy on 11.08.17.
  */
class LatitudeTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer {

  val logger = Logger.getGlobal

  def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.LATITUDEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    val coordParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "coordParameter", null)
    val latParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "latParameter", null)
    val latDegreesParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "latDegreesParameter", null)
    val latMinutesParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "latMinutesParameter", null)
    val latSecondsParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "latSecondsParameter", null)
    val latDirectionParameter = fn.references.getOrElse(RdfNamespace.DBF.namespace + "latDirectionParameter", null)

    LatitudeTemplate(coordParameter, latParameter, latDegreesParameter, latMinutesParameter, latSecondsParameter, latDirectionParameter)

  }

}
