package org.dbpedia.extraction.mappings.rml.model.factory

import org.dbpedia.extraction.mappings.rml.model.template._

/**
  * Created by wmaroy on 24.07.17.
  */
trait TemplateFactory {

  /**
    * Creates a ConstantTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createConstantTemplate(templateFactoryBundle: TemplateFactoryBundle) : ConstantTemplate

  /**
    * Creates a SimplePropertyTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createSimplePropertyTemplate(templateFactoryBundle: TemplateFactoryBundle) : SimplePropertyTemplate

  /**
    * Creates a GeoCoordinateTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createGeocoordinateTemplate(templateFactoryBundle: TemplateFactoryBundle) : GeocoordinateTemplate

  /**
    * Creates an IntermediateTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createIntermediateTemplate(templateFactoryBundle: TemplateFactoryBundle) : IntermediateTemplate

  /**
    * Creates a StartDateTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createStartDateTemplate(templateFactoryBundle: TemplateFactoryBundle) : StartDateTemplate

  /**
    * Creates an EndDateTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createEndDateTemplate(templateFactoryBundle: TemplateFactoryBundle) : EndDateTemplate

  /**
    * Creates a ConditionalTemplate object from a TemplateFactoryBundle object
    * @param templateFactoryBundle
    * @return
    */
  def createConditionalTemplate(templateFactoryBundle: TemplateFactoryBundle) : ConditionalTemplate

}
