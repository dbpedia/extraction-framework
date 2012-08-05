package org.dbpedia.extraction.util

import java.util.logging.Logger
import java.io.IOException
import scala.xml.{XML, Elem}
import org.dbpedia.extraction.wikiparser.{WikiTitle,Namespace}
import org.dbpedia.extraction.sources.WikiPage
import java.net.{URLEncoder, URL}
import WikiApi._

object WikiApi
{
  /** name of api.php parameter for page IDs */
  val PageIDs = "pageids"
  
  /** name of api.php parameter for revision IDs */
  val RevisionIDs = "revids"
}

/**
 * Executes queries to the MediaWiki API.
 * 
 * TODO: replace this class by code adapted from WikiDownloader. 
 *
 * @param url The URL of the MediaWiki API e.g. http://en.wikipedia.org/w/api.php.
 * @param language The language of the MediaWiki.
 */
class WikiApi(url: URL, language: Language)
{
    private val logger = Logger.getLogger(classOf[WikiApi].getName)

    /** The number of retries before a query is considered as failed */
    private val maxRetries = 10

    /** The number of pages, which are listed per request. MediaWikis usually limit this to a maximum of 500. */
    private val pageListLimit = 500

    /** The number of pages which are downloaded per request. MediaWikis usually limit this to a maximum of 50. */
    private val pageDownloadLimit = 50

    /**
     * Retrieves all pages with a specific namespace starting from a specific page.
     *
     * @param namespace The namespace of the requested pages.
     * @param fromPage The page title to start enumerating from.
     * @param f The function to be called on each page.
     */
    def retrievePagesByNamespace[U](namespace : Namespace, f : WikiPage => U, fromPage : String = "")
    {
        // TODO: instead of first getting the page ids and then the pages, use something like 
        // ?action=query&generator=allpages&prop=revisions&rvprop=ids|content&format=xml&gapnamespace=0
        // -> "generator" instead of "list" and "gapnamespace" instead of "apnamespace" ("gap" is for "generator all pages")
 
        //Retrieve list of pages
        val response = query("?action=query&format=xml&list=allpages&apfrom=" + fromPage + "&aplimit=" + pageListLimit + "&apnamespace=" + namespace.code)

        //Extract page ids
        val pageIds = for(p <- response \ "query" \ "allpages" \ "p") yield (p \ "@pageid").head.text.toLong

        //Retrieve pages
        retrievePagesByPageID(pageIds).foreach(f)

        //Retrieve remaining pages
        for(continuePage <- response \ "query-continue" \ "allpages" \ "@apfrom" headOption)
        {
            // TODO: use iteration instead of recursion
            retrievePagesByNamespace(namespace, f, continuePage.text)
        }
    }


    /**
     * Retrieves multiple pages by their page ID.
     *
     * @param pageIds The page IDs of the pages to be downloaded.
     */
    def retrievePagesByPageID[U](pageIds : Iterable[Long]): Traversable[WikiPage] =
    {
      retrievePagesByID(PageIDs, pageIds)
    }
    
    /**
     * Retrieves multiple pages by their revision ID.
     *
     * @param revisionIds The revision IDs of the pages to be downloaded.
     */
    def retrievePagesByRevisionID[U](revisionIds: Iterable[Long]): Traversable[WikiPage] =
    {
      retrievePagesByID(RevisionIDs, revisionIds)
    }
    
    /**
     * Retrieves multiple pages by page or revision IDs.
     *
     * @param param WikiApi.PageIDs ("pageids") or WikiApi.RevisionIDs ("revids") 
     * @param ids page or revision IDs of the pages to be downloaded.
     */
    def retrievePagesByID[U](param: String, ids : Iterable[Long]) = new Traversable[WikiPage]
    {
        override def foreach[U](proc : WikiPage => U) : Unit =
        {
            for(group <- ids.grouped(pageDownloadLimit))
            {
                val response = query("?action=query&format=xml&prop=revisions&"+param+"=" + group.mkString("|") + "&rvprop=ids|content|timestamp")
                processPages(response, proc)
            }
        }
    }

    /**
     * Retrieves multiple pages by their title.
     *
     * @param pageIds The titles of the pages to be downloaded.
     */
    def retrievePagesByTitle[U](titles : Iterable[WikiTitle]) = new Traversable[WikiPage]
    {
        override def foreach[U](proc : WikiPage => U) : Unit =
        {
            for(titleGroup <- titles.grouped(pageDownloadLimit))
            {
                val response = query("?action=query&format=xml&prop=revisions&titles=" + titleGroup.map(_.encodedWithNamespace).mkString("|") + "&rvprop=ids|content|timestamp")
                processPages(response, proc)
            }
        }
    }
    
    def processPages[U](response : Elem, proc : WikiPage => U) : Unit =
    {
        for(page <- response \ "query" \ "pages" \ "page";
            rev <- page \ "revisions" \ "rev" )
        {
            proc( new WikiPage( title     = WikiTitle.parse((page \ "@title").head.text, language),
                             redirect  = null, // TODO: read redirect from XML
                             id        = (page \ "@pageid").head.text,
                             revision  = (rev \ "@revid").head.text,
                             timestamp = (rev \ "@timestamp").head.text,
                             source    = rev.text ) )
        }
    }

    /**
     * Retrieves a list of pages which use a given template.
     *
     * @param title The title of the template
     * @param maxCount The maximum number of pages to retrieve
     */
    def retrieveTemplateUsages(title : WikiTitle, maxCount : Int = 500) : Seq[WikiTitle] =
    {
        val response = query("?action=query&format=xml&list=embeddedin&eititle=" + title.encodedWithNamespace + "&einamespace=0&eifilterredir=nonredirects&eilimit=" + maxCount)

        for(page <- response \ "query" \ "embeddedin" \ "ei";
            title <- page \ "@title" )
            yield new WikiTitle(title.text, Namespace.Main, language)
    }

  /**
   * Returns a list of page IDs fo the pages for a certain wiki title
   * @param title The title of the wiki
   * @param maxCount  the maximum number of matches
   * @return  A list of page IDs  
   */
  def retrieveTemplateUsageIDs(title : WikiTitle, maxCount : Int = 500) : List[Long] =
    {
        var pageList = List[Long]();
        var  canContinue = false;
        var eicontinue = "";
        var appropriateQuery = "";

        do{
          appropriateQuery = "?action=query&format=xml&list=embeddedin&eititle=" + title.encodedWithNamespace +
                              "&einamespace=0&eifilterredir=nonredirects&eilimit=" + maxCount;
          //Since the call can return only 500 matches at most we must use the eicontinue parameter to
          //get the other matches
          if(canContinue)
            appropriateQuery = appropriateQuery + "&eicontinue=" + eicontinue;

          val response = query(appropriateQuery);

            val queryContinue = response \ "query-continue" \ "embeddedin";
            val continueSeq = queryContinue \\ "@eicontinue";
            eicontinue = continueSeq.text;

            canContinue = false;

            if((eicontinue != null) && (eicontinue != ""))
              canContinue= true;

            for(page <- response \ "query" \ "embeddedin" \ "ei";
                title <- page \ "@pageid" ){
                 pageList = pageList ::: List(title.text.toLong);
            }
        }while(canContinue)

      pageList;
    }

    /**
     * Executes a query to the MediaWiki API.
     */
    protected def query(params : String) : Elem =
    {
        for(i <- 0 to maxRetries)
        {
            try
            {
                val reader = new URL(url + params).openStream()
                val xml = XML.load(reader)
                reader.close()

                return xml
            }
            catch
            {
                case ex : IOException =>
                {
                    if(i < maxRetries - 1)
                    {
                        logger.fine("Query failed: " + params + ". Retrying...")
                    }
                    else
                    {
                        throw ex
                    }
                }
            }

            Thread.sleep(100)
        }

        throw new IllegalStateException("Should never get there")
    }
}