package org.dbpedia.extraction.util

import java.util.logging.Logger
import java.io.IOException
import java.net.{HttpURLConnection, URL, URLEncoder}

import javax.net.ssl.HttpsURLConnection

import scala.xml.{Elem, Node, XML}
import scala.collection.immutable.Seq
import scala.language.postfixOps
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}
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
        System.getProperty("extract.wikiapi.customUserAgent.text", "curl/7.54")
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
                val response = query("?action=query&continue=&format=xml&prop=revisions%7Cinfo&"+param+"=" + group.mkString("%7C") + "&rvprop=ids%7Ccontent%7Ctimestamp%7Cuser%7Cuserid")
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
                val response = query("?action=query&continue=&format=xml&prop=revisions%7Cinfo&titles=" + titleGroup.map(formatWikiTitle).mkString("%7C") + "&rvprop=ids%7Ccontent%7Ctimestamp%7Cuser%7Cuserid")
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
                    redirect        = null, //WikiTitle.parse((page \ "redirect" \ "@title").text, language),
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
        var pageList = List[Long]()
        var canContinue = false
        var eicontinue = ""

        do{
            var appropriateQuery = "?action=query&continue=&format=xml&list=embeddedin&eititle=" + title.encodedWithNamespace +
                                "&einamespace=0&eifilterredir=nonredirects&eilimit=" + maxCount

            if(canContinue)
                appropriateQuery = appropriateQuery + "&eicontinue=" + eicontinue

            val response = query(appropriateQuery)
            val queryContinue = response \ "query-continue" \ "embeddedin"
            val continueSeq = queryContinue \\ "@eicontinue"
            eicontinue = continueSeq.text

            canContinue = eicontinue != null && eicontinue.nonEmpty

            for(page <- response \ "query" \ "embeddedin" \ "ei";
                title <- page \ "@pageid" ){
                pageList = pageList ::: List(title.text.toLong)
            }
        } while(canContinue)

        pageList
    }

    /**
     * Checks if the file exists in the mediawiki instance specified by the language.
     *
     * @param fileName the name of the file whose existence has to be checked
     * @param language the language which specifies the mediawiki instance to check on
     * @return true iff the file exists else false
     */
    def fileExistsOnWiki(fileName : String, language: Language): Boolean = {
        try {
            val fileNamespaceIdentifier = Namespace.File.name(language)
            val queryURLString = language.baseUri.replace("http:", "https:") + "/wiki/" + fileNamespaceIdentifier + ":" + URLEncoder.encode(fileName, "UTF-8")
            val connection = new URL(queryURLString).openConnection().asInstanceOf[HttpsURLConnection]
            connection.setRequestMethod("HEAD")
            connection.setConnectTimeout(10000)
            connection.setReadTimeout(10000)
            val responseCode = connection.getResponseCode()
            connection.disconnect()
            responseCode == HttpURLConnection.HTTP_OK
        } catch {
            case _: Exception => false
        }
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
     * Executes a query to the MediaWiki API with improved error handling.
     *
     * FIXED: HTTP blocking issues with Wikipedia API
     * PROBLEM: Getting connection timeouts, HTTP 403 responses, and blocked requests
     * ROOT CAUSE: Apache HttpClient was missing User-Agent header, causing Wikipedia to block bot requests
     *
     * SOLUTIONS DISCUSSED:
     *   1. Johannes's approach: Fix Apache HttpClient by adding proper User-Agent headers (IMPLEMENTED)
     *   2. URLConnection replacement: Replace HttpClient entirely with URLConnection (available as backup)
     *
     * The User-Agent header identifies the bot properly and prevents Wikipedia from treating requests as malicious.
     */
    protected def query(params : String) : Elem =
    {
        for(i <- 0 to maxRetries)
        {
            // OLD BROKEN CODE (kept for reference - caused Wikipedia blocking):
            // import org.apache.http.impl.client.HttpClients
            // import org.apache.http.client.methods.HttpGet
            // val client = HttpClients.createDefault
            // val request = new HttpGet(new URL(url + params).toString)
            // val response = client.execute(request)  // <-- NO User-Agent header = BLOCKED/TIMEOUT
            // val xml = XML.load(response.getEntity.getContent)

            // NEW WORKING CODE with Johannes's User-Agent fix:
            import org.apache.http.impl.client.HttpClients
            import org.apache.http.client.methods.HttpGet
            import org.apache.http.client.config.RequestConfig
            val client = HttpClients.createDefault

            try
            {
                val fullUrl = url + params
                logger.fine(s"Querying MediaWiki API: $fullUrl")

                val request = new HttpGet(fullUrl)

                // JOHANNES'S FIX: Set proper User-Agent header to prevent Wikipedia blocking
                request.setHeader("User-Agent", "DBpediaBot/1.0 (https://github.com/dbpedia/extraction-framework; dbpedia@infai.org) DBpedia/4.0")
                request.setHeader("Accept", "application/xml, text/xml, */*")

                // ADDITIONAL IMPROVEMENT: Set timeouts via RequestConfig
                val config = RequestConfig.custom()
                    .setConnectTimeout(15000)      // 15 seconds
                    .setSocketTimeout(30000)       // 30 seconds
                    .setConnectionRequestTimeout(10000) // 10 seconds to get connection from pool
                    .build()
                request.setConfig(config)

                val response = client.execute(request)

                try {
                    val xml = XML.load(response.getEntity.getContent)
                    return xml
                } finally {
                    response.close()
                }

            }
            catch
            {
                case ex: org.apache.http.conn.ConnectTimeoutException =>
                {
                    logger.warning(s"Connection timeout on attempt ${i+1} for: $params")

                    if(i < maxRetries)
                    {
                        val sleepTime = Math.min(1000 * (i + 1), 5000) // Progressive backoff, max 5 seconds
                        logger.fine(s"Retrying in ${sleepTime}ms...")
                        Thread.sleep(sleepTime)
                    } else {
                        logger.severe(s"All $maxRetries attempts timed out for: $params")
                        throw ex
                    }
                }
                case ex: java.net.SocketTimeoutException =>
                {
                    logger.warning(s"Socket timeout on attempt ${i+1} for: $params")

                    if(i < maxRetries)
                    {
                        val sleepTime = Math.min(1000 * (i + 1), 5000) // Progressive backoff, max 5 seconds
                        logger.fine(s"Retrying in ${sleepTime}ms...")
                        Thread.sleep(sleepTime)
                    } else {
                        logger.severe(s"All $maxRetries attempts timed out for: $params")
                        throw ex
                    }
                }
                case ex: Exception =>
                {
                    logger.warning(s"Query attempt ${i+1} failed: ${ex.getMessage}")

                    if(i < maxRetries)
                    {
                        val sleepTime = Math.min(1000 * (i + 1), 3000) // Progressive backoff, max 3 seconds
                        logger.fine(s"Query failed: $params. Retrying in ${sleepTime}ms...")
                        Thread.sleep(sleepTime)
                    } else {
                        logger.severe(s"All $maxRetries attempts failed for: $params")
                        throw ex
                    }
                }
            }
            finally {
                client.close()
            }
        }

        throw new IllegalStateException("Should never reach this point")
    }

    /**
     * Alternative URLConnection implementation (Haniya's September 2024 solution)
     *
     * BACKGROUND: This was the initial fix when HttpClient was completely failing.
     * Instead of fixing HttpClient, this replaces it entirely with Java's built-in URLConnection.
     *
     * PROS: No external Apache dependencies, simpler code, works reliably
     * CONS: Potentially slower than HttpClient (performance concern from kurzum)
     *
     * STATUS: Kept as backup/fallback approach. Can be used if HttpClient approach fails.
     */
    protected def queryWithURLConnection(params : String) : Elem =
    {
        for(i <- 0 to maxRetries)
        {
            try
            {
                val fullUrl = url + params
                logger.fine(s"Querying MediaWiki API: $fullUrl")

                val connection = new URL(fullUrl).openConnection()

                // Set proper headers
                connection.setRequestProperty("User-Agent", "DBpediaBot/1.0 (https://github.com/dbpedia/extraction-framework; dbpedia@infai.org) DBpedia/4.0")
                connection.setRequestProperty("Accept", "application/xml, text/xml, */*")
                connection.setConnectTimeout(30000) // 30 seconds
                connection.setReadTimeout(60000)    // 60 seconds

                val inputStream = connection.getInputStream

                // More efficient reading without StringBuilder
                val reader = new java.io.InputStreamReader(inputStream, "UTF-8")
                val bufferedReader = new java.io.BufferedReader(reader)

                try {
                    val xml = XML.load(bufferedReader)
                    return xml
                } finally {
                    bufferedReader.close()
                    inputStream.close()
                }

            }
            catch
            {
                case ex: java.net.SocketTimeoutException =>
                {
                    logger.warning(s"Timeout on attempt ${i+1} for: $params")

                    if(i < maxRetries)
                    {
                        val sleepTime = Math.min(1000 * (i + 1), 5000) // Progressive backoff, max 5 seconds
                        logger.fine(s"Retrying in ${sleepTime}ms...")
                        Thread.sleep(sleepTime)
                    } else {
                        logger.severe(s"All $maxRetries attempts timed out for: $params")
                        throw ex
                    }
                }
                case ex: Exception =>
                {
                    logger.warning(s"Query attempt ${i+1} failed: ${ex.getMessage}")

                    if(i < maxRetries)
                    {
                        val sleepTime = Math.min(1000 * (i + 1), 3000) // Progressive backoff, max 3 seconds
                        logger.fine(s"Query failed: $params. Retrying in ${sleepTime}ms...")
                        Thread.sleep(sleepTime)
                    } else {
                        logger.severe(s"All $maxRetries attempts failed for: $params")
                        throw ex
                    }
                }
            }
        }

        throw new IllegalStateException("Should never reach this point")
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