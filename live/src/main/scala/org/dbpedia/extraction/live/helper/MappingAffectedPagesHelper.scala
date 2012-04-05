package org.dbpedia.extraction.live.helper

import java.net.URL
import org.dbpedia.extraction.util.{Language, WikiApi}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.live.util.XMLUtil
import org.dbpedia.extraction.sources.{XMLSource, Source}
import org.dbpedia.extraction.live.extraction.LiveExtractionManager
import xml.{XML, Elem}
import java.util.PriorityQueue
import org.dbpedia.extraction.live.priority.PagePriority;
import org.dbpedia.extraction.live.priority.Priority;
import org.dbpedia.extraction.live.main._;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 3:37:09 PM
 * This object is used to help the mapping feeder to process the list of IDs of the pages that are affected by the
 * mapping.
 */

object MappingAffectedPagesHelper {
def GetMappingPages(src : Source, lastResponseDate :String ): Unit ={

    src.foreach(CurrentWikiPage =>
      {
          val mappingTitle = WikiTitle.parse(CurrentWikiPage.title.toString, Language.Default)
          val templateTitle = new WikiTitle(mappingTitle.decoded, Namespace.Template, Language.Default)

          //val wikiApiUrl = new URL("http://" + Language.Default.wikiCode + ".wikipedia.org/w/api.php")
          val wikiApiUrl = new URL("http://live.dbpedia.org/syncw/api.php")
          val api = new WikiApi(wikiApiUrl, Language.Default)

          val pageIDs = api.retrieveTemplateUsageIDs(templateTitle, 500);
          var NumberOfPagesToBeInvalidated = 0;
          pageIDs.foreach(CurrentPageID => {
            //val CurrentPageID = wikititle.toLong;
            Main.pageQueue.add(new PagePriority(CurrentPageID, Priority.MappingPriority, lastResponseDate));

            //We add the pageID here immediately without checking if it exist first, as put checks if it exists the old value will be replace,
            //so if it does not exist already it will be added, if it was added with lower priority, its priority will increase, and if it already exists with
            //with the same priority then no change
            Main.existingPagesTree.put(CurrentPageID, true);//Also insert it into the TreeMap, so it will not be double-processed
          }
        );
        //println("The size of the pageQueue = "+ Main.pageQueue.size())
        //println(Main.pageQueue)
      }
    );
  }
}