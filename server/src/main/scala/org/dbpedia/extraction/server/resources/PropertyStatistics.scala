package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import org.dbpedia.extraction.server.Server
import collection.immutable.ListMap
import scala.collection.mutable
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.{WikiUtil, Language}
import org.dbpedia.extraction.util.WikiUtil.{wikiDecode,wikiEncode}
import org.dbpedia.extraction.server.stats.MappingStats
import org.dbpedia.extraction.server.util.StringUtils.urlEncode
import java.io.{FileNotFoundException, File}

/**
 * Displays the statistics for the properties of one templates.
 * 
 * TODO: Some URLs contain spaces. We should convert spaces to underscores in most cases, but in
 * some cases we have to use %20.
 */
@Path("/templatestatistics/{lang}/")
class PropertyStatistics(@PathParam("lang") langCode: String, @QueryParam("template") var template: String, @QueryParam("p") password: String)
{
    // get canonical template name
    template = wikiDecode(template)
    
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if (! Server.instance.managers.contains(language)) throw new WebApplicationException(new Exception("language "+langCode+" not defined in server"), 404)

    private val mappingUrlPrefix = Server.instance.paths.pagesUrl+"/"+Namespace.mappings(language).name(language).replace(' ','_')+":"

    private val manager = Server.instance.managers(language)

    private val mappedSuccessClass = "success"
    private val mappedDangerClass ="danger"
    private val ignoreEmptyClass = ""
    private val notDefinedInfoClass = "info"

    private def passwordQuery : String = if (Server.instance.adminRights(password)) "?p="+password else ""

    @GET
    @Produces(Array("application/xhtml+xml"))
    def get = {
        val ms = getMappingStats(template)
        val sortedProps = ms.properties.toList.sortBy{ case (name, (count, mapped)) => (- count, name) }

        val percentageMapped: String = "%2.2f".format(ms.mappedPropertyRatio * 100)
        val percentageMappedUse: String = "%2.2f".format(ms.mappedPropertyUseRatio * 100)
        Server.logger.fine("ratioTemp: " + percentageMapped)
        Server.logger.fine("ratioTempUses: " + percentageMappedUse)
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {ServerHeader.getHeader(s"Statistics for template $template",true)}
        <body>
            <h2 align="center">Statistics for template <a href={language.baseUri + "/wiki/" + manager.templateNamespace + wikiEncode(template)}>{template}</a> and its <a href={mappingUrlPrefix + wikiEncode(template)}>DBpedia mapping</a></h2>
            <p align="center">
                {percentageMapped}
                % properties are mapped (
                {ms.mappedPropertyCount}
                of
                {ms.propertyCount}
                ).</p>
            <p align="center">
                {percentageMappedUse}
                % of all property occurrences in Wikipedia (
                {langCode}
                ) are mapped (
                {ms.mappedPropertyUseCount}
                of
                {ms.propertyUseCount}
                ).</p>
            <table class="table table-condensed" style="width:500px; margin:auto">
            <caption>The color codes:</caption>
            <tr>
                <td class={mappedSuccessClass}>property is mapped</td>
            </tr>
            <tr>
                <td class={mappedDangerClass}>property is not mapped</td>
            </tr>
            <tr>
                <td class={notDefinedInfoClass}>property is mapped but not found in the template definition</td>
            </tr>
            <tr>
                <td class={ignoreEmptyClass}>property is ignored</td>
            </tr>
            </table>
           <table class="tablesorter table myTable table-condensed" style="width:500px; margin:auto;margin-top:10px">
             <thead>
                <tr>
                    <th>occurrences</th> <th>property</th>
                </tr>
             </thead>
             <tbody>
                {
            for ((name, (count, mapped)) <- sortedProps) yield
            {
                var backgroundClass: String = ""
                if (mapped)
                {
                    backgroundClass = mappedSuccessClass
                }
                else
                {
                    backgroundClass = mappedDangerClass
                }

                var counter = ""
                if (count == MappingStats.InvalidTarget)
                {
                    backgroundClass = notDefinedInfoClass
                    counter = "na"
                }
                else counter = count.toString

                var isIgnored: Boolean = false
                var ignoreMsg: String = "add to ignore list"
                if (manager.ignoreList.isPropertyIgnored(wikiDecode(template), name))
                {
                    isIgnored = true
                    ignoreMsg = "remove from ignore list"
                    backgroundClass = ignoreEmptyClass
                }

                <tr class={backgroundClass}>
                        <td align="right">
                        <a name={urlEncode(name)}/>
                            {counter}
                        </td> <td>
                        {name}
                    </td>
                        {if (Server.instance.adminRights(password))
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
             </tbody>
            </table>
        </body>
        </html>
    }
    
    def getMappingStats(templateName: String) : MappingStats =
    {
        val statistics = manager.holder.mappedStatistics
        for (mappingStat <- statistics) if (mappingStat.templateName == templateName) return mappingStat
        throw new IllegalArgumentException("Could not find template: " + templateName)
    }
}