package org.dbpedia.extraction.server.stats

import java.util.logging.Logger
import org.dbpedia.extraction.mappings._
import scala.collection.mutable
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.Namespace
import MappingStats.InvalidTarget
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.wikiparser.TemplateNode

object MappingStatsHolder {
  
  private val logger = Logger.getLogger(getClass.getName)

  def apply(wikiStats: WikipediaStats, mappings: Mappings, ignoreList: IgnoreList): MappingStatsHolder = {
    
      val language = wikiStats.language
    
      val millis = System.currentTimeMillis
      logger.info("Updating "+language.wikiCode+" mapped statistics")
      
      val templateMappings = mappings.templateMappings
      
      var statistics = new mutable.ArrayBuffer[MappingStats]()

      val templateNamespace = Namespace.Template.getName(language) + ":"
      
      for ((rawTemplate, templateStats) <- wikiStats.templates)
      {
        if (rawTemplate startsWith templateNamespace) {
          
          val templateName = rawTemplate.substring(templateNamespace.length)
          val isMapped = templateMappings.contains(templateName)
          val mappedProps = 
            if (isMapped) new PropertyCollector(templateMappings(templateName)).properties 
            else Set.empty[String]
          
          var properties = new mutable.HashMap[String, (Int, Boolean)]
          
          for ((name, count) <- templateStats.properties) {
            properties(name) = (count, mappedProps.contains(name))
          }
          
          for (name <- mappedProps) {
            if (! properties.contains(name)) properties(name) = (InvalidTarget, true)
          }
            
          statistics += new MappingStats(templateStats, templateName, isMapped, properties.toMap, ignoreList)
          
        } else {
          logger.warning(language.wikiCode+" template '"+rawTemplate+"' does not start with '"+templateNamespace+"'")
        }
      }
      
      val redirects = wikiStats.redirects.filterKeys(title => templateMappings.contains(title)).map(_.swap)
      
      val holder = new MappingStatsHolder(mappings, statistics.toList, redirects, ignoreList)
      
      logger.info("Updated "+language.wikiCode+" mapped statistics in "+prettyMillis(System.currentTimeMillis - millis))
      
      holder
  }
  
}

/**
 * Contains statistics data computed from Wikipedia statistics numbers and template mappings.
 * Also holds on to the mappings to make synchronization in MappingStatsManager easier. 
 * TODO: better solution for mappings?
 */
class MappingStatsHolder(val mappings: Mappings, val mappedStatistics: List[MappingStats], val reversedRedirects: Map[String, String], ignoreList: IgnoreList) {

    private def countTemplates(all: Boolean, count: MappingStats => Int): Int = {
      var sum = 0
      for (ms <- mappedStatistics) {
        if (all || ms.isMapped) {
          if (! ignoreList.isTemplateIgnored(ms.templateName)) {
            sum += count(ms)
          }
        }
      }
      sum
    }

    private def countAllTemplates(count: MappingStats => Int): Int = countTemplates(true, count)
    private def countMappedTemplates(count: MappingStats => Int): Int = countTemplates(false, count)
      
    val templateCount = countAllTemplates(_ => 1)
    val mappedTemplateCount = countMappedTemplates(_ => 1)

    val templateUseCount = countAllTemplates(_.templateCount)
    val mappedTemplateUseCount = countMappedTemplates(_.templateCount)

    val propertyCount = countAllTemplates(_.propertyCount)
    val mappedPropertyCount = countMappedTemplates(_.mappedPropertyCount)
    
    val propertyUseCount = countAllTemplates(_.propertyUseCount)
    val mappedPropertyUseCount = countMappedTemplates(_.mappedPropertyUseCount)
    
    val mappedTemplateRatio = mappedTemplateCount.toDouble / templateCount.toDouble
    val mappedPropertyRatio = mappedPropertyCount.toDouble / propertyCount.toDouble
    
    val mappedTemplateUseRatio = mappedTemplateUseCount.toDouble / templateUseCount.toDouble
    val mappedPropertyUseRatio = mappedPropertyUseCount.toDouble / propertyUseCount.toDouble
}

class PropertyCollector(mapping: Mapping[TemplateNode]) {
  
  val properties = new mutable.HashSet[String]
  
  classMapping(mapping) // go get'em!
  
  private def classMapping(mapping: Mapping[TemplateNode]) : Unit = mapping match {
    case tm: TemplateMapping => tm.mappings.foreach(propertyMapping)
    case cm: ConditionalMapping =>
      cm.cases.foreach(conditionMapping)
      cm.defaultMappings.foreach(propertyMapping)
  }
  
  private def conditionMapping(mapping: ConditionMapping) : Unit = 
    classMapping(mapping.mapping)
  
  private def propertyMapping(mapping: PropertyMapping) : Unit = mapping match {
    case m: SimplePropertyMapping => this + m.templateProperty
    case m: GeoCoordinatesMapping => this + m.coordinates + m.latitude + m.longitude + m.longitudeDegrees + m.longitudeMinutes + m.longitudeSeconds + m.longitudeDirection + m.latitudeDegrees + m.latitudeMinutes + m.latitudeSeconds + m.latitudeDirection
    case m: CalculateMapping => this + m.templateProperty1 + m.templateProperty2
    case m: CombineDateMapping => this + m.templateProperty1 + m.templateProperty2 + m.templateProperty3
    case m: DateIntervalMapping => this + m.templateProperty
    case m: IntermediateNodeMapping => m.mappings.foreach(propertyMapping)
    case m: ConstantMapping => // ignore
  }
  
  private def +(name: String) : PropertyCollector = {
    if (name != null) properties.add(name)
    this
  }
}
