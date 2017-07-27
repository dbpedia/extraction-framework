package org.dbpedia.extraction.mappings.rml.model.factory
import java.net.URI

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.{EndDateTemplate, SimplePropertyTemplate, _}
import org.dbpedia.extraction.mappings.rml.util.JSONFactoryUtil
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}

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
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val value = getParameter("value", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)

    // create template
    new ConstantTemplate(ontologyProperty, value, unit)
  }

  override def createIntermediateTemplate(templateFactoryBundle: TemplateFactoryBundle): IntermediateTemplate = ???

  /**
    * Creates a StartDateTemplate object from a JSONBundle object
    * @param templateFactoryBundle
    * @return
    */
  override def createStartDateTemplate(templateFactoryBundle: TemplateFactoryBundle): StartDateTemplate = {
    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val property = getParameter("property", bundle.templateNode)

    // create template
    new StartDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates an EndDateTemplate object from a JSONBundle object
    * @param templateFactoryBundle
    * @return
    */
  override def createEndDateTemplate(templateFactoryBundle: TemplateFactoryBundle): EndDateTemplate = {
    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val property = getParameter("property", bundle.templateNode)

    // create template
    new EndDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates a GeoCoordinateTemplate object from a JSONBundle object
    * @param templateFactoryBundle
    * @return
    */
  override def createGeocoordinateTemplate(templateFactoryBundle: TemplateFactoryBundle): GeocoordinateTemplate = {
    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val ontologyProperty = if(getParameter("ontologyProperty", bundle.templateNode) != null) {
      JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
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
    * @param templateFactoryBundle
    * @return
    */
  override def createSimplePropertyTemplate(templateFactoryBundle: TemplateFactoryBundle): SimplePropertyTemplate = {
    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    // set parameters
    val property = getParameter("property", bundle.templateNode)
    val ontologyProperty = JSONFactoryUtil.getOntologyProperty(bundle.templateNode, bundle.ontology)
    val select = getParameter("select", bundle.templateNode)
    val prefix = getParameter("prefix", bundle.templateNode)
    val suffix = getParameter("suffix", bundle.templateNode)
    val transform = getParameter("transform", bundle.templateNode)
    val unit = JSONFactoryUtil.getUnit(bundle.templateNode, bundle.ontology)
    val factor = getParameter("factor", bundle.templateNode)
    val doubleFactor = if(factor == null || factor.equals("null")) 1.0 else factor.toDouble

    //create template
    val template = SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, doubleFactor)

    template

  }

  override def createConditionalTemplate(templateFactoryBundle: TemplateFactoryBundle): ConditionalTemplate = {

    def getFallbackTemplate(bundle: JSONBundle) : ConditionalTemplate = {
      if(bundle.templateNode.has("fallback")) {
        val fallbackNode = getParameterNode("fallback", bundle.templateNode)
        createConditionalTemplate(JSONBundle(fallbackNode, bundle.ontology))
      } else null
    }

    // get bundle
    val bundle = JSONFactoryUtil.getBundle(templateFactoryBundle)
    val templateNode = bundle.templateNode
    val ontology = bundle.ontology

    val conditionNode = getParameterNode("condition", templateNode)
    val condition = createCondition(conditionNode)

    val ontologyClassString = getParameter("class", templateNode)
    val ontologyClass = JSONFactoryUtil.getOntologyClass(ontologyClassString, ontology)

    val templateListNode = getParameterNode("templates", templateNode)
    val templates = getTemplates(JSONBundle(templateListNode, ontology))

    val fallbackTemplate = getFallbackTemplate(bundle)

    new ConditionalTemplate(condition, templates, ontologyClass, fallbackTemplate)

  }

  private def getBundle(bundle : TemplateFactoryBundle) : JSONBundle = {
    JSONFactoryUtil.getBundle(bundle)
  }
  
  private def getParameter(key : String, node : JsonNode) : String = {
    JSONFactoryUtil.parameters(key, node)
  }
  
  private def getParameterNode(key: String, node : JsonNode) : JsonNode = {
    JSONFactoryUtil.parametersNode(key, node)
  }

  private def getTemplates(bundle : JSONBundle) : Seq[Template] = {

    def convertToTemplate(templateNode : JsonNode) : Template = {
      val name = JSONFactoryUtil.get("name", templateNode)
      name match {
        case SimplePropertyTemplate.NAME => createSimplePropertyTemplate(bundle)
        case GeocoordinateTemplate.NAME => createGeocoordinateTemplate(bundle)
        case StartDateTemplate.NAME => createStartDateTemplate(bundle)
        case EndDateTemplate.NAME => createEndDateTemplate(bundle)
        case _ => throw new IllegalArgumentException("Incorrect template name is found: " + name)
      }
    }

    val templatesSeq = JSONFactoryUtil.jsonNodeToSeq(bundle.templateNode)
    templatesSeq.map(template => convertToTemplate(template))
  }

  private def createCondition(conditionNode : JsonNode) : Condition = {
    val operator = JSONFactoryUtil.get("operator", conditionNode)
    val property = getParameter("property", conditionNode)
    val value = getParameter("value", conditionNode)

    val condition = operator match {
      case Condition.ISSET => IsSetCondition(property)
      case Condition.EQUALS => EqualsCondition(property, value)
      case Condition.CONTAINS => ContainsCondition(property, value)
      case Condition.OTHERWISE => OtherwiseCondition()
    }

    condition
  }







}
