package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.RMLPredicateObjectMap
import org.dbpedia.extraction.mappings.rml.model.template._

/**
  * Created by wmaroy on 11.08.17.
  */
object StdTemplateAnalyzer extends TemplateAnalyzer {

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

    val templates = rmlModel.triplesMap.predicateObjectMaps.map(pom => {

      val template = getTemplateFromPom(pom)
      template

    })

    templates.toSet

  }

  private def getTemplateFromPom(pom : RMLPredicateObjectMap) : Template = {

    val uri = pom.resource.getURI
    uri match {

      case s : String if uri.contains(SimplePropertyTemplate.NAME) => analyzeSimplePropertyTemplate(pom)
      case s : String if uri.contains(ConditionalTemplate.NAME) => analyzeConditionalTemplate(pom)
      case s : String if uri.contains(ConstantTemplate.NAME) => analyzeConstantTemplate(pom)
      case s : String if uri.contains(StartDateTemplate.NAME) => analyzeStartDateTemplate(pom)
      case s : String if uri.contains(EndDateTemplate.NAME) => analyzeEndDateTemplate(pom)
      case s : String if uri.contains(IntermediateTemplate.NAME) => analyzeIntermediateTemplate(pom)
      case s : String if uri.contains(GeocoordinateTemplate.NAME) => analyzeGeocoordinateTemplate(pom)

    }
    
  }


  private def analyzeSimplePropertyTemplate(pom : RMLPredicateObjectMap) : SimplePropertyTemplate = {
    null
  }

  private def analyzeConstantTemplate(pom : RMLPredicateObjectMap) : ConstantTemplate = {
    null
  }

  private def analyzeStartDateTemplate(pom : RMLPredicateObjectMap) : StartDateTemplate = {
    null
  }

  private def analyzeEndDateTemplate(pom : RMLPredicateObjectMap) : EndDateTemplate = {
    null
  }

  private def analyzeGeocoordinateTemplate(pom : RMLPredicateObjectMap) : GeocoordinateTemplate = {
    null
  }

  private def analyzeIntermediateTemplate(pom : RMLPredicateObjectMap) : IntermediateTemplate = {
    null
  }

  private def analyzeConditionalTemplate(pom : RMLPredicateObjectMap) : ConditionalTemplate = {
    null
  }


}
