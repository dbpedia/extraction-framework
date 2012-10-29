package org.dbpedia.extraction.live.helper

import java.net.URL
import org.dbpedia.extraction.util.{Language, WikiApi}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{Source}
import xml.{XML, Elem}

import org.dbpedia.extraction.live.core.LiveOptions


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 3:37:09 PM
 * This object is used to help the mapping feeder to process the list of IDs of the pages that are affected by the
 * mapping.
 */

object MappingAffectedPagesHelper {
  def GetMappingPages(src : Source) : List[Long] = {

    val langCode = LiveOptions.options.get("language")
    val mappingNamespace = "Mapping_" + langCode + ":"
    val language = Language.apply(langCode)
    var idList : List[Long]  = List()

    src.foreach(CurrentWikiPage =>
    {
        if (CurrentWikiPage.title.encodedWithNamespace.startsWith(mappingNamespace)) {

          val templateTitle = new WikiTitle(CurrentWikiPage.title.decoded, Namespace.Template, language)
          val wikiApiUrl = new URL(LiveOptions.options.get("localApiURL"))
          val api = new WikiApi(wikiApiUrl, language)
          idList = idList :::  api.retrieveTemplateUsageIDs(templateTitle);
        }
    });
    idList
  }
}