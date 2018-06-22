package org.dbpedia.extraction.mappings.rml.model.factory

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, SimplePropertyTemplate, _}
import org.dbpedia.extraction.mappings.rml.util.JSONFactoryUtil

/**
  * Implementation of a TemplateFactory
  * Templates are created based on a bundle that contains a JsonNode (com.fasterxml.jackson.databind)
  */
object JSONTemplateFactory extends TemplateFactory {


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Interface Implementation
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  /**
    * Creates a ConstantTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createConstantTemplate(templateFactoryBundle: TemplateFactoryBundle): ConstantTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(ontologyPropertyParameter, bundle.ontology)
    val value = getParameter("value", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)

    // create template
    new ConstantTemplate(ontologyProperty, value, unit)
  }

  override def createIntermediateTemplate(templateFactoryBundle: TemplateFactoryBundle): IntermediateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)
    val ontology = bundle.ontology

    // set parameters
    val ontologyClassString = getParameter("class", bundle.templateNode)
    val ontologyClass = JSONFactoryUtil.getOntologyClass(ontologyClassString, ontology)
    val propertyString = getParameter("property", bundle.templateNode)
    val property = JSONFactoryUtil.getOntologyProperty(propertyString, bundle.ontology)
    val templateListNode = getParameterNode("templates", bundle.templateNode)
    val templates = getTemplates(JSONBundle(templateListNode, ontology)).toList

    // create template
    new IntermediateTemplate(ontologyClass, property, templates)

  }

  /**
    * Creates a StartDateTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createStartDateTemplate(templateFactoryBundle: TemplateFactoryBundle): StartDateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(ontologyPropertyParameter, bundle.ontology)
    val property = getParameter("property", bundle.templateNode)

    // create template
    new StartDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates an EndDateTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createEndDateTemplate(templateFactoryBundle: TemplateFactoryBundle): EndDateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(ontologyPropertyParameter, bundle.ontology)
    val property = getParameter("property", bundle.templateNode)

    // create template
    new EndDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates a GeoCoordinateTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createGeocoordinateTemplate(templateFactoryBundle: TemplateFactoryBundle): GeocoordinateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = if (getParameter("ontologyProperty", bundle.templateNode) != null) {
      val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", bundle.templateNode)
      JSONFactoryUtil.getOntologyProperty(ontologyPropertyParameter, bundle.ontology)
    } else null
    val coordinate = getParameter("coordinate", bundle.templateNode)
    val latitude = getParameter("latitude", bundle.templateNode)
    val longitude = getParameter("longitude", bundle.templateNode)
    val latitudeDegrees = getParameter("latitudeDegrees", bundle.templateNode)
    val latitudeMinutes = getParameter("latitudeMinutes", bundle.templateNode)
    val latitudeSeconds = getParameter("latitudeSeconds", bundle.templateNode)
    val latitudeDirection = getParameter("latitudeDirection", bundle.templateNode)
    val longitudeDegrees = getParameter("longitudeDegrees", bundle.templateNode)
    val longitudeMinutes = getParameter("longitudeMinutes", bundle.templateNode)
    val longitudeSeconds = getParameter("longitudeSeconds", bundle.templateNode)
    val longitudeDirection = getParameter("longitudeDirection", bundle.templateNode)

    // create template
    val template = GeocoordinateTemplate(ontologyProperty,
      coordinate,
      latitude,
      longitude,
      latitudeDegrees,
      latitudeMinutes,
      latitudeSeconds,
      latitudeDirection,
      longitudeDegrees,
      longitudeMinutes,
      longitudeSeconds,
      longitudeDirection)

    template
  }

  /**
    * Creates a SimplePropertyTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createSimplePropertyTemplate(templateFactoryBundle: TemplateFactoryBundle): SimplePropertyTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val property = getParameter("property", bundle.templateNode)
    val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(ontologyPropertyParameter, bundle.ontology)
    val select = getParameter("select", bundle.templateNode)
    val prefix = getParameter("prefix", bundle.templateNode)
    val suffix = getParameter("suffix", bundle.templateNode)
    val transform = getParameter("transform", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)
    val factor = getParameter("factor", bundle.templateNode)
    val doubleFactor = if (factor == null || factor.equals("null")) 1.0 else factor.toDouble

    //create template
    val template = SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, doubleFactor)

    template

  }

  /**
    * Creates a ConditionalTemplate object from a JSONBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createConditionalTemplate(templateFactoryBundle: TemplateFactoryBundle): ConditionalTemplate = {

    /////////////////////////////
    // Inner methods definitions
    /////////////////////////////

    def getFallbackTemplate(bundle: JSONBundle): ConditionalTemplate = {
      if (hasParameter("fallback", bundle.templateNode)) {
        val fallbackNode = getParameterNode("fallback", bundle.templateNode)
        createConditionalTemplate(JSONBundle(fallbackNode, bundle.ontology))
      } else null
    }

    def createCondition(conditionNode: JsonNode): Condition = {
      val operator = JSONFactoryUtil.get("operator", conditionNode)
      val property = getParameter("property", conditionNode)
      val value = getParameter("value", conditionNode)

      val condition = operator match {
        case Condition.ISSET => IsSetCondition(property)
        case Condition.EQUALS => EqualsCondition(property, value)
        case Condition.CONTAINS => ContainsCondition(property, value)
        case Condition.OTHERWISE => OtherwiseCondition()
        case _ => throw new IllegalArgumentException(Condition.ILLEGALARGUMENTMSG)
      }
      condition
    }

    def getCondition(templateNode: JsonNode): Condition = {
      if (hasParameter("condition", templateNode)) {
        val conditionNode = getParameterNode("condition", templateNode)
        createCondition(conditionNode)
      } else null
    }

    ////////////////
    // Main method
    ////////////////

    // get bundle
    val bundle = JSONFactoryUtil.getBundle(templateFactoryBundle)
    val templateNode = bundle.templateNode
    val ontology = bundle.ontology

    // set parameters
    val condition = getCondition(templateNode)
    val ontologyClassString = getParameter("class", templateNode)
    val ontologyClass = if (ontologyClassString != null) {
      JSONFactoryUtil.getOntologyClass(ontologyClassString, ontology)
    } else null
    val templateListNode = getParameterNode("templates", templateNode)
    val templates = getTemplates(JSONBundle(templateListNode, ontology))
    val fallbackTemplate = getFallbackTemplate(bundle)

    new ConditionalTemplate(condition, templates, ontologyClass, fallbackTemplate)

  }


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Util private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  private def getBundle(bundle: TemplateFactoryBundle): JSONBundle = {
    JSONFactoryUtil.getBundle(bundle)
  }

  private def getParameter(key: String, node: JsonNode): String = {
    JSONFactoryUtil.parameters(key, node)
  }

  private def getParameterNode(key: String, node: JsonNode): JsonNode = {
    JSONFactoryUtil.parametersNode(key, node)
  }

  private def hasParameter(key: String, node: JsonNode): Boolean = {
    JSONFactoryUtil.hasParameter(key, node)
  }

  private def getTemplates(bundle: JSONBundle): Seq[Template] = {

    def convertToTemplate(bundle: JSONBundle): Template = {
      val name = JSONFactoryUtil.get("name", bundle.templateNode)
      name match {
        case SimplePropertyTemplate.NAME => createSimplePropertyTemplate(bundle)
        case GeocoordinateTemplate.NAME => createGeocoordinateTemplate(bundle)
        case StartDateTemplate.NAME => createStartDateTemplate(bundle)
        case EndDateTemplate.NAME => createEndDateTemplate(bundle)
        case _ => throw new IllegalArgumentException("Incorrect template: " + name)
      }
    }

    val templatesSeq = JSONFactoryUtil.jsonNodeToSeq(bundle.templateNode)
    templatesSeq.map(template => {
      val newBundle = JSONBundle(template, bundle.ontology)
      convertToTemplate(newBundle)
    })
  }

}
