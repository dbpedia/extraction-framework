package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template._

/**
  * Created by wmaroy on 11.08.17.
  */
object StdTemplatesAnalyzer extends TemplatesAnalyzer {

  val logger = Logger.getGlobal

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Public methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def analyze(tm : RMLTriplesMap): Set[Template] = {

    logger.info("Analyzing templates in " + tm.resource.getURI)

    val templates = tm.predicateObjectMaps.map(pom => {

      val template = analyze(pom)
      template

    })

    templates.toSet

  }

  def analyze(pom : RMLPredicateObjectMap) : Template = {

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

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def analyzeSimplePropertyTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.SIMPLEPROPERTYMAPPING)
    val template = SimplePropertyTemplateAnalyzer(pom)
    template
  }

  private def analyzeConstantTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.CONSTANTMAPPING)
    val template = ConstantTemplateAnalyzer(pom)
    template
  }

  private def analyzeStartDateTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.STARTDATEMAPPING)
    val template = StartDateTemplateAnalyzer(pom)
    template
  }

  private def analyzeEndDateTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.ENDDATEMAPPING)
    val template = EndDateTemplateAnalyzer(pom)
    template
  }

  private def analyzeGeocoordinateTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.LONGITUDEMAPPING + "/" + RMLUri.LATITUDEMAPPING)
    val template = GeocoordinatesTemplateAnalyzer(pom)
    template
  }

  private def analyzeIntermediateTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.INTERMEDIATEMAPPING)
    val template = IntermediateTemplateAnalyzer(pom)
    template
  }

  private def analyzeConditionalTemplate(pom : RMLPredicateObjectMap) : Template = {

    logger.info("Found " + RMLUri.CONDITIONALMAPPING)
    null
  }


}
