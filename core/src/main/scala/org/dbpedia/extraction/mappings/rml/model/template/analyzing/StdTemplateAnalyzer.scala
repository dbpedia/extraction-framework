package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLFunctionTermMap, RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template._

/**
  * Created by wmaroy on 11.08.17.
  */
object StdTemplateAnalyzer extends TemplateAnalyzer {

  val logger = Logger.getGlobal

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Public methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def analyze(rmlModel : RMLModel): AnalyzedRMLModel = {

    val templates = analyzeTemplates(rmlModel)
    new AnalyzedRMLModel(rmlModel, templates)

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def analyzeTemplates(rmlModel: RMLModel) : Set[Template] = {

    logger.info("Analyzing templates in " + rmlModel.name)

    val templates = rmlModel.triplesMap.predicateObjectMaps.map(pom => {

      val template = getTemplateFromPom(pom)
      template

    })

    templates.toSet

  }

  private def getTemplateFromPom(pom : RMLPredicateObjectMap) : Template = {

    val uri = pom.resource.getURI
    uri match {

      case s : String if uri.contains(RMLUri.SIMPLEPROPERTYMAPPING) => analyzeSimplePropertyTemplate(pom)
      case s : String if uri.contains(RMLUri.CONDITIONALMAPPING) => analyzeConditionalTemplate(pom)
      case s : String if uri.contains(RMLUri.CONSTANTMAPPING) => analyzeConstantTemplate(pom)
      case s : String if uri.contains(RMLUri.STARTDATEMAPPING) => analyzeStartDateTemplate(pom)
      case s : String if uri.contains(RMLUri.ENDDATEMAPPING) => analyzeEndDateTemplate(pom)
      case s : String if uri.contains(RMLUri.INTERMEDIATEMAPPING) => analyzeIntermediateTemplate(pom)
      case s : String if uri.contains(RMLUri.LONGITUDEMAPPING) => analyzeGeocoordinateTemplate(pom)

    }

  }


  private def analyzeSimplePropertyTemplate(pom : RMLPredicateObjectMap) : SimplePropertyTemplate = {

    logger.info("Found " + RMLUri.SIMPLEPROPERTYMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null
  }

  private def analyzeConstantTemplate(pom : RMLPredicateObjectMap) : ConstantTemplate = {

    logger.info("Found " + RMLUri.CONSTANTMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null
  }

  private def analyzeStartDateTemplate(pom : RMLPredicateObjectMap) : StartDateTemplate = {

    logger.info("Found " + RMLUri.STARTDATEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null
  }

  private def analyzeEndDateTemplate(pom : RMLPredicateObjectMap) : EndDateTemplate = {

    logger.info("Found " + RMLUri.ENDDATEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null
  }

  private def analyzeGeocoordinateTemplate(pom : RMLPredicateObjectMap) : GeocoordinateTemplate = {

    logger.info("Found " + RMLUri.LONGITUDEMAPPING + "/" + RMLUri.LATITUDEMAPPING)

    val ftm = pom.objectMap.asInstanceOf[RMLFunctionTermMap]
    val fn = ftm.getFunction

    null
  }

  private def analyzeIntermediateTemplate(pom : RMLPredicateObjectMap) : IntermediateTemplate = {

    logger.info("Found " + RMLUri.INTERMEDIATEMAPPING)

    val ptm = pom.objectMap.parentTriplesMap
    val templates = ptm.predicateObjectMaps.map(pom => {
      val template = getTemplateFromPom(pom)
      template
    })

    null
  }

  private def analyzeConditionalTemplate(pom : RMLPredicateObjectMap) : ConditionalTemplate = {

    logger.info("Found " + RMLUri.CONDITIONALMAPPING)

    null
  }


}
