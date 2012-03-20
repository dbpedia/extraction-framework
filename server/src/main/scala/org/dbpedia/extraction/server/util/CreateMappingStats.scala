package org.dbpedia.extraction.server.util

import io.Source
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.Serializable
import java.io._
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.server.util.CreateMappingStats._
import java.net.{URLDecoder, URLEncoder}

/**
 * Script to gather statistics about mappings: how often they are used, which properties are used and for what mappings exist.
 * Take care of dump encodings. I had problems with the german redirects dump and had to delete all triples with a % sign.
 * There were only a few irrelevant templates, therefore it isn't a big deal.
 */
class CreateMappingStats(val language: Language)
{

    val mappingStatsObjectFileName = "src/main/resources/mappingstats_" + language.wikiCode + ".obj"

    val ignoreListFileName = "src/main/resources/ignoreList_" + language.wikiCode + ".obj"
    val ignoreListTemplatesFileName = "src/main/resources/ignoreListTemplates_" + language.wikiCode + ".txt"
    val ignoreListPropertiesFileName = "src/main/resources/ignoreListProperties_" + language.wikiCode + ".txt"

    val percentageFileName = "src/main/resources/percentage." + language.wikiCode

    val encodedTemplateNamespacePrefix = doubleEncode(Namespaces.getNameForNamespace(language, WikiTitle.Namespace.Template) + ":", language)
    private val resourceNamespacePrefix = if (language.wikiCode == "de" || language.wikiCode == "el" ||
            language.wikiCode == "it" || language.wikiCode == "ru") "http://" + language.wikiCode + ".dbpedia.org/resource/"
    else "http://dbpedia.org/resource/"

    private val ObjectPropertyTripleRegex = """<([^>]+)> <([^>]+)> <([^>]+)> .""".r
    private val DatatypePropertyTripleRegex = """<([^>]+)> <([^>]+)> "(.+?)"@?\w?\w? .""".r

    def doubleDecode(string: String, lang: Language): String =
    {
        var output = ""
        output = URLDecoder.decode(WikiUtil.wikiDecode(string, lang), "UTF-8")
        output
    }

    def doubleEncode(string: String, lang: Language): String =
    {
        var output = ""
        output = URLEncoder.encode(WikiUtil.wikiEncode(string, lang), "UTF-8")
        output
    }

    def isTemplateNamespaceEncoded(template: String): Int =
    {
        var output: Int = -1
        if (template startsWith encodedTemplateNamespacePrefix)
        {
            output = 1
        }
        if (template startsWith doubleDecode(encodedTemplateNamespacePrefix, language))
        {
            output = 0
        }
        output
    }

    def getDecodedTemplateName(rawTemplate: String): String =
    {
        var templateName = ""
        if (isTemplateNamespaceEncoded(rawTemplate) == 1)
        {
            templateName = doubleDecode(rawTemplate.substring(encodedTemplateNamespacePrefix.length()), language)
        }
        else if (isTemplateNamespaceEncoded(rawTemplate) == 0)
        {
            val subLength = doubleDecode(encodedTemplateNamespacePrefix, language).length()
            templateName = rawTemplate.substring(subLength).replace("_", " ")
        }
        else
        {
            templateName = WikiUtil.wikiDecode(rawTemplate)
        }
        templateName
    }

    def countMappedStatistics(mappings: Map[String, ClassMapping], wikipediaStatistics: WikipediaStats) =
    {
        val startTime = System.currentTimeMillis()

        // Hold the overall statistics
        var statistics: Set[MappingStats] = Set() // new Set() with Serializable

        var isMapped: Boolean = false

        for ((rawTemplate, templateStats) <- wikipediaStatistics.templates)
        {
            val templateName: String = getDecodedTemplateName(rawTemplate)

            //mappings: el, en, pt decoded templates without _
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

        Server.logger.fine("countMappedStatistics: " + (System.currentTimeMillis() - startTime) / 1000 + " s")
        statistics
    }


    def loadIgnorelist() =
    {
        if (new File(ignoreListFileName).isFile)
        {
            Server.logger.fine("Loading serialized object from " + ignoreListFileName)
            val input = new ObjectInputStream(new FileInputStream(ignoreListFileName))
            val m = input.readObject()
            input.close()
            m.asInstanceOf[IgnoreList]
        }
        else
        {
            val ign: IgnoreList = new IgnoreList(language)
            ign
        }
    }

    def saveIgnorelist(ignoreList: IgnoreList)
    {
        val output = new ObjectOutputStream(new FileOutputStream(ignoreListFileName))
        output.writeObject(ignoreList)
        output.close()
        ignoreList.exportToTextFile(ignoreListTemplatesFileName, ignoreListPropertiesFileName)
    }

    private def getWikipediaStats(redirectsFile: String, infoboxPropsFile: String, templParsFile: String, parsUsageFile: String): WikipediaStats =
    {
        var templatesMap: Map[String, TemplateStats] = Map() // "templateName" -> TemplateStats

        println("Reading redirects from " + redirectsFile)
        val redirects: Map[String, String] = loadTemplateRedirects(redirectsFile)
        println("  " + redirects.size + " redirects")
        println("Using Template namespace prefix " + encodedTemplateNamespacePrefix + " for language " + language.wikiCode)
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
                    //TODO: adjust depending on encoding in redirects file
                    var templateNamespacePrefix = ""
                    if (language.wikiCode == "de" || language.wikiCode == "el" || language.wikiCode == "ru")
                    {
                        templateNamespacePrefix = doubleDecode(encodedTemplateNamespacePrefix, language)
                    }
                    else
                    {
                        templateNamespacePrefix = encodedTemplateNamespacePrefix
                    }
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
                            stats.properties = stats.properties.updated(convertFromEscapedString(stripUri(obj)), 0)
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
                            stats.properties.get(convertFromEscapedString(stripUri(obj))) match
                            {
                                case Some(oldCount: Int) =>
                                {
                                    // increment count in properties map
                                    stats.properties = stats.properties.updated(convertFromEscapedString(stripUri(obj)), oldCount + 1)
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

    private def checkMapping(template: String, mappings: Map[String, ClassMapping]) =
    {
        if (mappings.contains(template)) true
        else false
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

    def convertFromEscapedString(value: String): String =
    {
        val sb = new java.lang.StringBuilder

        // iterate over code points (http://blogs.sun.com/darcy/entry/iterating_over_codepoints)
        val inputLength = value.length
        var offset = 0

        while (offset < inputLength)
        {
            val c = value.charAt(offset)
            if (c != '\\') sb append c
            else
            {
                offset += 1
                val specialChar = value.charAt(offset)
                specialChar match
                {
                    case '"' => sb append '"'
                    case 't' => sb append '\t'
                    case 'r' => sb append '\r'
                    case '\\' => sb append '\\'
                    case 'n' => sb append '\n'
                    case 'u' =>
                    {
                        offset += 1
                        val codepoint = value.substring(offset, offset + 4)
                        val character = Integer.parseInt(codepoint, 16).asInstanceOf[Char]
                        sb append character
                        offset += 3
                    }
                    case 'U' =>
                    {
                        offset += 1
                        val codepoint = value.substring(offset, offset + 8)
                        val character = Integer.parseInt(codepoint, 16)
                        sb appendCodePoint character
                        offset += 7
                    }
                }
            }
            offset += 1
        }
        sb.toString
    }

    /**
     * / to %2F did not work, therefore this hack.
     */
    def encodeSlash(url: String): String =
    {
        url.replace("/", "S-L-A-S-H")
    }

    def decodeSlash(url: String): String =
    {
        url.replace("S-L-A-S-H", "/")
    }
}

object CreateMappingStats
{

    // Hold template statistics
    class TemplateStats(var templateCount: Int = 0, var properties: Map[String, Int] = Map()) extends Serializable
    {
        override def toString = "TemplateStats[count:" + templateCount + ",properties:" + properties.mkString(",") + "]"
    }

    /**
     * Contains the statistic of a Template
     */
    class MappingStats(templateStats: TemplateStats, val templateName: String) extends Ordered[MappingStats] with Serializable
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

        def getNumberOfProperties(ignoreList: IgnoreList) =
        {
            var counter: Int = 0
            for ((propName, _) <- templateStats.properties)
            {
                if (!ignoreList.isPropertyIgnored(templateName, propName))
                {
                    counter = counter + 1
                }
            }
            counter
        }

        def getNumberOfMappedProperties(ignoreList: IgnoreList) =
        {
            var numMPs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propIsMapped && propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numMPs = numMPs + 1
            }
            numMPs
        }

        def getRatioOfMappedProperties(ignoreList: IgnoreList) =
        {
            var mappedRatio: Double = 0
            mappedRatio = getNumberOfMappedProperties(ignoreList).toDouble / getNumberOfProperties(ignoreList).toDouble
            mappedRatio
        }

        def getNumberOfPropertyOccurrences(ignoreList: IgnoreList) =
        {
            var numPOs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numPOs = numPOs + propCount
            }
            numPOs
        }

        def getNumberOfMappedPropertyOccurrences(ignoreList: IgnoreList) =
        {
            var numMPOs: Int = 0
            for ((propName, (propCount, propIsMapped)) <- properties)
            {
                if (propIsMapped && propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numMPOs = numMPOs + propCount
            }
            numMPOs
        }

        def getRatioOfMappedPropertyOccurrences(ignoreList: IgnoreList) =
        {
            var mappedRatio: Double = 0
            mappedRatio = getNumberOfMappedPropertyOccurrences(ignoreList).toDouble / getNumberOfPropertyOccurrences(ignoreList).toDouble
            mappedRatio
        }

        def compare(that: MappingStats) =
            this.templateCount - that.templateCount
    }

    // Hold template redirects and template statistics
    class WikipediaStats(var redirects: Map[String, String] = Map(), var templates: Map[String, TemplateStats] = Map()) extends Serializable
    {

        def checkForRedirects(mappingStats: Map[MappingStats, Int], mappings: Map[String, ClassMapping], lang: Language) =
        {
            val templateNamespacePrefix = Namespaces.getNameForNamespace(lang, WikiTitle.Namespace.Template) + ":"
            val mappedRedirrects = redirects.filterKeys(title => mappings.contains(WikiUtil.wikiDecode(title, lang).substring(templateNamespacePrefix.length())))
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
        val language = Language.forCode(args(5))
        var wikiStats: WikipediaStats = null
        val startTime = System.currentTimeMillis()
        val createMappingStats = new CreateMappingStats(language)

        if (new File(serializeFileName).isFile)
        {
            Server.logger.info("Loading serialized object from " + serializeFileName)
            wikiStats = deserialize(serializeFileName)
        }
        else
        {
            wikiStats = createMappingStats.getWikipediaStats(redirectsDatasetFileName, infoboxPropertiesDatasetFileName, templateParametersDatasetFileName, infoboxTestDatasetFileName)
            Server.logger.info("Serializing to " + serializeFileName)
            serialize(serializeFileName, wikiStats)
        }
        Server.logger.info((System.currentTimeMillis() - startTime) / 1000 + " s")
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


}