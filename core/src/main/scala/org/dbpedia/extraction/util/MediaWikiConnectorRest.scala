package org.dbpedia.extraction.util

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.config.Config.{MediaWikiConnection}
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.slf4j.LoggerFactory

import java.io.{InputStream, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.time.temporal.ChronoUnit
import scala.collection.JavaConverters._
import java.util.zip._
import java.util
import javax.xml.ws.WebServiceException
import scala.io.Source
import scala.util.{Failure, Success, Try}
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
class MediaWikiConnectorRest(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) extends MediaWikiConnectorAbstract(connectionConfig, xmlPath ) {
  //protected val apiUrl : String = connectionConfig.apiUrl
  protected val apiAccept: String = connectionConfig.accept
  protected val apiCharset: String = connectionConfig.charset
  protected val apiProfile: String = connectionConfig.profile
  protected val userAgent: String = connectionConfig.useragent
  private val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean
  private val availableProcessors = osBean.getAvailableProcessors

  /**
   * Retrieves a Wikipedia page.
   *
   * @param pageTitle The encoded title of the page
   * @return The page as an Option
   */
  override def retrievePage(pageTitle: WikiTitle, apiParameterString: String, isRetry: Boolean = false): Option[String] = {
    val retryFactor = if (isRetry) 2 else 1


    //val apiUrl: URL = new URL(connectionConfig.apiUrl.replace("{{LANG}}",pageTitle.language.wikiCode))
    // The encoded title may contain some URI-escaped characters (e.g. "5%25-Klausel"),
    // so we can't use URLEncoder.encode(). But "&" is not escaped, so we do this here.
    // TODO: test this in detail!!! there may be other characters that need to be escaped.
    // TODO central string management
    var titleParam = pageTitle.encodedWithNamespace
    this.CHARACTERS_TO_ESCAPE foreach {
      case (search, replacement) => titleParam = titleParam.replace(search, replacement);
    }
    //replaces {{lang}} with the language
    var url: String = connectionConfig.apiUrl.replace("{{LANG}}", pageTitle.language.wikiCode)
    val apiUrl: URL = new URL(url.concat(titleParam))


    var parameters = "redirect=true"

    // Fill parameters
    //var parameters = "uselang=" + pageTitle.language.wikiCode
    println(s"mediawikiurl: $apiUrl?$parameters")


    for (counter <- 1 to maxRetries) {
      try {
        val conn = apiUrl.openConnection
        conn.setDoOutput(true) // POST REQUEST to verify

        val start = java.time.LocalTime.now()

        conn.setConnectTimeout(retryFactor * connectMs)
        conn.setReadTimeout(retryFactor * readMs)
        conn.setRequestProperty("accept", apiAccept)
        conn.setRequestProperty("charset", apiCharset)
        conn.setRequestProperty("profile", apiProfile)
        conn.setRequestProperty("Accept-Language", pageTitle.language.wikiCode)
        conn.setRequestProperty("User-Agent", userAgent)

        val inputStream = conn.getInputStream

        val end = java.time.LocalTime.now()
        conn match {
          case connection: HttpURLConnection => {
            log.debug("Request type: " + connection.getRequestMethod + "; URL: " + connection.getURL +
              "; Parameters: " + parameters + "; HTTP code: " + connection.getHeaderField(null) +
              "; Request time: " + start + "; Response time: " + end + "; Time needed: " +
              start.until(end, ChronoUnit.MILLIS))
          }
          case _ =>
        }
        // Read answer
        return readInAbstract(inputStream) match {
          case Success(str) => Option(str)
          case Failure(e) => throw e
        }
      }
      catch {
        case ex: Exception =>
          // The web server may still be trying to render the page. If we send new requests
          // at once, there will be more and more tasks running in the web server and the
          // system eventually becomes overloaded. So we wait a moment. The higher the load,
          // the longer we wait.

          val zw = ex.getMessage
          var loadFactor = Double.NaN
          var sleepMs = sleepFactorMs

          // if the load average is not available, a negative value is returned
          val load = osBean.getSystemLoadAverage
          if (load >= 0) {
            loadFactor = load / availableProcessors
            sleepMs = (loadFactor * sleepFactorMs).toInt
          }
          ex match {
            case e: java.net.SocketTimeoutException =>
              if (counter < maxRetries)
                Thread.sleep(sleepMs)
              else
                throw new Exception("Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries. Giving up. Load factor: " + loadFactor, e)
            case _ => throw ex
          }
      }
    }
    throw new Exception("Could not retrieve abstract after " + maxRetries + " tries for page: " + pageTitle.encoded)
  }


  /**
   * Get the parsed and cleaned abstract text from the MediaWiki instance input stream.
   * It returns
   * <api> <query> <pages> <page> <extract> ABSTRACT_TEXT <extract> <page> <pages> <query> <api>
   * ///  <api> <parse> <text> ABSTRACT_TEXT </text> </parse> </api>
   */
  override def readInAbstract(inputStream: InputStream): Try[String] = {
    // for XML format
    var htmlAnswer = Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("")
    //var text = XML.loadString(xmlAnswer).asInstanceOf[NodeSeq]

    //test for errors
    val pattern = "(<error[^>]+info=\")([^\\\"]+)".r
    if (htmlAnswer.contains("error code=")) {
      return Failure(new WebServiceException(pattern.findFirstMatchIn(htmlAnswer) match {
        case Some(m) => m.group(2)
        case None => "An unknown exception occurred while retrieving the source XML from the mediawiki API."
      }))
    }


    decodeHtml(htmlAnswer.trim)
  }
}