package org.dbpedia.extraction.server.resources

import javax.ws.rs._
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil.{wikiDecode,wikiEncode}
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
class   TemplateStatistics(@PathParam("lang") langCode: String, @QueryParam("p") password: String, @QueryParam("show") @DefaultValue("20") show: Int = 20)
{
  @GET
  @Produces(Array("application/xhtml+xml"))
  def get: Elem = {
    if (langCode == "*") allLanguages else singleLanguage
  }
      
  /**
   * Displays the statistics for all languages.
   */
  private def allLanguages: Elem =
  {
    var templateCount = 0L
    var mappedTemplateCount = 0L
  
    var templateUseCount = 0L
    var mappedTemplateUseCount = 0L
  
    var propertyCount = 0L
    var mappedPropertyCount = 0L
    
    var propertyUseCount = 0L
    var mappedPropertyUseCount = 0L
      
    for ((language, manager) <- Server.instance.managers) {
      
      val holder = manager.holder
      
      templateCount += holder.templateCount
      mappedTemplateCount += holder.mappedTemplateCount
    
      templateUseCount += holder.templateUseCount
      mappedTemplateUseCount += holder.mappedTemplateUseCount
    
      propertyCount += holder.propertyCount
      mappedPropertyCount += holder.mappedPropertyCount
      
      propertyUseCount += holder.propertyUseCount
      mappedPropertyUseCount += holder.mappedPropertyUseCount
    }
  
    val mappedTemplateRatio = mappedTemplateCount.toDouble / templateCount.toDouble
    val mappedPropertyRatio = mappedPropertyCount.toDouble / propertyCount.toDouble
    
    val mappedTemplateUseRatio = mappedTemplateUseCount.toDouble / templateUseCount.toDouble
    val mappedPropertyUseRatio = mappedPropertyUseCount.toDouble / propertyUseCount.toDouble
    
    val percentageMappedTemplates: String = "%2.2f".format(mappedTemplateRatio * 100)
    val percentageMappedProperties: String = "%2.2f".format(mappedPropertyRatio * 100)
    val percentageMappedTemplateUse: String = "%2.2f".format(mappedTemplateUseRatio * 100)
    val percentageMappedPropertyUse: String = "%2.2f".format(mappedPropertyUseRatio * 100)
    
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
      {ServerHeader.getHeader("Mapping Statistics for all languages")}
    <body>
      <h2 align="center">Mapping Statistics for <u>all languages</u></h2>
      <p align="center">{percentageMappedTemplates} % of all templates are mapped ({mappedTemplateCount} of {templateCount}).</p>
      <p align="center">{percentageMappedProperties} % of all properties are mapped ({mappedPropertyCount} of {propertyCount}).</p>
      <p align="center">{percentageMappedTemplateUse} % of all template occurrences in Wikipedia are mapped ({mappedTemplateUseCount} of {templateUseCount}).</p>
      <p align="center">{percentageMappedPropertyUse} % of all property occurrences in Wikipedia are mapped ({mappedPropertyUseCount} of {propertyUseCount}).</p>
    </body>
    </html>
  }
  
  private def singleLanguage: Elem = {
      
    val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language " + langCode), 404))

    if (! Server.instance.managers.contains(language)) throw new WebApplicationException(new Exception("language " + langCode + " not defined in server"), 404)

    val manager = Server.instance.managers(language)

    val statsHolder = manager.holder
    
    val sortedStats = statsHolder.mappedStatistics.sortBy(ms => (- ms.templateCount, ms.templateName))

    val reversedRedirects = statsHolder.reversedRedirects
    val percentageMappedTemplates: String = "%2.2f".format(statsHolder.mappedTemplateRatio * 100)
    val percentageMappedProperties: String = "%2.2f".format(statsHolder.mappedPropertyRatio * 100)
    val percentageMappedTemplateUse: String = "%2.2f".format(statsHolder.mappedTemplateUseRatio * 100)
    val percentageMappedPropertyUse: String = "%2.2f".format(statsHolder.mappedPropertyUseRatio * 100)

    val mappingUrlPrefix = Server.instance.paths.pagesUrl+"/"+Namespace.mappings(language).name(Language.Mappings).replace(' ','_')+":"

    val mappedSuccessClass = "success"
    val mappedActiveClass ="active"
    val mappedWarningClass = "warning"
    var mappedDangerClass = "danger"

    val goodThreshold = 0.8
    val mediumThreshold = 0.4

    val renameInfoClass = "info"
    val ignoreEmptyClass = ""

    // TODO: stream xml to browser. We produce up to 10MB HTML. XML in memory is even bigger.
      
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {ServerHeader.getHeader(s"Mapping Statistics for $langCode", true)}
            <body>
                <h2 align="center">Mapping Statistics for <u>{langCode}</u></h2>
                <p align="center">{percentageMappedTemplates} % of all templates in Wikipedia ({langCode}) are mapped 
                ({statsHolder.mappedTemplateCount} of {statsHolder.templateCount}).</p>
                <p align="center">{percentageMappedProperties} % of all properties in Wikipedia ({langCode}) are mapped 
                ({statsHolder.mappedPropertyCount} of {statsHolder.propertyCount}).</p>
                <p align="center">{percentageMappedTemplateUse} % of all template occurrences in Wikipedia ({langCode}) are mapped 
                ({statsHolder.mappedTemplateUseCount} of {statsHolder.templateUseCount}).</p>
                <p align="center">{percentageMappedPropertyUse} % of all property occurrences in Wikipedia ({langCode}) are mapped
                ({statsHolder.mappedPropertyUseCount} of {statsHolder.propertyUseCount}).</p>

                <table class="table table-condensed" align="center" style="width:500px; margin:auto">
                    <caption>The color codes:</caption>
                    <tr><td class={mappedSuccessClass}>template is mapped withl more than {"%2.0f".format(goodThreshold*100)}%</td></tr>
                    <tr><td class={mappedActiveClass}>template is mapped with more than {"%2.0f".format(mediumThreshold*100)}%</td></tr>
                    <tr><td class={mappedWarningClass}>template is mapped with less than {"%2.0f".format(mediumThreshold*100)}%</td></tr>
                    <tr><td class={mappedDangerClass}>template is not mapped</td></tr>
                    <tr><td class={renameInfoClass}>template mapping must be renamed</td></tr>
                    <tr><td class={ignoreEmptyClass}>template is on the ignorelist (is not an infobox that contains relevant properties)</td></tr>
                </table>
                { templateCountLinks }
        <table class="tablesorter table myTable table-condensed" align="center" style="width: 70%; margin:auto">
          <thead>
          <tr>
            <th>occurrences</th> <th colspan="2">template (with link to property statistics)</th>
            <th>num properties</th> <th>mapped properties (%)</th>
            <th>num property occurrences</th> <th>mapped property occurrences (%)</th> <th class="sorter-false"></th>
          </tr>
          </thead>
          <tbody>
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
            var mappingsWikiLink = mappingUrlPrefix + wikiEncode(mappingStat.templateName)
            
            var backgroundClass: String =
            if (! mappingStat.isMapped) mappedDangerClass
            else if (mappingStat.mappedPropertyUseRatio > goodThreshold) mappedSuccessClass
            else if(mappingStat.mappedPropertyUseRatio > mediumThreshold) mappedActiveClass
            else mappedWarningClass

            var mustRename = false
            var redirectMsg = ""
            for (redirect <- targetRedirect) {
              if (mappingStat.isMapped) {
                  //redirectMsg = " NOTE: the mapping for " + wikiDecode(redirect, language).substring(createMappingStats.templateNamespace.length()) + " is redundant!"
              } else {
                  mappingsWikiLink = mappingUrlPrefix + redirect.substring(manager.templateNamespace.length)
                  backgroundClass = renameInfoClass
                  mustRename = true
                  redirectMsg = "Mapping of " + redirect.substring(manager.templateNamespace.length) + " must be renamed to "
              }
            }
  
            var isIgnored = false
            var ignoreMsg = "add to ignore list"
            if (manager.ignoreList.isTemplateIgnored(mappingStat.templateName))
            {
                isIgnored = true
                ignoreMsg = "remove from ignore list"
                backgroundClass = ignoreEmptyClass
            }
          <tr class={backgroundClass}>
          <td align="right"><a name={urlEncode(mappingStat.templateName)}/>{mappingStat.templateCount}</td>
          { if (mustRename) {
          <td>
            {redirectMsg}
            <a href={"../../templatestatistics/"+langCode+"/?template="+wikiEncode(mappingStat.templateName)+cookieQuery('&')}>
              {mappingStat.templateName}
            </a>
          </td>
          } else {
          <td>
            <a href={"../../templatestatistics/"+langCode+"/?template="+wikiEncode(mappingStat.templateName)+cookieQuery('&')}>
              {mappingStat.templateName}
            </a>
            {redirectMsg}
          </td>
          } }
          <td><a href={mappingsWikiLink}>Edit</a></td>
          <td align="right">{mappingStat.propertyCount}</td> 
          <td align="right">{percentMappedProps}</td>
          <td align="right">{mappingStat.propertyUseCount}</td>
          <td align="right">{percentMappedPropOccur}</td>
          { if (Server.instance.adminRights(password)) {
          <td>
            <a href={"../../ignore/"+langCode+"/template/?ignore="+(! isIgnored)+"&template="+wikiEncode(mappingStat.templateName)+cookieQuery('&', show)}>
              {ignoreMsg}
            </a>
          </td>
          } }
          </tr>
          }
        }
       </tbody>
      </table>
      { templateCountLinks }
    </body>
    </html>
  }
    
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
    
    sb.toString
  }
  
  private def templateCountLinks: Elem = {
    <p align="center">Show 
      <a href={cookieQuery('?', 20)}>top&nbsp;20</a> |
      <a href={cookieQuery('?', 100)}>top&nbsp;100</a> |
      <a href={cookieQuery('?', 100000)}>all&nbsp;templates</a>
    </p>
  }
  
}