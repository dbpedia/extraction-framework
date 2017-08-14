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

    val convertedTemplates = convertLatLonTemplates(templates.toSet)
    convertedTemplates
  }

  def analyze(pom : RMLPredicateObjectMap) : Template = {

    val uri = pom.resource.getURI
    uri match {

      case s : String if uri.contains(RMLUri.SIMPLEPROPERTYMAPPING) => analyzeTemplate(new SimplePropertyTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.CONSTANTMAPPING) => analyzeTemplate(new ConstantTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.STARTDATEMAPPING) => analyzeTemplate(new StartDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.ENDDATEMAPPING) => analyzeTemplate(new EndDateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.LATITUDEMAPPING) => analyzeTemplate(new LatitudeTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.LONGITUDEMAPPING) => analyzeTemplate(new LongitudeTemplateAnalyzer(ontology), pom)

      case s : String if uri.contains(RMLUri.INTERMEDIATEMAPPING) => analyzeTemplate(new IntermediateTemplateAnalyzer(ontology), pom)
      case s : String if uri.contains(RMLUri.CONDITIONALMAPPING) => analyzeTemplate(new ConditionalTemplateAnalyzer(ontology), pom)

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


}
