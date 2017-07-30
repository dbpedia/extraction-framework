package org.dbpedia.extraction.mappings.rml.model.factory
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.exception.TemplateFactoryBundleException
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.ontology.OntologyProperty

/**
  * Created by wmaroy on 30.07.17.
  */
object WikiTextTemplateFactory extends TemplateFactory {


  /**
    * Creates a ConstantTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createConstantTemplate(templateFactoryBundle: TemplateFactoryBundle): ConstantTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    val mapping = bundle.propertyMapping.asInstanceOf[ConstantMapping]
    val ontologyProperty = mapping.ontologyProperty
    val value = mapping.value
    val unit = mapping.datatype

    new ConstantTemplate(ontologyProperty, value, unit)
  }

  /**
    * Creates an IntermediateTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createIntermediateTemplate(templateFactoryBundle: TemplateFactoryBundle): IntermediateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    val mapping = bundle.propertyMapping.asInstanceOf[IntermediateNodeMapping]
    val ontologyClass = mapping.nodeClass
    val propertyMappings = mapping.mappings
    val templates = getTemplates(propertyMappings)
    val property = mapping.correspondingProperty

    new IntermediateTemplate(ontologyClass, property, templates)
  }

  /**
    * Creates a StartDateTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createStartDateTemplate(templateFactoryBundle: TemplateFactoryBundle): StartDateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)

    val mapping = bundle.propertyMapping.asInstanceOf[DateIntervalMapping]
    val ontologyProperty = mapping.endDateOntologyProperty
    val property = mapping.templateProperty

    new StartDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates a ConditionalTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createConditionalTemplate(templateFactoryBundle: TemplateFactoryBundle): ConditionalTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)
    val mapping = bundle.propertyMapping.asInstanceOf[ConditionalMapping]

    val conditionalTemplate = mapping.cases.foldRight(null : ConditionalTemplate)((conditionalMapping, fallback) => {

      val templateMapping = conditionalMapping.mapping.asInstanceOf[TemplateMapping]
      val ontologyClass = templateMapping.mapToClass
      val mappings = templateMapping.mappings
      val templates = getTemplates(mappings).toSeq

      val operator = conditionalMapping.operator
      val property = conditionalMapping.templateProperty
      val value = conditionalMapping.value

      val condition = operator match {
        case "isSet" => IsSetCondition(property)
        case "equals" => EqualsCondition(property, value)
        case "contains" => ContainsCondition(property, value)
        case "otherwise" => OtherwiseCondition()
      }

      ConditionalTemplate(condition, templates, ontologyClass, fallback)
    })

    conditionalTemplate
  }

  /**
    * Creates an EndDateTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createEndDateTemplate(templateFactoryBundle: TemplateFactoryBundle): EndDateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)
    val mapping = bundle.propertyMapping.asInstanceOf[DateIntervalMapping]

    val ontologyProperty = mapping.endDateOntologyProperty
    val property = mapping.templateProperty

    new EndDateTemplate(property, ontologyProperty)

  }

  /**
    * Creates a GeoCoordinateTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createGeocoordinateTemplate(templateFactoryBundle: TemplateFactoryBundle): GeocoordinateTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)
    val mapping = bundle.propertyMapping.asInstanceOf[GeoCoordinatesMapping]

    val ontologyProperty = mapping.ontologyProperty
    val coordinate = mapping.coordinates
    val latitude =  mapping.latitude
    val longitude = mapping.longitude
    val latitudeDegrees = mapping.latitudeDegrees
    val latitudeMinutes = mapping.latitudeMinutes
    val latitudeSeconds = mapping.latitudeSeconds
    val latitudeDirection = mapping.latitudeDirection
    val longitudeDegrees = mapping.latitudeDegrees
    val longitudeMinutes = mapping.longitudeMinutes
    val longitudeSeconds = mapping.longitudeSeconds
    val longitudeDirection = mapping.longitudeDirection

    new GeocoordinateTemplate(ontologyProperty, coordinate, latitude, longitude, latitudeDegrees, latitudeMinutes, latitudeSeconds,
      latitudeDirection, longitudeDegrees, longitudeMinutes, longitudeSeconds, longitudeDirection)

  }

  /**
    * Creates a SimplePropertyTemplate object from a TemplateFactoryBundle object
    *
    * @param templateFactoryBundle
    * @return
    */
  override def createSimplePropertyTemplate(templateFactoryBundle: TemplateFactoryBundle): SimplePropertyTemplate = {

    // get bundle
    val bundle = getBundle(templateFactoryBundle)
    val mapping = bundle.propertyMapping.asInstanceOf[SimplePropertyMapping]

    val property = mapping.templateProperty
    val ontologyProperty = mapping.ontologyProperty
    val suffix = mapping.suffix
    val select = mapping.select
    val prefix = mapping.prefix
    val transform = mapping.transform
    val factor = mapping.factor
    val unit = mapping.unit

    new SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, factor)
  }

  private def getBundle(bundle: TemplateFactoryBundle): WikiTextBundle = {
    if (!bundle.isInstanceOf[WikiTextBundle]) {
      throw new TemplateFactoryBundleException(TemplateFactoryBundleException.WRONG_BUNDLE_MSG)
    } else {
      bundle.asInstanceOf[WikiTextBundle]
    }
  }

  private def getTemplates(propertyMappings : List[PropertyMapping]) : List[Template] = {
    val templates = propertyMappings.flatMap {
      case simplePropertyMapping: SimplePropertyMapping => List(createSimplePropertyTemplate(WikiTextBundle(simplePropertyMapping)))
      case constantMapping: ConstantMapping => List(createConstantTemplate(WikiTextBundle(constantMapping)))
      case geocoordinateMapping: GeoCoordinatesMapping => List(createGeocoordinateTemplate(WikiTextBundle(geocoordinateMapping)))
      case dateIntervalMapping : DateIntervalMapping => List(createStartDateTemplate(WikiTextBundle(dateIntervalMapping)), createEndDateTemplate(WikiTextBundle(dateIntervalMapping)))
      case _ => List()
    }
    templates
  }

}
