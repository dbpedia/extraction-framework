package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import javax.ws.rs.core.Response
import org.dbpedia.extraction.server.Server
import scala.collection.mutable
import scala.collection.immutable.ListMap
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.server.util.MappingStats
import org.dbpedia.extraction.server.util.StringUtils.urlEncode
import java.net.URI
import java.io.PrintWriter

@Path("/statistics/{lang}/")
class TemplateStatistics(@PathParam("lang") langCode: String, @QueryParam("p") password: String, @QueryParam("all") all: Boolean)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (! Server.languages.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val manager = Server.statsManager(language)

    private var wikiStats = manager.wikiStats

    private val mappings = getClassMappings
    private val mappingStatistics = manager.countMappedStatistics(mappings, wikiStats)
    private val ignoreList = manager.loadIgnorelist()

    private val mappingUrlPrefix = Server.wikiPagesUrl + "/" + Namespace.mappingNamespace(language).get.toString + ":"

    private val mappedGoodColor = "#65c673"
    private val mappedMediumColor = "#ecea48"
    private val mappedBadColor = "#e0ab3a"
    private val notMappedColor = "#df5c56"

    private val goodThreshold = 0.8
    private val mediumThreshold = 0.4

    private val renameColor = "#b03060"
    private val ignoreColor = "#cdcdcd"

    private def cookieQuery(sep: Char, all: Boolean = all) : String = {
      val sb = new StringBuilder
      
      var vsep = sep
      if (Server.adminRights(password)) {
        sb append sep append "p=" append password
        vsep = '&'
      }
      
      if (all) sb append vsep append "all=true"
      
      sb toString
    }
      
    private val minCount = Map("ar"->50,"bn"->50,"ca"->100,"cs"->100,"de"->100,"el"->50,"en"->500,"es"->100,"eu"->50,"fr"->200,"ga"->50,"hi"->50,"hr"->50,"hu"->50,"it"->100,"ko"->50,"nl"->200,"pl"->50,"pt"->50,"ru"->100,"sl"->10,"tr"->10)

    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        val minCounter = minCount(language.wikiCode)
        
        var statsMap = new mutable.HashMap[MappingStats, Int]
        for (mappingStat <- mappingStatistics) statsMap(mappingStat) = mappingStat.templateCount
        
        val sortedStatsMap = ListMap(statsMap.toList.sortBy(statsInt => (-statsInt._2, statsInt._1)): _*)

        val reversedRedirects = wikiStats.checkForRedirects(sortedStatsMap, mappings)
        val percentageMappedTemplates: String = "%2.2f".format(getNumberOfMappedTemplates(statsMap).toDouble / getNumberOfTemplates(statsMap).toDouble * 100)
        val percentageMappedTemplateOccurrences: String = "%2.2f".format(getRatioOfMappedTemplateOccurrences(statsMap) * 100)
        val percentageMappedPropertyOccurrences: String = "%2.2f".format(getRatioOfAllMappedPropertyOccurrences(statsMap) * 100)

        // print percentage to file for Pablo's counter
        val out = new PrintWriter(manager.percentageFile)
        try out.write(percentageMappedTemplateOccurrences) finally out.close()

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
                    for ((mappingStat, counter) <- sortedStatsMap; if mappingStat.getNumberOfProperties(ignoreList) > 0) yield
                    {
                        if (all || counter >= minCounter || mappingStat.isMapped) { 
                            val templateName = manager.templateNamespacePrefix + mappingStat.templateName
                            val targetRedirect = reversedRedirects.get(templateName)

                            val percentMappedProps: String = "%2.2f".format(mappingStat.getRatioOfMappedProperties(ignoreList) * 100)
                            val percentMappedPropOccur: String = "%2.2f".format(mappingStat.getRatioOfMappedPropertyOccurrences(ignoreList) * 100)
                            var mappingsWikiLink = mappingUrlPrefix + mappingStat.templateName
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
                                    mappingsWikiLink = mappingUrlPrefix + redirect.substring(manager.templateNamespacePrefix.length)
                                    bgcolor = renameColor
                                    mustRenamed = true
                                    redirectMsg = "Mapping of " + redirect.substring(manager.templateNamespacePrefix.length) + " must be renamed to "
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
                                    <a name={urlEncode(mappingStat.templateName)}/>
                                        {counter}
                                    </td>
                                {
                                if (mustRenamed)
                                {
                                    <td>
                                            {redirectMsg}<a href={"../../templatestatistics/"+langCode+"/?template="+mappingStat.templateName+cookieQuery('&')}>
                                            {mappingStat.templateName}
                                        </a>
                                        </td>
                                }
                                else
                                {

                                    <td>
                                            <a href={"../../templatestatistics/"+langCode+"/?template="+mappingStat.templateName+cookieQuery('&')}>
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
                                </td>
                                    {if (Server.adminRights(password) && mappingStat.getNumberOfMappedProperties(ignoreList) == 0)
                                {
                                    <td>
                                        <a href={"../../ignore/"+langCode+"/template/?ignore="+(! isIgnored)+"&template="+mappingStat.templateName+cookieQuery('&')}>
                                            {ignoreMsg}
                                        </a>
                                        </td>
                                }}
                                </tr>
                            }
                        }
                    }
                </table>
                { if (! all) { <p align="center"><a href={cookieQuery('?', true)}>View all</a></p> } }
            </body>
        </html>
    }
    
    /**
     * @return Set[String, ClassMapping] (encoded template name with Template%3A prefix, ClassMapping)
     */
    def getClassMappings =
    {
        val mappings = Server.extractor.mappings(language)
        mappings.templateMappings ++ mappings.conditionalMappings
    }

    def getRatioOfMappedTemplateOccurrences(stats: mutable.Map[MappingStats, Int]) =
    {
        var mappedRatio: Double = 0
        mappedRatio = getNumberOfMappedTemplateOccurrences(stats).toDouble / getNumberOfTemplateOccurrences(stats).toDouble
        mappedRatio
    }

    def getNumberOfTemplates(stats: mutable.Map[MappingStats, Int]) =
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

    def getNumberOfTemplateOccurrences(stats: mutable.Map[MappingStats, Int]) =
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

    def getNumberOfMappedTemplateOccurrences(stats: mutable.Map[MappingStats, Int]) =
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

    def getNumberOfMappedTemplates(stats: mutable.Map[MappingStats, Int]) =
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

    def getRatioOfAllMappedPropertyOccurrences(stats: mutable.Map[MappingStats, Int]) =
    {
        var mappedRatio: Double = 0
        mappedRatio = getAllMappedPropertyOccurrences(stats).toDouble / getAllPropertyOccurrences(stats).toDouble
        mappedRatio
    }

    def getAllPropertyOccurrences(stats: mutable.Map[MappingStats, Int]) =
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

    def getAllMappedPropertyOccurrences(stats: mutable.Map[MappingStats, Int]) =
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