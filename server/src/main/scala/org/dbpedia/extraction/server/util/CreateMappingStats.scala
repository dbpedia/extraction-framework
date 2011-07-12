package org.dbpedia.extraction.server.util

import collection.immutable.ListMap
import io.Source
import java.lang.IllegalArgumentException
import java.io._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser.WikiTitle
import java.net.URL
import org.dbpedia.extraction.sources.{MemorySource, WikiSource}
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import com.sun.xml.internal.bind.v2.runtime.property.Property
import tools.nsc.doc.model.comment.Title
import java.util.logging.Filter


/**
 * Script to gather statistics about mappings: how often they are used, which properties are used and for what mappings exist.
 */
object CreateMappingStats
{

    // Hold template statistics
    @serializable
    class TemplateStats(var templateCount: Int = 0, var properties: Map[String, Int] = Map())
    {
        override def toString = "TemplateStats[count:" + templateCount + ",properties:" + properties.mkString(",") + "]"
    }

    /**
     * Contains the statistic of a Template
     */
    @serializable
    class MappingStats(templateStats: TemplateStats, val templateName: String) extends Ordered[MappingStats]
    {
        var templateCount = templateStats.templateCount
        var isMapped: Boolean = false
        var properties: Map[String, (Int, Boolean)] = templateStats.properties.mapValues
                {
                    freq => (freq, false)
                }.toMap

        def setTemplateMapped(mapped: Boolean)
        {
            isMapped = mapped
        }

        def setPropertyMapped(propertyName: String, mapped: Boolean)
        {
            val (freq, _) = properties.getOrElse(propertyName, (-1, false)) // -1 mapped but not allowed in the template
            properties = properties.updated(propertyName, (freq, mapped))
        }

        def getNumberOfProperties() =
        {
            templateStats.properties.size
        }

        def getNumberOfMappedProperties() =
        {
            var numMPs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propIsMapped && propCount != -1) numMPs = numMPs + 1
            }
            numMPs
        }

        def getRatioOfMappedProperties() =
        {
            var mappedRatio: Double = 0
            mappedRatio = getNumberOfMappedProperties().toDouble / getNumberOfProperties().toDouble
            mappedRatio
        }

        def getNumberOfPropertyOccurrences() =
        {
            var numPOs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propCount != -1) numPOs = numPOs + propCount
            }
            numPOs
        }

        def getNumberOfMappedPropertyOccurrences() =
        {
            var numMPOs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propIsMapped && propCount != -1) numMPOs = numMPOs + propCount
            }
            numMPOs
        }

        def getRatioOfMappedPropertyOccurrences() =
        {
            var mappedRatio: Double = 0
            mappedRatio = getNumberOfMappedPropertyOccurrences().toDouble / getNumberOfPropertyOccurrences().toDouble
            mappedRatio
        }

        def compare(that: MappingStats) =
            this.templateCount - that.templateCount
    }

    val lang = "en"
    private val language = Language.fromWikiCode(lang).get

    private val templateNamespacePrefix = Namespaces.getNameForNamespace(language, WikiTitle.Namespace.Template) + "%3A"
    private val resourceNamespacePrefix = if (lang == "en") "http://dbpedia.org/resource/" else "http://" + lang + "dbpedia.org/resource/"

    private val ObjectPropertyTripleRegex = """<([^>]+)> <([^>]+)> <([^>]+)> .""".r
    private val DatatypePropertyTripleRegex = """<([^>]+)> <([^>]+)> "(.+?)"@?\w?\w? .""".r
    val serializeFileName = "C:\\Users\\Paul\\Documents\\mapping_stats_datasets\\serialized.obj"
    val ignoreListFileName = "C:\\Users\\Paul\\Documents\\mapping_stats_datasets\\ignoreList.obj"
    val ignoreListTemplatesFileName = "C:\\Users\\Paul\\Documents\\mapping_stats_datasets\\ignoreListTemplates.txt"
    val ignoreListPropertiesFileName = "C:\\Users\\Paul\\Documents\\mapping_stats_datasets\\ignoreListProperties.txt"


    // Hold template redirects and template statistics
    @serializable
    class WikipediaStats(var redirects: Map[String, String] = Map(), var templates: Map[String, TemplateStats] = Map())
    {

        def checkForRedirects(mappingStats: Map[MappingStats, Int], mappings: Map[String, ClassMapping], lang: Language) =
        {
            val mappedRedirrects = redirects.filterKeys(title => mappings.contains(WikiUtil.wikiDecode(title, lang).substring(9)))
            mappedRedirrects.map(_.swap)
        }
    }

    def main(args: Array[String])
    {
        val serializeFileName = args(0)
        val redirectsDatasetFileName = args(1)
        val infoboxPropertiesDatasetFileName = args(2)
        val templateParametersDatasetFileName = args(3)
        val infoboxTestDatasetFileName = args(4)
        var wikiStats: WikipediaStats = null
        val startTime = System.currentTimeMillis()

        if (new File(serializeFileName).isFile)
        {
            println("Loading serialized object from " + serializeFileName)
            wikiStats = deserialize(serializeFileName)
        }
        else
        {
            wikiStats = getWikipediaStats(redirectsDatasetFileName, infoboxPropertiesDatasetFileName, templateParametersDatasetFileName, infoboxTestDatasetFileName)
            println("Serializing to " + serializeFileName)
            serialize(serializeFileName, wikiStats)
        }
        println((System.currentTimeMillis() - startTime) / 1000 + " s")
    }

    def countMappedStatistics(mappings: Map[String, ClassMapping], wikipediaStatistics: WikipediaStats) =
    {
        val startTime = System.currentTimeMillis()


        // Hold the overall statistics
        @serializable
        var statistics: Set[MappingStats] = Set()

        //Thread.sleep(500)

        //    println("number of templates " + wikiStats.templates.size)

        var isMapped: Boolean = false

        for ((encodedTemplateName, templateStats) <- wikipediaStatistics.templates)
        {
            val templateName: String = WikiUtil.wikiDecode(encodedTemplateName).substring(9)
            isMapped = checkMapping(templateName, mappings)
            var mappingStats: MappingStats = new MappingStats(templateStats, templateName)
            mappingStats.setTemplateMapped(isMapped)

            if (isMapped)
            {
                val mappedProperties = collectProperties(mappings.get(templateName).get)
                for (propName <- mappedProperties)
                {
                    mappingStats.setPropertyMapped(propName, true)
                }
            }
            statistics += mappingStats
        }

        var statsMap: Map[MappingStats, Int] = Map()
        for (mappingStat <- statistics)
        {
            statsMap += ((mappingStat, mappingStat.templateCount))
        }
        val sortedStatsMap = ListMap(statsMap.toList.sortBy
        {
            case (key, value) => (-value, key)
        }: _*)

        val out = new java.io.FileWriter("C:/Users/Paul/Documents/mapping_stats_datasets/stats.txt")
        //for (tempStat <- statistics)
        for ((mappinsStat, counter) <- sortedStatsMap)
        {
            out.write(mappinsStat.templateName + ";" + counter + ";" + mappinsStat.isMapped)


            //out.write(tempStat.templateName + ";" + tempStat.templateCount + ";" + tempStat.isMapped)
            /**for ((prop, (count, isMapped)) <- tempStat.properties)
            {
                out.write(";" + prop + ";" + count + ";" + isMapped)
            }*/
            out.write("\n")
        }
        out.close
        println((System.currentTimeMillis() - startTime) / 1000 + " s")
        //serializeStats("C:/Users/Paul/Documents/mapping_stats_datasets/serializedStats.obj", statistics)
        statistics
    }

    //TODO everything below

    // load data from mappings wiki


    // compare the result map to the output from the mappings wiki

    // - mapped infobox name is not found in Wikipedia stats (name has changed)
    //     if is redirect now -> schedule for infobox mapping renaming
    //     else -> schedule for investigation

    // - template is not mapped on the wiki
    //     schedule for infobox mapping (sorted by templateCount)

    // - missing property mappings
    //     schedule for property mapping (sorted by propertyCount)

    // - property is mapped that is not found with a template in Wikipedia stats !TemplateStats.properties.contains(mappedProperty)
    //     schedule for property deletion


    // produce and write out a table


    def loadIgnorelist() =
    {
        if (new File(ignoreListFileName).isFile)
        {
            println("Loading serialized object from " + ignoreListFileName)
            val input = new ObjectInputStream(new FileInputStream(ignoreListFileName))
            val m = input.readObject()
            input.close()
            m.asInstanceOf[IgnoreList]
        }
        else
        {
            val ign: IgnoreList = new IgnoreList()
            ign
        }

    }

    def saveIgnorelist(ignoreList: IgnoreList)
    {
        val output = new ObjectOutputStream(new FileOutputStream(ignoreListFileName))
        output.writeObject(ignoreList)
        output.close()
        ignoreList.exportToTextFile()
    }

    private def checkMapping(template: String, mappings: Map[String, ClassMapping]) =
    {
        if (mappings.contains(template)) true
        else false
    }

    private def checkProperty(propName: String, wikiProperties: Set[String]) =
    {
        if (wikiProperties.contains(propName)) true
        else false
    }

    private def collectProperties(mapping: Object): Set[String] =
    {
        val properties = mapping match
        {
            case classMapping: ClassMapping => collectPropertiesFromClassMapping(classMapping)
            case propertyMapping: PropertyMapping => collectPropertiesFromIntermediate(propertyMapping)
            case _ => Set[String]()
        }

        properties.filter(_ != null)
    }

    private def collectPropertiesFromClassMapping(classMapping: ClassMapping): Set[String] = classMapping match
    {
        case templateMapping: TemplateMapping => templateMapping.mappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
        case conditionalMapping: ConditionalMapping => conditionalMapping.defaultMappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
        case _ => Set()
    }

    private def collectPropertiesFromIntermediate(propertyMapping: PropertyMapping): Set[String] = propertyMapping match
    {
        case intermediateNodeMapping: IntermediateNodeMapping => intermediateNodeMapping.mappings.toSet.flatMap(collectPropertiesFromPropertyMapping)
        case _ => Set()
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
        case _ => Set()
    }

    private def getWikipediaStats(redirectsFile: String, infoboxPropsFile: String, templParsFile: String, parsUsageFile: String): WikipediaStats =
    {
        var templatesMap: Map[String, TemplateStats] = Map() // "templateName" -> TemplateStats

        println("Reading redirects from " + redirectsFile)
        val redirects: Map[String, String] = loadTemplateRedirects(redirectsFile)
        println("  " + redirects.size + " redirects")

        println("Using Template namespace prefix " + templateNamespacePrefix + " for language " + lang)
        println("Counting templates in " + infoboxPropsFile)
        templatesMap = countTemplates(infoboxPropsFile, templatesMap, redirects)
        println("  " + templatesMap.size + " different templates")

        println("Loading property definitions from " + templParsFile)
        templatesMap = propertyDefinitions(templParsFile, templatesMap, redirects)

        println("Counting properties in " + parsUsageFile)
        templatesMap = countProperties(parsUsageFile, templatesMap, redirects)

        new WikipediaStats(redirects, templatesMap)
    }

    private def stripUri(fullUri: String): String =
    {
        fullUri.replace(resourceNamespacePrefix, "")
    }

    private def loadTemplateRedirects(fileName: String): Map[String, String] =
    {
        var redirects: Map[String, String] = Map()
        for (line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case ObjectPropertyTripleRegex(subj, pred, obj) =>
                {
                    val templateName = stripUri(subj)
                    if (templateName startsWith templateNamespacePrefix)
                    {
                        redirects = redirects.updated(templateName, stripUri(obj))
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match redirects syntax: " + line)
                case _ =>
            }
        }

        // resolve transitive closure
        for ((source, target) <- redirects)
        {
            var cyclePrevention: Set[String] = Set()
            var closure = target
            while (redirects.contains(closure) && !cyclePrevention.contains(closure))
            {
                closure = redirects.get(closure).get
                cyclePrevention += closure
            }
            redirects = redirects.updated(source, closure)
        }

        redirects
    }

    private def countTemplates(fileName: String, resultMap: Map[String, TemplateStats], redirects: Map[String, String]): Map[String, TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through infobox properties
        for (line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                // if there is a wikiPageUsesTemplate relation
                case ObjectPropertyTripleRegex(subj, pred, obj) if pred contains "wikiPageUsesTemplate" =>
                {
                    // resolve redirect for *object*
                    val templateName = redirects.get(stripUri(obj)).getOrElse(stripUri(obj))

                    // lookup the *object* in the resultMap;   create a new TemplateStats object if not found
                    val stats = newResultMap.get(templateName).getOrElse(new TemplateStats)

                    // increment templateCount
                    stats.templateCount += 1

                    newResultMap = newResultMap.updated(templateName, stats)
                }
                case _ =>
            }
        }
        newResultMap
    }

    private def propertyDefinitions(fileName: String, resultMap: Map[String, TemplateStats], redirects: Map[String, String]): Map[String, TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through template parameters
        for (line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case DatatypePropertyTripleRegex(subj, pred, obj) =>
                {
                    // resolve redirect for *subject*
                    val templateName = redirects.get(stripUri(subj)).getOrElse(stripUri(subj))

                    // lookup the *subject* in the resultMap
                    newResultMap.get(templateName) match
                    {
                        case Some(stats: TemplateStats) =>
                        {
                            // add object to properties map with count 0
                            stats.properties = stats.properties.updated(stripUri(obj), 0)
                            newResultMap = newResultMap.updated(templateName, stats)
                        }
                        case None => //skip the templates that are not found (they don't occurr in Wikipedia)
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match property syntax: " + line)
                case _ =>
            }
        }
        newResultMap
    }

    private def countProperties(fileName: String, resultMap: Map[String, TemplateStats], redirects: Map[String, String]): Map[String, TemplateStats] =
    {
        var newResultMap = resultMap
        // iterate through infobox test
        for (line <- Source.fromFile(fileName, "UTF-8").getLines())
        {
            line match
            {
                case DatatypePropertyTripleRegex(subj, pred, obj) =>
                {
                    // resolve redirect for *predicate*
                    val templateName = redirects.get(stripUri(pred)).getOrElse(stripUri(pred))

                    // lookup the *predicate* in the resultMap
                    newResultMap.get(templateName) match
                    {
                        case Some(stats: TemplateStats) =>
                        {
                            // lookup *object* in the properties map
                            stats.properties.get(stripUri(obj)) match
                            {
                                case Some(oldCount: Int) =>
                                {
                                    // increment count in properties map
                                    stats.properties = stats.properties.updated(stripUri(obj), oldCount + 1)
                                    newResultMap = newResultMap.updated(templateName, stats)
                                }
                                case None => //skip the properties that are not found with any count (they don't occurr in the template definition)
                            }
                        }
                        case None => //skip the templates that are not found (they don't occurr in Wikipedia)
                    }
                }
                case _ if line.nonEmpty => throw new IllegalArgumentException("line did not match countProperties syntax: " + line)
                case _ =>
            }
        }
        newResultMap
    }

    private def serialize(fileName: String, wikiStats: WikipediaStats)
    {
        val output = new ObjectOutputStream(new FileOutputStream(fileName))
        output.writeObject(wikiStats)
        output.close()
    }

    def deserialize(fileName: String): WikipediaStats =
    {
        val input = new ObjectInputStream(new FileInputStream(fileName))
        val m = input.readObject()
        input.close()
        m.asInstanceOf[WikipediaStats]
    }

    @deprecated
    private def serializeStats(fileName: String, statistics: Set[MappingStats])
    {
        val output = new ObjectOutputStream(new FileOutputStream(fileName))
        output.writeObject(statistics)
        output.close()
    }
}