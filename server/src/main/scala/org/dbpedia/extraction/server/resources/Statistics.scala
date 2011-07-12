package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import javax.ws.rs.core.MediaType;
import org.dbpedia.extraction.server.{ServerExtractionContext, Server}
import collection.immutable.ListMap
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.server.util.CreateMappingStats.{WikipediaStats, MappingStats}
import java.io._
import org.dbpedia.extraction.server.util.{CreateMappingStats, IgnoreList}

@Path("/statistics/{lang}")
class Statistics(@PathParam("lang") langCode: String) extends Base
{
    private val language = Language.fromWikiCode(langCode)
            .getOrElse(throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (!Server.config.languages.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private var wikipediaStatistics: WikipediaStats = null
    if (new File(CreateMappingStats.serializeFileName).isFile)
    {
        println("Loading serialized object from " + CreateMappingStats.serializeFileName)
        wikipediaStatistics = CreateMappingStats.deserialize(CreateMappingStats.serializeFileName)
        println("loaded!")
    }
    else
    {
        println("Can not load WikipediaStats from " + CreateMappingStats.serializeFileName)
        throw new FileNotFoundException("Can not load WikipediaStats from " + CreateMappingStats.serializeFileName)
    }

    private val mappings = getClassMappings()
    private val mappingStatistics = CreateMappingStats.countMappedStatistics(mappings, wikipediaStatistics)
    private val ignoreList: IgnoreList = CreateMappingStats.loadIgnorelist()

    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        var statsMap: Map[MappingStats, Int] = Map()
        for (mappingStat <- mappingStatistics)
        {
            statsMap += ((mappingStat, mappingStat.templateCount))
        }
        val sortedStatsMap = ListMap(statsMap.toList.sortBy
        {
            case (key, value) => (-value, key)
        }: _*)

        val reversedRedirects = wikipediaStatistics.checkForRedirects(sortedStatsMap, mappings, language)
        val percentageMappedTemplates: String = "%2.2f".format(getNumberOfMappedTemplates(statsMap).toDouble / getNumberOfTemplates(statsMap).toDouble * 100)
        val percentageMappedTemplateOccurrences: String = "%2.2f".format(getRatioOfMappedTemplateOccurrences(statsMap) * 100)
        println("ratioTemp: " + percentageMappedTemplates)
        println("ratioTempUses: " + percentageMappedTemplateOccurrences)
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
            <body>
                <h2 align="center">Mapping Statistics (
                    {langCode}
                    )</h2>
                <p align="center">
                    {percentageMappedTemplates}
                    % templates are mapped (
                    {getNumberOfMappedTemplates(statsMap)}
                    of
                    {getNumberOfTemplates(statsMap)}
                    ).</p>
                <p align="center">
                    {percentageMappedTemplateOccurrences}
                    % of all template occurrences in Wikipedia (
                    {langCode}
                    ) are mapped (
                    {getNumberOfMappedTemplateOccurrences(statsMap)}
                    of
                    {getNumberOfTemplateOccurrences(statsMap)}
                    ).</p>

                <table align="center">
                    <caption>The color codes:</caption>
                    <tr>
                        <td bgcolor="#4E9258">template is mapped</td>
                    </tr>
                    <tr>
                        <td bgcolor="#C24641">template is not mapped</td>
                    </tr>
                    <tr>
                        <td bgcolor="#FFE87C">template mapping must be renamed</td>
                    </tr>
                    <tr>
                        <td bgcolor="#808080">template is on the ignorelist and does not contain relevant properties</td>
                    </tr>
                </table>

                <table align="center">
                    <tr>
                        <td>occurrence</td> <td colspan="2">template (with link to property statistics)</td>
                        <td>num properties</td> <td>mapped properties (%)</td>
                        <td>num property occurrences</td> <td>mapped property occurrence (%)</td> <td></td>
                    </tr>{for ((mappingStat, counter) <- sortedStatsMap.filter(_._1.getNumberOfProperties() > 0)) yield
                {
                    val encodedTemplateName = "Template%3A" + WikiUtil.wikiEncode(mappingStat.templateName, language)
                    val targetRedirect = reversedRedirects.get(encodedTemplateName)

                    val percentMappedProps: String = "%2.2f".format(mappingStat.getRatioOfMappedProperties() * 100)
                    val percentMappedPropOccur: String = "%2.2f".format(mappingStat.getRatioOfMappedPropertyOccurrences() * 100)
                    if (counter > 99)
                    {
                        var mappingsWikiLink = "http://mappings.dbpedia.org/index.php/Mapping:" + encodedTemplateName.substring(11)
                        var bgcolor: String = ""
                        if (mappingStat.isMapped) bgcolor = "#4E9258" else bgcolor = "#C24641"

                        var redirectMsg = ""
                        for (redirect <- targetRedirect)
                        {
                            mappingsWikiLink = "http://mappings.dbpedia.org/index.php/Mapping:" + redirect.substring(11)
                            if (mappingStat.isMapped)
                            {
                                //redirectMsg = " NOTE: the mapping for " + WikiUtil.wikiDecode(redirect, language).substring(9) + " is redundant!"
                            }
                            else
                            {
                                bgcolor = "#FFE87C"
                                redirectMsg = WikiUtil.wikiDecode(redirect, language).substring(9) + " must be renamed to "
                            }
                        }

                        var isIgnored: Boolean = false
                        var ignoreMsg: String = "add to ignore list"
                        if (ignoreList.isTemplateIgnored(mappingStat.templateName))
                        {
                            isIgnored = true
                            ignoreMsg = "remove from ignore list"
                            bgcolor = "#808080"
                        }

                        <tr bgcolor={bgcolor}>
                            <td align="right">
                                {counter}
                            </td>{if (bgcolor == "#FFE87C")
                        {
                            <td>
                                {redirectMsg}<a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName)}>
                                {mappingStat.templateName}
                            </a>
                            </td>
                        }
                        else
                        {

                            <td>
                                <a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName)}>
                                    {mappingStat.templateName}
                                </a>{redirectMsg}
                            </td>
                        }}<td>
                            <a href={mappingsWikiLink}>
                                Edit
                            </a>
                        </td>
                                <td align="right">
                                    {mappingStat.getNumberOfProperties}
                                </td> <td align="right">
                            {percentMappedProps}
                        </td>
                                <td align="right">
                                    {mappingStat.getNumberOfPropertyOccurrences}
                                </td> <td align="right">
                            {percentMappedPropOccur}
                        </td> <td>
                            <a href={WikiUtil.wikiEncode(mappingStat.templateName) + "/" + isIgnored.toString()}>
                                {ignoreMsg}
                            </a>
                        </td>
                        </tr>
                    }
                }}
                </table>
            </body>
        </html>
    }

    @GET
    @Path("/{template}/{ignorelist}")
    @Produces(Array("application/xhtml+xml"))
    def ignoreListAction(@PathParam("template") template: String, @PathParam("ignorelist") ignored: String) =
    {
        if (ignored == "true")
        {
            ignoreList.removeTemplate(WikiUtil.wikiDecode(template))
            <h2>removed from ignore list</h2>
        }
        else
        {
            ignoreList.addTemplate(WikiUtil.wikiDecode(template))
            <h2>added to ignore list</h2>
        }
        val html =
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
                <head>
                    <script type="text/javascript">
                        <!--
                   window.location="http://localhost:9999/statistics/en/";
                //-->
                    </script>
                </head>
                <body>
                </body>
            </html>
        CreateMappingStats.saveIgnorelist(ignoreList)
        html
    }

    /**
     * @return Set[String, ClassMapping] (encoded template name with Template%3A prefix, ClassMapping)
     */
    def getClassMappings() =
    {
        val (templateMappings, tableMappings, conditionalMappings) = MappingsLoader.load(new ServerExtractionContext(language, Server.extractor))
        templateMappings ++ conditionalMappings
    }

    def loadStatistics(fileName: String): Set[MappingStats] =
    {
        val input = new ObjectInputStream(new FileInputStream(fileName))
        val m = input.readObject()
        input.close()
        m.asInstanceOf[Set[MappingStats]]
    }

    def getRatioOfMappedTemplateOccurrences(stats: Map[MappingStats, Int]) =
    {
        var mappedRatio: Double = 0
        mappedRatio = getNumberOfMappedTemplateOccurrences(stats).toDouble / getNumberOfTemplateOccurrences(stats).toDouble
        mappedRatio
    }

    def getNumberOfTemplates(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (!ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + 1
            }
        }
        counter
    }

    def getNumberOfTemplateOccurrences(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (!ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + templateCounter
            }
        }
        counter
    }

    def getNumberOfMappedTemplateOccurrences(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (mappingStat.isMapped && !ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + templateCounter
            }
        }
        counter
    }

    def getNumberOfMappedTemplates(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (mappingStat.isMapped && !ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + 1
            }
        }
        counter
    }

    def getRatioOfAllMappedProperties(stats: Map[MappingStats, Int]) =
    {
        var mappedRatio: Double = 0
        var numMP: Int = 0
        var numP: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            numMP = numMP + mappingStat.getNumberOfMappedProperties()
            numP = numP + mappingStat.getNumberOfProperties()
        }
        mappedRatio = numMP.toDouble / numP.toDouble
        mappedRatio
    }

    def getRatioOfAllMappedPropertyOccurrences(stats: Map[MappingStats, Int]) =
    {
        var mappedRatio: Double = 0
        var numMPO: Int = 0
        var numPO: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            numMPO = numMPO + mappingStat.getNumberOfMappedPropertyOccurrences()
            numPO = numPO + mappingStat.getNumberOfPropertyOccurrences()
        }
        mappedRatio = numMPO.toDouble / numPO.toDouble
        mappedRatio
    }
}