package org.dbpedia.extraction.server.util

import java.util.logging.Logger
import io.Source
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.Serializable
import scala.collection
import scala.collection.mutable
import java.io._
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Dataset}
import org.dbpedia.extraction.server.util.CreateMappingStats._
import java.net.{URLDecoder, URLEncoder}
import org.dbpedia.extraction.util.StringUtils.prettyMillis

class MappingStatsManager(statsDir : File, language: Language)
extends MappingStatsConfig(statsDir, language)
{
    private val logger = Logger.getLogger(getClass.getName)

    val ignoreList = new IgnoreList(new File(statsDir, "ignorelist_"+language.wikiCode+".txt"))

    val percentageFile = new File(statsDir, "percentage." + language.wikiCode)

    val wikiStats = loadStats
    
    def countMappedStatistics(mappings: Map[String, ClassMapping], wikiStats: WikipediaStats) : Seq[MappingStats] =
    {
      var statistics = new mutable.ArrayBuffer[MappingStats]()

      for ((rawTemplate, templateStats) <- wikiStats.templates)
      {
        if (rawTemplate startsWith templateNamespacePrefix) {
          
          val templateName = rawTemplate.substring(templateNamespacePrefix.length)
          val isMapped = mappings.contains(templateName)
          val mappedProps = if (isMapped) new PropertyCollector(mappings(templateName)).properties else Set.empty[String]
          
          var properties = new mutable.HashMap[String, (Int, Boolean)]
          
          for ((name, count) <- templateStats.properties) {
            properties(name) = (count, mappedProps.contains(name))
          }
          
          for (name <- mappedProps) {
            if (! properties.contains(name)) properties(name) = (-1, true) // -1 means mapped in dbpedia but not found in wikipedia
          }
            
          statistics += new MappingStats(templateStats, templateName, isMapped, properties.toMap, ignoreList)
          
        } else {
          logger.warning(language.wikiCode+" template '"+rawTemplate+"' does not start with '"+templateNamespacePrefix+"'")
        }
      }

      statistics
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

class PropertyCollector(mapping: ClassMapping) {
  
  val properties = new mutable.HashSet[String]
  
  classMapping(mapping)
  
  private def classMapping(mapping: ClassMapping) : Unit = mapping match {
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
