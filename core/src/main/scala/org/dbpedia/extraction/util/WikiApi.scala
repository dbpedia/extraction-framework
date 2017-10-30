package org.dbpedia.extraction.util

import java.util.logging.Logger
import java.io.IOException
import java.net.{HttpURLConnection, URL, URLEncoder}
import javax.net.ssl.HttpsURLConnection
import scala.xml.{XML, Elem, Node}
import scala.collection.immutable.Seq
import scala.language.postfixOps
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiTitle, Namespace}

import WikiApi._

object WikiApi
{
    /** name of api.php parameter for page IDs */
    val PageIDs = "pageids"

    /** name of api.php parameter for revision IDs */
    val RevisionIDs = "revids"

    /** Specify whether you want to set the user agent for queries to the MediaWiki API */
    private val customUserAgentEnabled =
    try {
        System.getProperty("extract.wikiapi.customUserAgent.enabled", "false").toBoolean
    } catch {
        case ex : Exception => false
    }

    /** Specify a custom user agent for queries to the MediaWiki API */
    private val customUserAgentText =
    try {
        System.getProperty("extract.wikiapi.customUserAgent.text", "DBpedia Extraction Framework")
    } catch {
        case ex : Exception => "DBpedia Extraction Framework"
    }
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
        // ?action=query&continue=&generator=allpages&prop=revisions|info&rvprop=ids|content&format=xml&gapnamespace=0
        // -> "generator" instead of "list" and "gapnamespace" instead of "apnamespace" ("gap" is for "generator all pages")

        //Retrieve list of pages
        val response = query("?action=query&continue=&format=xml&list=allpages&apcontinue=" + URLEncoder.encode(fromPage, "UTF-8") + "&aplimit=" + pageListLimit + "&apnamespace=" + namespace.code)

        //Extract page ids
        val pageIds = for(p <- response \ "query" \ "allpages" \ "p") yield (p \ "@pageid").head.text.toLong

        //Retrieve pages
        retrievePagesByPageID(pageIds).foreach(f)

        //Retrieve remaining pages
        for(continuePage <- response \ "continue" \ "@apcontinue" headOption)
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
                val q ="?action=query&continue=&format=xml&prop=revisions|info&"+param+"=" + group.mkString("|") + "&rvprop=ids|content|timestamp|user|userid"
                val response = query(q)
                processPages(response, proc)
            }
        }
    }

    /**
     * Retrieves multiple pages by their title.
     *
     * @param titles The titles of the pages to be downloaded.
     */
    def retrievePagesByTitle[U](titles : Iterable[WikiTitle]) = new Traversable[WikiPage]
    {
        override def foreach[U](proc : WikiPage => U) : Unit =
        {
            for(titleGroup <- titles.grouped(pageDownloadLimit))
            {
                val response = query("?action=query&continue=&format=xml&prop=revisions|info&titles=" + titleGroup.map(formatWikiTitle).mkString("|") + "&rvprop=ids|content|timestamp|user|userid")
                processPages(response, proc)
            }
        }
    }

    def processPages[U](response : Elem, proc : WikiPage => U) : Unit =
    {
        for(page <- response \ "query" \ "pages" \ "page";
            rev <- page \ "revisions" \ "rev" )
        {
            // "userid" is not supported on older mediawiki versions and the Mapping mediawiki does not support it yet
            // TODO: update mapping mediawiki and assign name & id directly
            val _contributorID = (rev \ "@userid")
            val _contributorName = (rev \ "@user")
            val _format = (rev \ "@contentformat")

            proc(
                new WikiPage(
                    title           = WikiTitle.parse((page \ "@title").head.text, language),
                    //redirect        = null, //WikiTitle.parse((page \ "redirect" \ "@title").text, language),
                    id              = (page \ "@pageid").head.text,
                    revision        = (rev \ "@revid").head.text,
                    timestamp       = (rev \ "@timestamp").head.text,
                    contributorID   = if (_contributorID == null || _contributorID.length != 1) "0" else _contributorID.head.text,
                    contributorName = if (_contributorName == null || _contributorName.length != 1) "" else _contributorName.head.text,
                    source          = rev.text,
                    format          = if (_format == null || _format.length != 1) "" else _format.head.text
                )
            )
        }
    }

    /**
     * Retrieves a list of pages which use a given template.
     *
     * @param title The title of the template
     * @param namespace The namespace to search within
     * @param maxCount The maximum number of pages to retrieve
     */
    def retrieveTemplateUsages(title : WikiTitle, namespace: Namespace = Namespace.Main, maxCount : Int = 500) : Seq[WikiTitle] =
    {
        val response = query("?action=query&continue=&format=xml&list=embeddedin&eititle=" + title.encodedWithNamespace + "&einamespace=" + namespace.code + "&eifilterredir=nonredirects&eilimit=" + maxCount)

        for(page <- response \ "query" \ "embeddedin" \ "ei";
            title <- page \ "@title" )
            yield new WikiTitle(title.text, Namespace.Main, language)
    }

    /**
     * Returns a list of page IDs fo the pages for a certain wiki title
 *
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
            appropriateQuery = "?action=query&continue=&format=xml&list=embeddedin&eititle=" + title.encodedWithNamespace +
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
     * Checks if the file exists in the mediawiki instance specified by the language.
     *
     * @param fileName the name of the file whose existence has to be checked
     * @param language the language which specifies the mediawiki instance to check on
     * @return true iff the file exists else false
     */
    def fileExistsOnWiki(fileName : String, language: Language): Boolean = {
        val fileNamespaceIdentifier = Namespace.File.name(language)

        val queryURLString = language.baseUri.replace("http:", "https:") + "/wiki/" + fileNamespaceIdentifier + ":" + fileName
        val connection = new URL(queryURLString).openConnection().asInstanceOf[HttpsURLConnection]
        connection.setRequestMethod("HEAD")
        val responseCode = connection.getResponseCode()
        connection.disconnect()

        responseCode == HttpURLConnection.HTTP_OK
    }

    /**
     * Retrieves all pages for a given namespace starting from a specific page and returning the next title to continue.
     *
     * @param namespace The namespace code of the requested pages.
     * @param continueString The value to be used in the continue field, which is probably -||
     *                       This value seems to be unused for listing all pages (but nevertheless it is required).
     * @param continueTitle The page title to start (or continue) enumerating from.
     */
    def retrieveAllPagesPerNamespace(namespace : Integer, continueString : String, continueTitle : String) : (String, String, Seq[Node]) =
    {
        val baseURL = "?action=query&format=xml&list=allpages&aplimit=500&apnamespace=%d&continue=%s&apcontinue=%s"
        val response = query(String.format(baseURL, namespace, continueString, continueTitle))
        val apcontinue = response \ "continue" \@ "apcontinue"
        val continue = response \ "continue" \@ "continue"
        val pages = (response \ "query" \ "allpages" \ "p").theSeq
        (apcontinue, continue, pages)
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
                val connection = new URL(url + params).openConnection()
                if (customUserAgentEnabled) {
                    connection.setRequestProperty("User-Agent", customUserAgentText)
                }
                val reader = connection.getInputStream
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

    /**
     * Formats {@param title} to be used with MediaWiki API
     *
     * @param title
     * @return
     */
    private def formatWikiTitle(title: WikiTitle) : String = {
        URLEncoder.encode(title.decodedWithNamespace.replace(' ', '_'), "UTF-8")
    }
}
