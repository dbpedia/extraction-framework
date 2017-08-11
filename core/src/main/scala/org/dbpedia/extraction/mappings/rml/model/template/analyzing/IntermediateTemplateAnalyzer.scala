package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.Template
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 11.08.17.
  */
class IntermediateTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer{

  val logger = Logger.getGlobal

  override def apply(pom: RMLPredicateObjectMap): Template = {

    logger.info("Found " + RMLUri.INTERMEDIATEMAPPING)

    val ptm = pom.objectMap.parentTriplesMap
    val analyzer : TemplatesAnalyzer = new StdTemplatesAnalyzer(ontology)
    val templates = analyzer.analyze(ptm)
    null
  }

}
