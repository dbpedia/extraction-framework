package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.RMLPredicateObjectMap
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
                     intermediates : Int = 0,
                     simpleProperties : Int = 0,
                     geoCoordinates : Int = 0,
                     constants : Int = 0,
                     startDates : Int = 0,
                     endDates : Int = 0) {


    /**
      * creates a new Counter object with updated values, parameters that are not given are standard set to current values
      */
    def update(conditionals : Int = conditionals, subConditions : Int = subConditions, intermediates : Int = intermediates,
               simpleProperties : Int = simpleProperties, geoCoordinates : Int = geoCoordinates,
               constants :Int = constants, startDates : Int = startDates, endDates : Int = endDates) : Counter = {
      Counter(conditionals, subConditions, intermediates, simpleProperties, geoCoordinates, constants, startDates, endDates)
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Assemble a template
    * Add a template to an existing rml model
    *
    * @param rmlModel
    * @param template
    * @param language
    * @param counter Object that counts templates for generating numbers in the template uri's
    * @return
    */
  def assembleTemplate(rmlModel : RMLModel, baseUri : String, template : Template, language : String, counter : Counter, independent : Boolean = false) : (Counter, List[RMLPredicateObjectMap]) = {

    template.name match {

      case SimplePropertyTemplate.NAME => {
        val templateInstance = template.asInstanceOf[SimplePropertyTemplate]
        val pomList = assembleSimplePropertyTemplate(rmlModel, templateInstance, baseUri, language, counter, independent)
        val updatedCounter = counter.update(simpleProperties = counter.simpleProperties + 1)
        (updatedCounter, pomList)
      }

      case ConstantTemplate.NAME => {
        val templateInstance = template.asInstanceOf[ConstantTemplate]
        val pomList = assembleConstantTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(constants = counter.constants + 1)
        (updatedCounter, pomList)
      }

      case GeocoordinateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[GeocoordinateTemplate]
        val pomList = assembleGeocoordinateTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(geoCoordinates = counter.geoCoordinates + 1)
        (updatedCounter, pomList)
      }

      case StartDateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[StartDateTemplate]
        val pomList = assembleStartDateTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(startDates = counter.startDates + 1)
        (updatedCounter, pomList)
      }

      case EndDateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[EndDateTemplate]
        val pomList = assembleEndDateTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(endDates = counter.endDates + 1)
        (updatedCounter, pomList)
      }

      case ConditionalTemplate.NAME => {
       val templateInstance = template.asInstanceOf[ConditionalTemplate]
        val pomList = assembleConditionalTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(conditionals = counter.conditionals + 1)
        (updatedCounter, pomList)
      }

      case IntermediateTemplate.NAME => {
        val templateInstance = template.asInstanceOf[IntermediateTemplate]
        val pomList = assembleIntermediateTemplate(rmlModel, templateInstance, baseUri, language, counter)
        val updatedCounter = counter.update(intermediates = counter.intermediates + 1)
        (updatedCounter, pomList)
      }

      case _ => throw new IllegalArgumentException("Unsupported template: " + template.name)
    }

  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    * Assembles a template with standard as baseUri the main triples map uri
    *
    * @param rmlModel
    * @param template
    * @param language
    * @param counter
    * @return
    */
  def assembleTemplate(rmlModel : RMLModel, template : Template, language : String, counter : Counter) : (Counter, List[RMLPredicateObjectMap]) = {
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
  private def assembleSimplePropertyTemplate(rmlModel : RMLModel, simplePropertyTemplate: SimplePropertyTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
    if(simplePropertyTemplate.isSimple) {
      val assembler = new ShortSimplePropertyTemplateAssembler(rmlModel, baseUri, language, simplePropertyTemplate, counter)
      assembler.assemble(independent)
    } else {
      val assembler = new SimplePropertyTemplateAssembler(rmlModel, baseUri, language, simplePropertyTemplate, counter)
      assembler.assemble(independent)
    }
  }

  /**
    *
    * @param rmlModel
    * @param constantTemplate
    * @param language
    * @param counter
    * @return
    */
  private def assembleConstantTemplate(rmlModel: RMLModel, constantTemplate : ConstantTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
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
  private def assembleStartDateTemplate(rmlModel: RMLModel, startDateTemplate : StartDateTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
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
  private def assembleEndDateTemplate(rmlModel: RMLModel, endDateTemplate : EndDateTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
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
  private def assembleGeocoordinateTemplate(rmlModel: RMLModel, geocoordinateTemplate : GeocoordinateTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
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
  private def assembleConditionalTemplate(rMLModel: RMLModel, conditionalTemplate: ConditionalTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
    val assembler = new ConditionalTemplateAssembler(rMLModel, baseUri, conditionalTemplate, language, counter)
    assembler.assemble()
  }

  /**
    *
    * @param rMLModel
    * @param template
    * @param language
    * @param counter
    */
  private def assembleIntermediateTemplate(rMLModel: RMLModel, template: IntermediateTemplate, baseUri: String, language: String, counter : Counter, independent : Boolean = false) : List[RMLPredicateObjectMap] = {
    val assembler = new IntermediateTemplateAssembler(rMLModel, baseUri, language, template,  counter)
    assembler.assemble()
  }

}
