package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.server.stats.MappingStats
import org.dbpedia.extraction.server.util.StringUtils.urlEncode
import java.net.URI
import java.io.PrintWriter
import scala.xml.Elem

/**
 * Displays the statistics for all templates of a language.
 * 
 * TODO: Some URLs contain spaces. We should convert spaces to underscores in most cases, but in
 * some cases we have to use %20.
 */
@Path("/statistics/{lang}/")
class TemplateStatistics(@PathParam("lang") langCode: String, @QueryParam("p") password: String, @QueryParam("show") @DefaultValue("20") show: Int = 20)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (! Server.instance.managers.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    private val manager = Server.instance.managers(language)

    private val statsHolder = manager.holder
    
    private val sortedStats = statsHolder.mappedStatistics.sortBy(ms => (- ms.templateCount, ms.templateName))

    private val reversedRedirects = statsHolder.reversedRedirects
    private val percentageMappedTemplates: String = "%2.2f".format(statsHolder.mappedTemplateRatio * 100)
    private val percentageMappedTemplateOccurrences: String = "%2.2f".format(statsHolder.mappedTemplateUseRatio * 100)
    private val percentageMappedPropertyOccurrences: String = "%2.2f".format(statsHolder.mappedPropertyUseRatio * 100)

    private val mappingUrlPrefix = Server.instance.paths.pagesUrl+"/"+Namespace.mappings(language).getName(Language.Mappings).replace(' ','_')+":"

    private val mappedGoodColor = "#65c673"
    private val mappedMediumColor = "#ecea48"
    private val mappedBadColor = "#e0ab3a"
    private val notMappedColor = "#df5c56"

    private val goodThreshold = 0.8
    private val mediumThreshold = 0.4

    private val renameColor = "#b03060"
    private val ignoreColor = "#cdcdcd"

    private def cookieQuery(sep: Char, show: Int = -1) : String = {
      var vsep = sep
      
      val sb = new StringBuilder
      
      if (Server.instance.adminRights(password)) {
        sb append vsep append "p=" append password
        vsep = '&'
      }
      
      if (show != -1) { 
        sb append vsep append "show=" append show
        vsep = '&' // for future additions below
      }
      
      sb toString
    }
      
    // TODO: stream xml to browser. We produce up to 10MB HTML. XML in memory is even bigger.
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get = {
      
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
                    {statsHolder.mappedTemplateCount}
                    of
                    {statsHolder.templateCount}
                    ).</p>
                <p align="center">
                    {percentageMappedTemplateOccurrences}
                    % of all template occurrences in Wikipedia (
                    {langCode}
                    ) are mapped (
                    {statsHolder.mappedTemplateUseCount}
                    of
                    {statsHolder.templateUseCount}
                    ).</p>
                <p align="center">
                    {percentageMappedPropertyOccurrences}
                    % of all property occurrences in Wikipedia (
                    {langCode}
                    ) are mapped (
                    {statsHolder.mappedPropertyUseCount}
                    of
                    {statsHolder.propertyUseCount}
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
                { templateCountLinks }
                <table align="center">
                    <tr>
                        <td>occurrences</td> <td colspan="2">template (with link to property statistics)</td>
                        <td>num properties</td> <td>mapped properties (%)</td>
                        <td>num property occurrences</td> <td>mapped property occurrences (%)</td> <td></td>
                    </tr>
                    {
                    var shown = 0
                    // TODO: Solve problem of templates for which no properties are found in the template documentation (e.g. Geobox).
                    for (mappingStat <- sortedStats if /* mappingStat.propertyCount > 0 && */ shown < show) yield
                    {
                        shown += 1
                        val templateName = manager.templateNamespace + mappingStat.templateName
                        val targetRedirect = reversedRedirects.get(templateName)

                        val percentMappedProps: String = "%2.2f".format(mappingStat.mappedPropertyRatio * 100)
                        val percentMappedPropOccur: String = "%2.2f".format(mappingStat.mappedPropertyUseRatio * 100)
                        var mappingsWikiLink = mappingUrlPrefix + mappingStat.templateName
                        var bgcolor: String =
                            if(!mappingStat.isMapped)
                            {
                                notMappedColor
                            }
                            else
                            {
                                if(mappingStat.mappedPropertyUseRatio > goodThreshold)
                                {
                                    mappedGoodColor
                                }
                                else if(mappingStat.mappedPropertyUseRatio > mediumThreshold)
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
                                //redirectMsg = " NOTE: the mapping for " + WikiUtil.wikiDecode(redirect, language).substring(createMappingStats.templateNamespace.length()) + " is redundant!"
                            }
                            else
                            {
                                mappingsWikiLink = mappingUrlPrefix + redirect.substring(manager.templateNamespace.length)
                                bgcolor = renameColor
                                mustRenamed = true
                                redirectMsg = "Mapping of " + redirect.substring(manager.templateNamespace.length) + " must be renamed to "
                            }
                        }

                        var isIgnored: Boolean = false
                        var ignoreMsg: String = "add to ignore list"
                        if (manager.ignoreList.isTemplateIgnored(mappingStat.templateName))
                        {
                            isIgnored = true
                            ignoreMsg = "remove from ignore list"
                            bgcolor = ignoreColor
                        }

                        <tr bgcolor={bgcolor}>
                                    <td align="right">
                                    <a name={urlEncode(mappingStat.templateName)}/>
                                        {mappingStat.templateCount}
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
                                            {mappingStat.propertyCount}
                                        </td> <td align="right">
                                    {percentMappedProps}
                                </td>
                                        <td align="right">
                                            {mappingStat.propertyUseCount}
                                        </td> <td align="right">
                                    {percentMappedPropOccur}
                                </td>
                                    {if (Server.instance.adminRights(password))
                            {
                                <td>
                                        <a href={"../../ignore/"+langCode+"/template/?ignore="+(! isIgnored)+"&template="+mappingStat.templateName+cookieQuery('&', show)}>
                                            {ignoreMsg}
                                        </a>
                                        </td>
                            }}
                                </tr>
                        }
                    }
                </table>
                { templateCountLinks }
            </body>
        </html>
    }
    
    private def templateCountLinks: Elem = {
      <p align="center">Show 
        <a href={cookieQuery('?', 20)}>top&nbsp;20</a> |
        <a href={cookieQuery('?', 100)}>top&nbsp;100</a> |
        <a href={cookieQuery('?', 100000)}>all&nbsp;templates</a>
      </p>
    }
    
}