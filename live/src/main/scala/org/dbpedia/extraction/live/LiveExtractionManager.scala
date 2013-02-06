package org.dbpedia.extraction.live.extraction


import java.io._
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.sources.XMLSource


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
      LiveExtractionConfigLoader.startExtraction(articlesSource);
    }

}