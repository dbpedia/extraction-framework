package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 11.08.17.
  */
class StdTemplatesAnalyzer(ontology: Ontology) extends TemplatesAnalyzer {

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

      case s : String if uri.contains(RMLUri.SIMPLEPROPERTYMAPPING) => analyzeTemplate(new SimplePropertyTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.CONDITIONALMAPPING) => null
      case s : String if uri.contains(RMLUri.CONSTANTMAPPING) => analyzeTemplate(new ConstantTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.STARTDATEMAPPING) => analyzeTemplate(new StartDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.ENDDATEMAPPING) => analyzeTemplate(new EndDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.INTERMEDIATEMAPPING) => analyzeTemplate(new IntermediateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.LONGITUDEMAPPING) => analyzeTemplate(new GeocoordinatesTemplateAnalyzer(ontology), pom)

    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def analyzeTemplate(analyzer : TemplateAnalyzer, pom : RMLPredicateObjectMap) : Template = {
    analyzer(pom)
  }


}
