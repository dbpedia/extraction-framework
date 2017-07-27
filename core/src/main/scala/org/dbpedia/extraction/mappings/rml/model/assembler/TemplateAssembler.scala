package org.dbpedia.extraction.mappings.rml.model.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.util.Language

/**
  * Created by wmaroy on 24.07.17.
  * This object adds mapping templates from
  * the rml.model.template package to an RMLTriplesMap from the rml.model.resource package
  */
object TemplateAssembler {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Case classes
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Keeps track of the count of templates (for nesting, generating meaningful uri's)
    */
  case class Counter(conditionals : Int = 0,
                     subConditions : Int = 0,
                     simpleProperties : Int = 0,
                     geoCoordinates : Int = 0,
                     constants : Int = 0,
                     startDates : Int = 0,
                     endDates : Int = 0) {


    /**
      * creates a new Counter object with updated values, parameters that are not given are standard set to current values
      */
    def update(conditionals : Int = conditionals, subConditions : Int = subConditions,
               simpleProperties : Int = simpleProperties, geoCoordinates : Int = geoCoordinates,
               constants :Int = constants, startDates : Int = startDates, endDates : Int = endDates) : Counter = {
      Counter(conditionals, subConditions, simpleProperties, geoCoordinates, constants, startDates, endDates)
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Assemble a template
    * Add a template to an existing rml model
    * @param rmlModel
    * @param template
    * @param language
    * @param counter Object that counts templates for generating numbers in the template uri's
    * @return
    */
  def assembleTemplate(rmlModel : RMLModel, baseUri : String, template : Template, language : String, counter : Counter) : Counter = {

    template.name match {

      case SimplePropertyTemplate.NAME => {
        val templateInstance = template.asInstanceOf[SimplePropertyTemplate]
        assembleSimplePropertyTemplate(rmlModel, templateInstance, baseUri, language, counter.simpleProperties)
        counter.update(simpleProperties = counter.simpleProperties + 1)
      }

      case ConstantTemplate.NAME => {
        val templateInstance = template.asInstanceOf[ConstantTemplate]
        assembleConstantTemplate(rmlModel, templateInstance, baseUri, language, counter.constants)
        counter.update(constants = counter.constants + 1)
      }

      case GeocoordinateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[GeocoordinateTemplate]
        assembleGeocoordinateTemplate(rmlModel, templateInstance, baseUri, language, counter.geoCoordinates)
        counter.update(geoCoordinates = counter.geoCoordinates + 1)
      }

      case StartDateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[StartDateTemplate]
        assembleStartDateTemplate(rmlModel, templateInstance, baseUri, language, counter.startDates)
        counter.update(startDates = counter.startDates + 1)
      }

      case EndDateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[EndDateTemplate]
        assembleEndDateTemplate(rmlModel, templateInstance, baseUri, language, counter.endDates)
        counter.update(endDates = counter.endDates + 1)
      }

      case ConditionalTemplate.NAME => {
       val templateInstance = template.asInstanceOf[ConditionalTemplate]
        assembleConditionalTemplate(rmlModel, templateInstance, baseUri, language, counter.conditionals)
        counter.update(conditionals = counter.conditionals + 1)
      }

      case _ => throw new IllegalArgumentException("Unsupported template: " + template.name)
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Assembles a template with standard as baseUri the main triples map uri
    * @param rmlModel
    * @param template
    * @param language
    * @param counter
    * @return
    */
  def assembleTemplate(rmlModel : RMLModel, template : Template, language : String, counter : Counter) : Counter = {
    val baseUri = rmlModel.triplesMap.resource.getURI
    assembleTemplate(rmlModel, baseUri, template, language, counter)
  }

  /**
    *
    * @param rmlModel
    * @param simplePropertyTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleSimplePropertyTemplate(rmlModel : RMLModel, simplePropertyTemplate: SimplePropertyTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new SimplePropertyTemplateAssembler(rmlModel, baseUri, language, simplePropertyTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rmlModel
    * @param constantTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleConstantTemplate(rmlModel: RMLModel, constantTemplate : ConstantTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new ConstantTemplateAssembler(rmlModel, baseUri, language, constantTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rmlModel
    * @param startDateTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleStartDateTemplate(rmlModel: RMLModel, startDateTemplate : StartDateTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new StartDateTemplateAssembler(rmlModel, baseUri,language, startDateTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rmlModel
    * @param endDateTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleEndDateTemplate(rmlModel: RMLModel, endDateTemplate : EndDateTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new EndDateTemplateAssembler(rmlModel, baseUri, language, endDateTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rmlModel
    * @param geocoordinateTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleGeocoordinateTemplate(rmlModel: RMLModel, geocoordinateTemplate : GeocoordinateTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new GeocoordinateTemplateAssembler(rmlModel, baseUri, language, geocoordinateTemplate, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rMLModel
    * @param conditionalTemplate
    * @param language
    * @param counter
    */
  private def assembleConditionalTemplate(rMLModel: RMLModel, conditionalTemplate: ConditionalTemplate, baseUri: String, language: String, counter : Int) = {
    val assembler = new ConditionalTemplateAssembler(rMLModel, baseUri, conditionalTemplate, language, counter)
    assembler.assemble()
  }

}
