package org.dbpedia.extraction.live.extraction


import java.io._
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.sources.{Source, WikiSource, XMLSource}
import java.net.URL
import org.dbpedia.extraction.util.Language


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 26, 2010
 * Time: 4:15:28 PM
 * This is the main class responsible for managing the process of live extraction.
 */

object LiveExtractionManager
{
  var wiki : WikiTitle = null;
  var oaiID : String = "";
  var Num=1;


  def extractFromPageID(pageID :Long, apiURL :String, landCode :String)
    {
      val lang = Language.apply(landCode)
      val articlesSource = WikiSource.fromPageIDs(List(pageID), new URL(apiURL), lang);
      extract(articlesSource,lang);
    }

  def extract(source :Source, language: Language)
      {
        LiveExtractionConfigLoader.startExtraction(source, language);
      }
}