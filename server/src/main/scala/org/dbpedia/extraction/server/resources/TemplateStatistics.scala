package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import org.dbpedia.extraction.server.Server
import collection.immutable.ListMap
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.server.util.CreateMappingStats._
import java.io._
import org.dbpedia.extraction.server.util.{CreateMappingStats, IgnoreList}

@Path("/statistics/{lang}")
class TemplateStatistics(@PathParam("lang") langCode: String) extends Base
{
    private val language = Language.fromWikiCode(langCode)
                                   .getOrElse(throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (!Server.config.languages.contains(language))
        throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val createMappingStats = new CreateMappingStats(language)

    private var wikipediaStatistics: WikipediaStats = null
    if (new File(createMappingStats.mappingStatsObjectFileName).isFile)
    {
        Server.logger.info("Loading serialized WikiStats object from " + createMappingStats.mappingStatsObjectFileName)
        wikipediaStatistics = CreateMappingStats.deserialize(createMappingStats.mappingStatsObjectFileName)
		Server.logger.info("done")
    }
    else
    {
        Server.logger.info("Can not load WikipediaStats from " + createMappingStats.mappingStatsObjectFileName)
        throw new FileNotFoundException("Can not load WikipediaStats from " + createMappingStats.mappingStatsObjectFileName)
    }

    private val mappings = getClassMappings
    private val mappingStatistics = createMappingStats.countMappedStatistics(mappings, wikipediaStatistics)
    private val ignoreList: IgnoreList = createMappingStats.loadIgnorelist()

    private val mappingUrlPrefix =
        if (langCode == "en") "http://mappings.dbpedia.org/index.php/Mapping:"
        else "http://mappings.dbpedia.org/index.php/Mapping_"+langCode+":"


    private val mappedGoodColor = "#65c673"
    private val mappedMediumColor = "#ecea48"
    private val mappedBadColor = "#e0ab3a"
    private val notMappedColor = "#df5c56"

    private val goodThreshold = 0.8
    private val mediumThreshold = 0.4

    private val renameColor = "#df5c56"
    private val ignoreColor = "#cdcdcd"

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

        // print percentage to file for Pablo's counter
        val out = new PrintWriter(createMappingStats.percentageFileName)
        out.write(percentageMappedTemplateOccurrences)
        out.close()

        //Server.logger.info("ratioTemp: " + percentageMappedTemplates)
        //Server.logger.info("ratioTempUses: " + percentageMappedTemplateOccurrences)
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
            <body>
                <h2 align="center">Mapping Statistics for <u>{langCode}</u></h2>
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
                        <td bgcolor={mappedGoodColor}>template is mapped with more than
                            {"%2.0f".format(goodThreshold*100)}%
                        </td>
                    </tr>
                    <tr>
                        <td bgcolor={mappedMediumColor}>template is mapped with more than
                            {"%2.0f".format(mediumThreshold*100)}%
                        </td>
                    </tr>
                    <tr>
                        <td bgcolor={mappedBadColor}>template is mapped with less than
                            {"%2.0f".format(mediumThreshold*100)}%
                        </td>
                    </tr>
                    <tr>
                        <td bgcolor={notMappedColor}>template is not mapped</td>
                    </tr>
                    <tr>
                        <td bgcolor={renameColor}>template mapping must be renamed</td>
                    </tr>
                    <tr>
                        <td bgcolor={ignoreColor}>template is on the ignorelist and does not contain relevant properties</td>
                    </tr>
                </table>

                <table align="center">
                    <tr>
                        <td>occurrences</td> <td colspan="2">template (with link to property statistics)</td>
                        <td>num properties</td> <td>mapped properties (%)</td>
                        <td>num property occurrences</td> <td>mapped property occurrences (%)</td> <td></td>
                    </tr>{for ((mappingStat, counter) <- sortedStatsMap.filter(_._1.getNumberOfProperties(ignoreList) > 0)) yield
                {
                    val encodedTemplateName = "Template%3A" + WikiUtil.wikiEncode(mappingStat.templateName, language)
                    val targetRedirect = reversedRedirects.get(encodedTemplateName)

                    val percentMappedProps: String = "%2.2f".format(mappingStat.getRatioOfMappedProperties(ignoreList) * 100)
                    val percentMappedPropOccur: String = "%2.2f".format(mappingStat.getRatioOfMappedPropertyOccurrences(ignoreList) * 100)
                    if (counter > 99)
                    {
                        var mappingsWikiLink = mappingUrlPrefix + encodedTemplateName.substring(11)
                        var bgcolor: String =
                            if(!mappingStat.isMapped)
                            {
                                notMappedColor
                            }
                            else
                            {
                                if(mappingStat.getRatioOfMappedPropertyOccurrences(ignoreList) > goodThreshold)
                                {
                                    mappedGoodColor
                                }
                                else if(mappingStat.getRatioOfMappedPropertyOccurrences(ignoreList) > mediumThreshold)
                                {
                                    mappedMediumColor
                                }
                                else
                                {
                                    mappedBadColor
                                }
                            }


                        var redirectMsg = ""
                        for (redirect <- targetRedirect)
                        {
                            if (mappingStat.isMapped)
                            {
                                //redirectMsg = " NOTE: the mapping for " + WikiUtil.wikiDecode(redirect, language).substring(9) + " is redundant!"
                            }
                            else
                            {
                                mappingsWikiLink = mappingUrlPrefix + redirect.substring(11)
                                bgcolor = renameColor
                                redirectMsg = WikiUtil.wikiDecode(redirect, language).substring(9) + " must be renamed to "
                            }
                        }

                        var isIgnored: Boolean = false
                        var ignoreMsg: String = "add to ignore list"
                        if (ignoreList.isTemplateIgnored(mappingStat.templateName))
                        {
                            isIgnored = true
                            ignoreMsg = "remove from ignore list"
                            bgcolor = ignoreColor
                        }

                        <tr bgcolor={bgcolor}>
                            <td align="right">
                                {counter}
                            </td>
                        {
                            if (bgcolor == renameColor)
                            {
                                <td>
                                    {redirectMsg}<a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName) + "/"}>
                                    {mappingStat.templateName}
                                </a>
                                </td>
                            }
                            else
                            {

                                <td>
                                    <a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName) + "/"}>
                                        {mappingStat.templateName}
                                    </a>{redirectMsg}
                                </td>
                            }
                        }<td>
                            <a href={mappingsWikiLink}>
                                Edit
                            </a>
                        </td>
                                <td align="right">
                                    {mappingStat.getNumberOfProperties(ignoreList)}
                                </td> <td align="right">
                            {percentMappedProps}
                        </td>
                                <td align="right">
                                    {mappingStat.getNumberOfPropertyOccurrences(ignoreList)}
                                </td> <td align="right">
                            {percentMappedPropOccur}
                        </td> <td>
                            <a href={WikiUtil.wikiEncode(mappingStat.templateName) + "/" + isIgnored.toString}>
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
                        window.location="../";
                        //-->
                    </script>
                </head>
                <body>
                </body>
            </html>
        createMappingStats.saveIgnorelist(ignoreList)
        html
    }

    /**
     * @return Set[String, ClassMapping] (encoded template name with Template%3A prefix, ClassMapping)
     */
    def getClassMappings =
    {
        val mappings = Server.extractor.mappings(language)
        mappings.templateMappings ++ mappings.conditionalMappings
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
            numMP = numMP + mappingStat.getNumberOfMappedProperties(ignoreList)
            numP = numP + mappingStat.getNumberOfProperties(ignoreList)
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
            numMPO = numMPO + mappingStat.getNumberOfMappedPropertyOccurrences(ignoreList)
            numPO = numPO + mappingStat.getNumberOfPropertyOccurrences(ignoreList)
        }
        mappedRatio = numMPO.toDouble / numPO.toDouble
        mappedRatio
    }
}