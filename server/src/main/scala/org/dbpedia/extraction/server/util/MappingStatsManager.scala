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
import org.dbpedia.extraction.server.util.StringUtils.prettyMillis

class MappingStatsManager(statsDir : File, language: Language)
extends MappingStatsConfig(statsDir, language)
{
    private val logger = Logger.getLogger(getClass.getName)

    private val ignoreListFile = new File(statsDir, "ignoreList_" + language.wikiCode + ".obj")
    private val ignoreListTemplatesFile = new File(statsDir, "ignoreListTemplates_" + language.wikiCode + ".txt")
    private val ignoreListPropertiesFile = new File(statsDir, "ignoreListProperties_" + language.wikiCode + ".txt")

    val percentageFile = new File(statsDir, "percentage." + language.wikiCode)

    val wikiStats = loadStats
    
    def countMappedStatistics(mappings: Map[String, ClassMapping], wikipediaStatistics: WikipediaStats) : mutable.Set[MappingStats] =
    {
        logger.info("count mapped statistics "+language.wikiCode+" start")
        
        val millis = System.currentTimeMillis()

        // Hold the overall statistics
        var statistics = mutable.Set[MappingStats]()

        var isMapped: Boolean = false

        for ((rawTemplate, templateStats) <- wikipediaStatistics.templates)
        {
            if (rawTemplate startsWith templateNamespacePrefix) {
              val templateName = rawTemplate.substring(templateNamespacePrefix.length)
              
              isMapped = mappings.contains(templateName)
              var mappingStats = new MappingStats(templateStats, templateName)
              mappingStats.setTemplateMapped(isMapped)
              
              if (isMapped)
              {
                val mappedProperties = collectProperties(mappings(templateName))
                for (propName <- mappedProperties)
                {
                  mappingStats.setPropertyMapped(propName, true)
                }
              }
              statistics += mappingStats
            } else {
              logger.warning("unknown "+language.wikiCode+" template '"+rawTemplate+"'")
            }
        }

        logger.info("count mapped statistics "+language.wikiCode+" done - " + prettyMillis(System.currentTimeMillis - millis))
        
        statistics
    }
    
    def loadIgnorelist() =
    {
        if (ignoreListFile.isFile)
        {
            logger.fine("Loading serialized object from " + ignoreListFile)
            val input = new ObjectInputStream(new FileInputStream(ignoreListFile))
            val m = try input.readObject() finally input.close()
            m.asInstanceOf[IgnoreList]
        }
        else
        {
            new IgnoreList(language)
        }
    }

    def saveIgnorelist(ignoreList: IgnoreList)
    {
        val output = new ObjectOutputStream(new FileOutputStream(ignoreListFile))
        try output.writeObject(ignoreList) finally output.close()
        ignoreList.exportToTextFile(ignoreListTemplatesFile, ignoreListPropertiesFile)
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

    private def collectProperties(mapping: Object): Set[String] =
    {
        val properties = mapping match
        {
            case templateMapping: TemplateMapping => templateMapping.mappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
            case conditionalMapping: ConditionalMapping => conditionalMapping.defaultMappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
            case _ => Set[String]()
        }

        properties.filter(_ != null)
    }

    private def collectPropertiesFromPropertyMapping(propertyMapping: PropertyMapping): Set[String] = propertyMapping match
    {
        case simple: SimplePropertyMapping => Set(simple.templateProperty)
        case coord: GeoCoordinatesMapping => Set(coord.coordinates, coord.latitude, coord.longitude, coord.longitudeDegrees,
            coord.longitudeMinutes, coord.longitudeSeconds, coord.longitudeDirection,
            coord.latitudeDegrees, coord.latitudeMinutes, coord.latitudeSeconds, coord.latitudeDirection)
        case calc: CalculateMapping => Set(calc.templateProperty1, calc.templateProperty2)
        case combine: CombineDateMapping => Set(combine.templateProperty1, combine.templateProperty2, combine.templateProperty3)
        case interval: DateIntervalMapping => Set(interval.templateProperty)
        case intermediateNodeMapping: IntermediateNodeMapping => intermediateNodeMapping.mappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
        case _ => Set()
    }

}
