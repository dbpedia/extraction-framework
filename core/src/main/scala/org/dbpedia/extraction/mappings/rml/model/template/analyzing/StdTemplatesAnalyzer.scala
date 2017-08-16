package org.dbpedia.extraction.mappings.rml.model.template.analyzing
import java.util.logging.Logger

import org.dbpedia.extraction.mappings.rml.model.resource.{RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 11.08.17.
  */
class StdTemplatesAnalyzer(ontology: Ontology, ignoreURIPart : String = null) extends TemplatesAnalyzer {

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

    val convertedTemplates = convertLatLonTemplates(templates.toSet)
    val convertedTemplates2 = convertConditionals(convertedTemplates)
    convertedTemplates2
  }

  def analyze(pom : RMLPredicateObjectMap) : Template = {

    val uri = if(ignoreURIPart == null) pom.resource.getURI else pom.resource.getURI.replace(ignoreURIPart, "")
    uri match {

      // check for these first!
      case s : String if uri.contains(RMLUri.CONDITIONALMAPPING) => analyzeTemplate(new ConditionalTemplateAnalyzer(ontology), pom)

      case s : String if uri.contains(RMLUri.SIMPLEPROPERTYMAPPING) => analyzeTemplate(new SimplePropertyTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.CONSTANTMAPPING) => analyzeTemplate(new ConstantTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.STARTDATEMAPPING) => analyzeTemplate(new StartDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.ENDDATEMAPPING) => analyzeTemplate(new EndDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.LATITUDEMAPPING) => analyzeTemplate(new LatitudeTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.LONGITUDEMAPPING) => analyzeTemplate(new LongitudeTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.INTERMEDIATEMAPPING) => analyzeTemplate(new IntermediateTemplateAnalyzer(ontology), pom)

      case _ => logger.info(uri + " contains no known/supported template.") ; null

    }

  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def analyzeTemplate(analyzer : TemplateAnalyzer, pom : RMLPredicateObjectMap) : Template = {
    analyzer(pom)
  }

  /**
    * Search for Lon/Lat templates in a set of Templates and converts them to GeoTemplates
    * Return a new set of Templates without the Lat/Lon templates, but with the GeoTemplate
    *
    * @param templates Set of templates
    * @return
    */
  private def convertLatLonTemplates(templates : Set[Template]) : Set[Template] = {

    case class LatLonGroup(lat : LatitudeTemplate = null, lon : LongitudeTemplate = null) {
      def isNonEmpty : Boolean = {
        lat != null && lon != null
      }
    }

    // there are three different types of LatLon groups
    var type_1 = LatLonGroup()
    var type_2 = LatLonGroup()
    var type_3 = LatLonGroup()

    // iterate over the templates and put Lat/Lon templates in their groups
    templates.foreach {
      case tlon: LongitudeTemplate => tlon.kind match {
        case LongitudeTemplate.TYPE_1 => type_1 = LatLonGroup(type_1.lat, tlon)
        case LongitudeTemplate.TYPE_2 => type_2 = LatLonGroup(type_2.lat, tlon)
        case LongitudeTemplate.TYPE_3 => type_3 = LatLonGroup(type_3.lat, tlon)
      }
      case tlat: LatitudeTemplate => tlat.kind match {
        case LongitudeTemplate.TYPE_1 => type_1 = LatLonGroup(tlat, type_1.lon)
        case LongitudeTemplate.TYPE_2 => type_2 = LatLonGroup(tlat, type_1.lon)
        case LongitudeTemplate.TYPE_3 => type_3 = LatLonGroup(tlat, type_1.lon)
      }
      case _ =>
    }

    // update the set: remove for each valid group the original Lat/Lon templates and add the GeoTemplate
    val updatedSet = Seq(type_1, type_2, type_3).foldLeft(templates)((templates, group) => {
      if(group.isNonEmpty) {
        val geoTemplate = GeocoordinateTemplate(group.lat, group.lon)
        templates - (group.lat, group.lon) + geoTemplate
      } else templates
    })

    updatedSet

  }

  private def convertConditionals(templates : Set[Template]) : Set[Template] = {

    // filter out conditional templates
    val conditionalTemplates = templates.filter(template => template.isInstanceOf[ConditionalTemplate])
                                        .map(template => template.asInstanceOf[ConditionalTemplate])


    // group by their condition
    val groupedTemplates = conditionalTemplates.groupBy(template => template.condition)

    // for every group create the main Conditional Template
    val set = groupedTemplates.flatMap(entry => {

      val conditionalTemplates = entry._2

      var _classMapping : ConditionalTemplate = null
      var _templates = List[ConditionalTemplate]()

      // extract the conditional with classmappings and the others
      conditionalTemplates.foreach {
        case template: ConditionalTemplate => {
          if (template.ontologyClass != null) _classMapping = template else _templates = _templates :+ template
        }
        case _ =>
      }

      // make a distinction between the two cases, class mapping or not
      if(_classMapping != null) {

        // it is assumed that these will only contain a single template
        val subTemplates = _templates.map(template => template.templates.head)

        val updatedClassMapping = ConditionalTemplate(_classMapping.condition, subTemplates,
          _classMapping.ontologyClass, _classMapping.fallback)

        val returnSet : Set[Template] = templates - _classMapping -- _templates.toSet + updatedClassMapping
        returnSet

      } else {

        // get the template with the fallback, if there is none just take the first
        val templateWithFallback = _templates.find(template => template.hasFallback).getOrElse(templates.head).asInstanceOf[ConditionalTemplate]
        val templateWithFallbackSubTemplate = templateWithFallback.templates.head

        val conditionalTemplatesWithoutFallback = _templates.toSet - templateWithFallback
        val templatesWithoutFallback = conditionalTemplatesWithoutFallback.map(template => {
          // it is assumed that these will only contain a single template
          template.asInstanceOf[ConditionalTemplate].templates.head
        }).toSeq

        val updatedTemplateWithFallback = ConditionalTemplate(templateWithFallback.condition, templatesWithoutFallback :+ templateWithFallbackSubTemplate , null, templateWithFallback.fallback)

        val returnSet : Set[Template] = templates - templateWithFallback -- conditionalTemplatesWithoutFallback + updatedTemplateWithFallback
        returnSet
      }


    }).toSet

    // quick check fix
    if(set.isEmpty) templates else set

  }


}
