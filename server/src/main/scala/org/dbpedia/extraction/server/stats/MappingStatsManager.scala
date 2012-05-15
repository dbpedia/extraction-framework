package org.dbpedia.extraction.server.stats

import java.util.logging.Logger
import scala.io.Source
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.util.Language
import scala.collection.mutable
import java.io.File
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import MappingStats.InvalidTarget

class MappingStatsManager(statsDir : File, language: Language)
extends MappingStatsConfig(statsDir, language)
{
    private val logger = Logger.getLogger(getClass.getName)

    // TODO: call updateMappings when the ignoreList changes. 
    // Problem: we don't keep a reference to the mappings.
    val ignoreList = new IgnoreList(new File(statsDir, "ignorelist_"+language.wikiCode+".txt"))

    val percentageFile = new File(statsDir, "percentage." + language.wikiCode)

    val wikiStats = loadStats
    
    @volatile var holder: MappingStatsHolder = null
    
    def updateMappings(all: Mappings) = {
      
      val millis = System.currentTimeMillis
      logger.info("Updating "+language.wikiCode+" mapped statistics")
      
      val mappings = all.templateMappings
      
      var statistics = new mutable.ArrayBuffer[MappingStats]()

      for ((rawTemplate, templateStats) <- wikiStats.templates)
      {
        if (rawTemplate startsWith templateNamespace) {
          
          val templateName = rawTemplate.substring(templateNamespace.length)
          val isMapped = mappings.contains(templateName)
          val mappedProps = 
            if (isMapped) new PropertyCollector(mappings(templateName)).properties 
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
      
      val redirects = wikiStats.redirects.filterKeys(title => mappings.contains(title)).map(_.swap)
      
      // computation is done - set all values in one atomic action
      holder = new MappingStatsHolder(statistics.toList, redirects)
      
      logger.info("Updated "+language.wikiCode+" mapped statistics in "+prettyMillis(System.currentTimeMillis - millis))
    }

    private def loadStats(): WikipediaStats =
    {
        val millis = System.currentTimeMillis
        logger.info("Loading "+language.wikiCode+" wiki statistics from " + mappingStatsFile)
        val source = Source.fromFile(mappingStatsFile, "UTF-8")
        val wikiStats = try new WikipediaStatsReader(source.getLines).read() finally source.close
        logger.info("Loaded "+language.wikiCode+" wiki statistics from " + mappingStatsFile+" in "+prettyMillis(System.currentTimeMillis - millis))
        wikiStats
    }
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
