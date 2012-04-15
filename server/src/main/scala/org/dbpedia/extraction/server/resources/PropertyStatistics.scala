package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import org.dbpedia.extraction.server.Server
import collection.immutable.ListMap
import scala.collection.mutable
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.server.util.MappingStats
import org.dbpedia.extraction.server.util.StringUtils.urlEncode
import java.io.{FileNotFoundException, File}

@Path("/templatestatistics/{lang}/")
class PropertyStatistics(@PathParam("lang") langCode: String, @QueryParam("template") template: String, @QueryParam("p") password: String)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if (!Server.languages.contains(language)) throw new WebApplicationException(new Exception("language "+langCode+" not defined in server"), 404)

    private val mappingUrlPrefix = Server.wikiPagesUrl+"/"+Namespace.mappings(language).toString + ":"

    private val manager = Server.statsManager(language)

    private var wikipediaStatistics = manager.wikiStats

    private val mappings = getClassMappings
    private val statistics = manager.countMappedStatistics(mappings, wikipediaStatistics)
    private val ignoreList = manager.loadIgnorelist()

    private val mappedColor = "#65c673"
    private val notMappedColor = "#e05d57"
    private val ignoreColor = "#b0b0b0"
    private val notDefinedColor = "#FFF8C6"

    private def wikiDecode(name: String) : String = WikiUtil.wikiDecode(name, language, capitalize=false)

    private def passwordQuery : String = if (Server.adminRights(password)) "?p="+password else ""

    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        val ms = getMappingStats(wikiDecode(template))
        if (ms == null)
        {
            throw new IllegalArgumentException("Could not find template: " + wikiDecode(template))
        }
        else
        {
            val propMap: mutable.Map[String, (Int, Boolean)] = ms.properties
            val sortedPropMap = ListMap(propMap.toList.sortBy
            {
                case (key, (value1, value2)) => -value1
            }: _*)

            val percentageMappedProps: String = "%2.2f".format(ms.getRatioOfMappedProperties(ignoreList) * 100)
            val percentageMappedPropOccurrences: String = "%2.2f".format(ms.getRatioOfMappedPropertyOccurrences(ignoreList) * 100)
            Server.logger.fine("ratioTemp: " + percentageMappedProps)
            Server.logger.fine("ratioTempUses: " + percentageMappedPropOccurrences)
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
                <head>
                  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                </head>
                <body>

                    <h2 align="center">Template Statistics  for <a href={mappingUrlPrefix + template}>{wikiDecode(template)}</a></h2>
                    <p align="center">
                        {percentageMappedProps}
                        % properties are mapped (
                        {ms.getNumberOfMappedProperties(ignoreList)}
                        of
                        {ms.getNumberOfProperties(ignoreList)}
                        ).</p>
                    <p align="center">
                        {percentageMappedPropOccurrences}
                        % of all property occurrences in Wikipedia (
                        {langCode}
                        ) are mapped (
                        {ms.getNumberOfMappedPropertyOccurrences(ignoreList)}
                        of
                        {ms.getNumberOfPropertyOccurrences(ignoreList)}
                        ).</p>
                    <table align="center">
                    <caption>The color codes:</caption>
                    <tr>
                        <td bgcolor={mappedColor}>property is mapped</td>
                    </tr>
                    <tr>
                        <td bgcolor={notMappedColor}>property is not mapped</td>
                    </tr>
                    <tr>
                        <td bgcolor={notDefinedColor}>property is mapped but not found in the template definition</td>
                    </tr>
                    <tr>
                        <td bgcolor={ignoreColor}>property is ignored</td>
                    </tr>
                    </table>
                    <table align="center">
                        <tr>
                            <td>occurrences</td> <td>property</td>
                        </tr>
                        {
                        for ((name, (occurrences, isMapped)) <- sortedPropMap) yield
                        {
                            var bgcolor: String = ""
                            if (isMapped)
                            {
                                bgcolor = mappedColor
                            }
                            else
                            {
                                bgcolor = notMappedColor
                            }

                            var counter = ""
                            if (occurrences == -1)
                            {
                                bgcolor = notDefinedColor
                                counter = "na"
                            }
                            else counter = occurrences.toString

                            var isIgnored: Boolean = false
                            var ignoreMsg: String = "add to ignore list"
                            if (ignoreList.isPropertyIgnored(wikiDecode(template), name))
                            {
                                isIgnored = true
                                ignoreMsg = "remove from ignore list"
                                bgcolor = ignoreColor
                            }

                            <tr bgcolor={bgcolor}>
                                <td align="right">
                                <a name={urlEncode(name)}/>
                                    {counter}
                                </td> <td>
                                {name}
                            </td>
                                {if (Server.adminRights(password))
                                {
                                    <td>
                                        <a href={"../../ignore/"+langCode+"/property/"+passwordQuery+"&ignore="+(! isIgnored)+"&template="+template+"&property="+name}>
                                            {ignoreMsg}
                                        </a>
                                    </td>
                                }}
                            </tr>
                        }
                        }
                    </table>
                </body>
            </html>
        }
    }
    
    def getMappingStats(templateName: String) : MappingStats =
    {
        for (mappingStat <- statistics) if (mappingStat.templateName == templateName) return mappingStat
        null
    }

    def getClassMappings =
    {
        val mappings = Server.extractor.mappings(language)
        mappings.templateMappings ++ mappings.conditionalMappings
    }
}