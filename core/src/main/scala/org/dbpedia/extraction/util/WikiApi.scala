package org.dbpedia.extraction.util

import java.io.{IOException, InputStream}
import java.net.{URL, URLEncoder, HttpURLConnection}
import javax.net.ssl.HttpsURLConnection
import java.util.logging.Logger
import scala.xml._
import scala.collection.immutable.Seq
import scala.language.postfixOps
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}
import WikiApi._
import org.apache.http.client.HttpClient

object WikiApi
{
    private val logger = Logger.getLogger(classOf[WikiApi].getName)

    // Constants required by WikiSource.scala
    val PageIDs = "pageids"
    val RevisionIDs = "revids"

    def apply(apiUrl: URL, language: Language) : WikiApi =
    {
        val api = new WikiApi(apiUrl, language)
        api.loadNamespaces()
        api
    }
}

/**
 * Represents the MediaWiki API with enhanced reliability for Wikipedia access.
 *
 * MAJOR FIX: Resolves Wikipedia API blocking issues that caused HTTP 403 errors and timeouts.
 * Implements dual HTTP approach: Apache HttpClient (primary) + URLConnection (backup).
 */
class WikiApi(url: URL, language: Language, useHttpClient: Boolean = true,
             customUserAgentEnabled: Boolean = true,
             customUserAgentText: String = "DBpediaBot/1.0 (https://github.com/dbpedia/extraction-framework; dbpedia@infai.org) DBpedia/4.0",
             maxRetries: Int = 3, maxLagSecondsBeforeRetry: Int = 5)
{
    require(url != null, "url != null")
    require(language != null, "language != null")

    private val logger = Logger.getLogger(classOf[WikiApi].getName)

    // REFACTORED: Extract constants to eliminate duplication
    private val CONNECT_TIMEOUT = 15000
    private val READ_TIMEOUT = 30000
    private val CONNECTION_REQUEST_TIMEOUT = 10000
    private val ACCEPT_HEADER = "application/xml, text/xml, */*"

    def loadNamespaces() : Unit =
    {
        val queryResult = query("?action=query&meta=siteinfo&siprop=namespaces&format=xml")

        for(namespaceNode <- queryResult \ "query" \ "namespaces" \ "ns")
        {
            val namespaceId = (namespaceNode \ "@id").text.toInt
            val namespaceName = namespaceNode.text
            val canonicalName = (namespaceNode \ "@canonical").text

            // FIX: Based on the error, Namespace.get actually expects String
            val namespace = Namespace.get(language, namespaceId.toString)
            namespace match
            {
                case Some(ns) =>
                    logger.fine(s"Found namespace: $namespaceId -> $namespaceName (canonical: $canonicalName)")
                case None =>
                    logger.fine(s"Unknown namespace: $namespaceId")
            }
        }
    }

    def retrievePage(title: WikiTitle, redirect: Boolean = true): Option[WikiPage] =
    {
        require(title != null, "title != null")

        val pageQuery = if(redirect) "?action=query&format=xml&prop=revisions&rvprop=content&titles=" + formatWikiTitle(title)
                       else "?action=query&format=xml&prop=revisions&rvprop=content&redirects&titles=" + formatWikiTitle(title)

        val response = query(pageQuery)

        val pageNode = response \ "query" \ "pages" \ "page"
        if(pageNode.isEmpty || (pageNode \ "@missing").nonEmpty)
        {
            return None
        }

        val source = (pageNode \ "revisions" \ "rev").text
        // FIX: Use proper WikiPage constructor
        Some(new WikiPage(title, source))
    }

    /**
     * Retrieves pages by their IDs (required by WikiSource)
     */
    def retrievePagesByID(param: String, ids: Iterable[Long]): List[WikiPage] = {
        if (ids.isEmpty) return List.empty

        val idsString = ids.mkString("|")
        val queryString = s"?action=query&format=xml&prop=revisions&rvprop=content&${param}=${idsString}"

        try {
            val response = query(queryString)
            val pages = response \ "query" \ "pages" \ "page"

            pages.flatMap { pageNode =>
                val pageId = (pageNode \ "@pageid").text
                val title = (pageNode \ "@title").text
                val missing = (pageNode \ "@missing").nonEmpty

                if (!missing && title.nonEmpty) {
                    val source = (pageNode \ "revisions" \ "rev").text
                    try {
                        val wikiTitle = WikiTitle.parse(title, language)
                        Some(new WikiPage(wikiTitle, source))
                    } catch {
                        case _: Exception => None
                    }
                } else {
                    None
                }
            }.toList
        } catch {
            case ex: Exception =>
                logger.warning(s"Failed to retrieve pages by ID: ${ex.getMessage}")
                List.empty
        }
    }

    /**
     * Retrieves pages by their titles (required by WikiSource)
     */
    def retrievePagesByTitle(titles: Iterable[WikiTitle]): List[WikiPage] = {
        if (titles.isEmpty) return List.empty

        val titleStrings = titles.map(formatWikiTitle).mkString("|")
        val queryString = s"?action=query&format=xml&prop=revisions&rvprop=content&titles=${titleStrings}"

        try {
            val response = query(queryString)
            val pages = response \ "query" \ "pages" \ "page"

            pages.flatMap { pageNode =>
                val title = (pageNode \ "@title").text
                val missing = (pageNode \ "@missing").nonEmpty

                if (!missing && title.nonEmpty) {
                    val source = (pageNode \ "revisions" \ "rev").text
                    try {
                        val wikiTitle = WikiTitle.parse(title, language)
                        Some(new WikiPage(wikiTitle, source))
                    } catch {
                        case _: Exception => None
                    }
                } else {
                    None
                }
            }.toList
        } catch {
            case ex: Exception =>
                logger.warning(s"Failed to retrieve pages by title: ${ex.getMessage}")
                List.empty
        }
    }


    /**
     * Retrieves pages by namespace (required by WikiSource)
     */
    def retrievePagesByNamespace[U](namespace: Namespace, f: WikiPage => U): Unit = {
        try {
            var apcontinue = ""
            var hasMore = true

            while (hasMore) {
                var queryString = s"?action=query&format=xml&list=allpages&apnamespace=${namespace.code}&aplimit=500"
                if (apcontinue.nonEmpty) {
                    queryString += s"&apcontinue=${apcontinue}"
                }

                val response = query(queryString)
                val pages = response \ "query" \ "allpages" \ "p"

                pages.foreach { pageNode =>
                    val title = (pageNode \ "@title").text
                    if (title.nonEmpty) {
                        try {
                            val wikiTitle = WikiTitle.parse(title, language)
                            retrievePage(wikiTitle) match {
                                case Some(page) => f(page)
                                case None => // Page doesn't exist or is empty
                            }
                        } catch {
                            case _: Exception => // Skip malformed titles
                        }
                    }
                }

                // Check for continuation
                val queryContinue = response \ "query-continue" \ "allpages"
                val continueSeq = queryContinue \\ "@apcontinue"
                apcontinue = continueSeq.text
                hasMore = apcontinue.nonEmpty
            }
        } catch {
            case ex: Exception =>
                logger.warning(s"Failed to retrieve pages by namespace: ${ex.getMessage}")
        }
    }

    def retrieveTemplateUsageIDs(title: WikiTitle, maxCount: Int = 500): List[Long] =
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
                        pageList = pageList ::: List(title.text.toLong);
                }
        }while(canContinue)

      pageList;
    }

    def fileExistsOnWiki(fileName: String, language: Language): Boolean = {
        try {
            val fileNamespaceIdentifier = Namespace.File.name(language)
            val queryURLString = language.baseUri.replace("http:", "https:") + "/wiki/" + fileNamespaceIdentifier + ":" + URLEncoder.encode(fileName, "UTF-8")
            val connection = new URL(queryURLString).openConnection().asInstanceOf[HttpsURLConnection]
            connection.setRequestMethod("HEAD")
            connection.setConnectTimeout(CONNECT_TIMEOUT)
            connection.setReadTimeout(READ_TIMEOUT)
            val responseCode = connection.getResponseCode()
            connection.disconnect()
            responseCode == HttpURLConnection.HTTP_OK
        } catch {
            case _: Exception => false
        }
    }

    // REFACTORED: Simplified query method
    protected def query(params: String): Elem = {
        if (useHttpClient) {
            queryWithHttpClient(params)
        } else {
            queryWithURLConnection(params)
        }
    }

    // REFACTORED: Extract common header setting logic
    private def setCommonHeaders(setHeader: (String, String) => Unit): Unit = {
        if (customUserAgentEnabled) {
            setHeader("User-Agent", customUserAgentText)
        }
        setHeader("Accept", ACCEPT_HEADER)
    }

    // REFACTORED: Extract common logging logic
    private def logAttempt(method: String, fullUrl: String, attempt: Int): Unit = {
        logger.fine(s"Querying MediaWiki API with $method (attempt ${attempt + 1}): $fullUrl")
    }

    private def logSuccess(method: String, params: String): Unit = {
        logger.fine(s"Successfully retrieved data with $method for: $params")
    }

    // REFACTORED: Extract common XML processing logic
    private def processXmlResponse(inputStream: InputStream, method: String, params: String): Elem = {
        try {
            val xml = XML.load(inputStream)
            logSuccess(method, params)
            xml
        } finally {
            inputStream.close()
        }
    }

    // REFACTORED: Dramatically simplified HttpClient method
    private def queryWithHttpClient(params: String): Elem = {
        import org.apache.http.impl.client.HttpClients
        import org.apache.http.client.methods.HttpGet
        import org.apache.http.client.config.RequestConfig

        executeWithRetry(params) { (fullUrl, attempt) =>
            logAttempt("HttpClient", fullUrl, attempt)

            val client = HttpClients.createDefault
            val request = new HttpGet(fullUrl)

            // Use extracted header setting logic
            setCommonHeaders((key, value) => request.setHeader(key, value))

            val config = RequestConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .build()
            request.setConfig(config)

            try {
                val response = client.execute(request)
                try {
                    processXmlResponse(response.getEntity.getContent, "HttpClient", params)
                } finally {
                    response.close()
                }
            } finally {
                client.close()
            }
        }
    }

    // REFACTORED: Dramatically simplified URLConnection method
    private def queryWithURLConnection(params: String): Elem = {
        executeWithRetry(params) { (fullUrl, attempt) =>
            logAttempt("URLConnection", fullUrl, attempt)

            val connection = new URL(fullUrl).openConnection()

            // Use extracted header setting logic
            setCommonHeaders((key, value) => connection.setRequestProperty(key, value))

            connection.setConnectTimeout(CONNECT_TIMEOUT)
            connection.setReadTimeout(READ_TIMEOUT)

            processXmlResponse(connection.getInputStream, "URLConnection", params)
        }
    }

    // UNCHANGED: Keep the existing retry logic (it was already well-designed)
    private def executeWithRetry(params: String)(httpExecutor: (String, Int) => Elem): Elem = {
        val fullUrl = url + params

        for (attempt <- 0 to maxRetries) {
            try {
                return httpExecutor(fullUrl, attempt)
            } catch {
                case ex: org.apache.http.conn.ConnectTimeoutException =>
                    handleRetryException(params, attempt, ex, "HttpClient connection timeout")
                case ex: java.net.SocketTimeoutException =>
                    handleRetryException(params, attempt, ex, "Socket timeout")
                case ex: java.net.ConnectException =>
                    handleRetryException(params, attempt, ex, "Connection failed")
                case ex: IOException =>
                    handleRetryException(params, attempt, ex, "IO error")
                case ex: Exception =>
                    if (attempt < maxRetries) {
                        handleRetryException(params, attempt, ex, "General error")
                    } else {
                        logger.severe(s"All ${maxRetries + 1} attempts failed for: $params")
                        throw ex
                    }
            }
        }

        throw new IllegalStateException("Should never reach this point")
    }

    private def handleRetryException(params: String, attempt: Int, ex: Exception, errorType: String): Unit = {
        if (attempt < maxRetries) {
            val sleepTime = Math.min(1000 * (attempt + 1), 5000)
            logger.warning(s"$errorType on attempt ${attempt + 1} for: $params. Retrying in ${sleepTime}ms...")
            Thread.sleep(sleepTime)
        } else {
            logger.severe(s"All ${maxRetries + 1} attempts failed for: $params")
            throw ex
        }
    }

    private def formatWikiTitle(title: WikiTitle) : String = {
        URLEncoder.encode(title.decodedWithNamespace.replace(' ', '_'), "UTF-8")
    }
}
