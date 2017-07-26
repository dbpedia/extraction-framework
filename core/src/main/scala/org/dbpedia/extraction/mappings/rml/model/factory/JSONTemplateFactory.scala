package org.dbpedia.extraction.mappings.rml.model.factory
import java.net.URI

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, SimplePropertyTemplate, _}
import org.dbpedia.extraction.mappings.rml.util.JSONFactoryUtil
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 25.07.17.
  */
object JSONTemplateFactory extends TemplateFactory {

  /**
    * Creates a ConstantTemplate object from a JSONBundle object
    * @param templateFactoryBundle
    * @return
    */
  override def createConstantTemplate(templateFactoryBundle: TemplateFactoryBundle): ConstantTemplate = {
    // get bundle
    val bundle = JSONFactoryUtil.getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val value = JSONFactoryUtil.parameters("value", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)

    // create template
    new ConstantTemplate(ontologyProperty, value, unit)
  }

  override def createIntermediateTemplate(templateFactoryBundle: TemplateFactoryBundle): IntermediateTemplate = ???

  override def createStartDateTemplate(templateFactoryBundle: TemplateFactoryBundle): StartDateTemplate = ???

  override def createConditionalTemplate(templateFactoryBundle: TemplateFactoryBundle): ConditionalTemplate = ???

  override def createEndDateTemplate(templateFactoryBundle: TemplateFactoryBundle): EndDateTemplate = ???

  override def createGeocoordinateTemplate(templateFactoryBundle: TemplateFactoryBundle): GeocoordinateTemplate = ???

  /**
    * Creates a SimplePropertyTemplate object from a JSONBundle object
    * @param templateFactoryBundle
    * @return
    */
  override def createSimplePropertyTemplate(templateFactoryBundle: TemplateFactoryBundle): SimplePropertyTemplate = {
    // get bundle
    val bundle = JSONFactoryUtil.getBundle(templateFactoryBundle)

    // set parameters
    val property = JSONFactoryUtil.parameters("property", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val select = JSONFactoryUtil.parameters("select", bundle.templateNode)
    val prefix = JSONFactoryUtil.parameters("prefix", bundle.templateNode)
    val suffix = JSONFactoryUtil.parameters("suffix", bundle.templateNode)
    val transform = JSONFactoryUtil.parameters("transform", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)
    val factor = JSONFactoryUtil.parameters("factor", bundle.templateNode)
    val doubleFactor = if(factor == null || factor.equals("null")) 1.0 else factor.toDouble

    //create template
    new SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, doubleFactor)
  }
}
