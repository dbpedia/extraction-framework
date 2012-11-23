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

  def extractFromPage(Element :scala.xml.Elem)
    {
      val articlesSource = XMLSource.fromOAIXML(Element);
      extract(articlesSource);
    }

  def extractFromPageID(pageID :Long, apiURL :String, landCode :String)
    {
      val articlesSource = WikiSource.fromPageIDs(List(pageID), new URL(apiURL), Language.apply(landCode));
      extract(articlesSource);
    }

  def extract(source :Source)
      {
        LiveExtractionConfigLoader.startExtraction(source);
      }
}