package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import javax.ws.rs.core.Response
import org.dbpedia.extraction.server.Server
import collection.immutable.ListMap
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.server.util.CreateMappingStats._
import java.io._
import java.net.URI
import org.dbpedia.extraction.server.util.{CreateMappingStats, IgnoreList}
import java.lang.Boolean

@Path("/statistics/{lang}/")
class TemplateStatistics(@PathParam("lang") langCode: String, @QueryParam("p") password: String)
{
    private val language = Language.tryCode(langCode)
                                   .getOrElse(throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (!Server.config.languages.contains(language))
        throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val createMappingStats = new CreateMappingStats(language)

    private var wikipediaStatistics: WikipediaStats = null
    if (createMappingStats.mappingStatsObjectFile.isFile)
    {
        Server.logger.info("Loading serialized WikiStats object from " + createMappingStats.mappingStatsObjectFile)
        wikipediaStatistics = CreateMappingStats.deserialize(createMappingStats.mappingStatsObjectFile)
        Server.logger.info("done")
    }
    else
    {
        Server.logger.info("Can not load WikipediaStats from " + createMappingStats.mappingStatsObjectFile)
        throw new FileNotFoundException("Can not load WikipediaStats from " + createMappingStats.mappingStatsObjectFile)
    }

    private val mappings = getClassMappings
    private val mappingStatistics = createMappingStats.countMappedStatistics(mappings, wikipediaStatistics)
    private val ignoreList: IgnoreList = createMappingStats.loadIgnorelist()

    private val mappingUrlPrefix = Server.config.wikiPagesUrl + "/" + WikiTitle.mappingNamespace(language).get.toString + ":"

    private val mappedGoodColor = "#65c673"
    private val mappedMediumColor = "#ecea48"
    private val mappedBadColor = "#e0ab3a"
    private val notMappedColor = "#df5c56"

    private val goodThreshold = 0.8
    private val mediumThreshold = 0.4

    private val renameColor = "#b03060"
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
        val percentageMappedPropertyOccurrences: String = "%2.2f".format(getRatioOfAllMappedPropertyOccurrences(statsMap) * 100)

        // print percentage to file for Pablo's counter
        val out = new PrintWriter(createMappingStats.percentageFileName)
        out.write(percentageMappedTemplateOccurrences)
        out.close()

        //Server.logger.info("ratioTemp: " + percentageMappedTemplates)
        //Server.logger.info("ratioTempUses: " + percentageMappedTemplateOccurrences)
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
            <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
            </head>
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
                <p align="center">
                    {percentageMappedPropertyOccurrences}
                    % of all property occurrences in Wikipedia (
                    {langCode}
                    ) are mapped (
                    {getAllMappedPropertyOccurrences(statsMap)}
                    of
                    {getAllPropertyOccurrences(statsMap)}
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
                        <td bgcolor={ignoreColor}>template is on the ignorelist (is not an infobox that contains relevant properties)</td>
                    </tr>
                </table>

                <table align="center">
                    <tr>
                        <td>occurrences</td> <td colspan="2">template (with link to property statistics)</td>
                        <td>num properties</td> <td>mapped properties (%)</td>
                        <td>num property occurrences</td> <td>mapped property occurrences (%)</td> <td></td>
                    </tr>
                    {
                    // TODO: Solve problem of templates for which no properties are found in the template documentation (e.g. Geobox).
                    for ((mappingStat, counter) <- sortedStatsMap.filter(_._1.getNumberOfProperties(ignoreList) > 0)) yield
                        {
                            val decodedTemplateName = createMappingStats.doubleDecode(createMappingStats.encodedTemplateNamespacePrefix, language) + mappingStat.templateName
                            val encodedTemplateName = createMappingStats.doubleEncode(decodedTemplateName, language)
                            // de and el aren't encoded
                            val targetRedirect = reversedRedirects.get(encodedTemplateName)

                            val percentMappedProps: String = "%2.2f".format(mappingStat.getRatioOfMappedProperties(ignoreList) * 100)
                            val percentMappedPropOccur: String = "%2.2f".format(mappingStat.getRatioOfMappedPropertyOccurrences(ignoreList) * 100)
                            var minTempOccurToShow = 99
                            if (langCode != "en") minTempOccurToShow = 49
                            if (counter > minTempOccurToShow)
                            {
                                var mappingsWikiLink = mappingUrlPrefix + decodedTemplateName.substring(createMappingStats.doubleDecode(createMappingStats.encodedTemplateNamespacePrefix, language).length())
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


                                var mustRenamed : Boolean = false
                                var redirectMsg = ""
                                for (redirect <- targetRedirect)
                                {
                                    if (mappingStat.isMapped)
                                    {
                                        //redirectMsg = " NOTE: the mapping for " + WikiUtil.wikiDecode(redirect, language).substring(createMappingStats.templateNamespacePrefix.length()) + " is redundant!"
                                    }
                                    else
                                    {
                                        mappingsWikiLink = mappingUrlPrefix + redirect.substring(createMappingStats.encodedTemplateNamespacePrefix.length())
                                        bgcolor = renameColor
                                        mustRenamed = true
                                        redirectMsg = "Mapping of " + createMappingStats.doubleDecode(redirect.substring(createMappingStats.encodedTemplateNamespacePrefix.length()), language) + " must be renamed to "
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
                                    if (mustRenamed)
                                    {
                                        <td>
                                            {redirectMsg}<a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName)}>
                                            {WikiUtil.wikiDecode(mappingStat.templateName, language)}
                                        </a>
                                        </td>
                                    }
                                    else
                                    {

                                        <td>
                                            <a href={"../../templatestatistics/" + langCode + "/" + WikiUtil.wikiEncode(mappingStat.templateName)}>
                                                {WikiUtil.wikiDecode(mappingStat.templateName, language)}
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
                                </td>
                                    {if (Server.adminRights(password) && mappingStat.getNumberOfMappedProperties(ignoreList) == 0)
                                    {
                                        <td>
                                        <a href={"ignore/" + (! isIgnored).toString + "/" + WikiUtil.wikiEncode(mappingStat.templateName)+"?p="+password}>
                                            {ignoreMsg}
                                        </a>
                                        </td>
                                    }}
                                </tr>
                            }
                        }
                    }
                </table>
            </body>
        </html>
    }


    @GET
    @Path("ignore/{ignore}/{template: .+$}")
    @Produces(Array("application/xhtml+xml"))
    def ignoreListAction(@PathParam("template") template: String, @PathParam("ignore") ignore: String) =
    {
        if (Server.adminRights(password))
        {
            if (ignore == "true")
            {
                ignoreList.addTemplate(WikiUtil.wikiDecode(template))
            }
            else
            {
                ignoreList.removeTemplate(WikiUtil.wikiDecode(template))
            }
            createMappingStats.saveIgnorelist(ignoreList)
        }
        
        Response.temporaryRedirect(new URI("statistics/"+langCode+"/?p="+password)).build
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
        mappedRatio = getAllMappedPropertyOccurrences(stats).toDouble / getAllPropertyOccurrences(stats).toDouble
        mappedRatio
    }

    def getAllPropertyOccurrences(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (!ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + mappingStat.getNumberOfPropertyOccurrences(ignoreList)
            }
        }
        counter
    }

    def getAllMappedPropertyOccurrences(stats: Map[MappingStats, Int]) =
    {
        var counter: Int = 0
        for ((mappingStat, templateCounter) <- stats)
        {
            if (!ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                counter = counter + mappingStat.getNumberOfMappedPropertyOccurrences(ignoreList)
            }
        }
        counter
    }
}