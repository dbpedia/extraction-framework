package org.dbpedia.extraction.util

import org.dbpedia.extraction.config.Config.MediaWikiConnection
import org.dbpedia.extraction.wikiparser.WikiTitle
import java.io.InputStream
import java.net.{HttpURLConnection, URL}
import java.time.temporal.ChronoUnit
import scala.collection.JavaConverters._
import scala.math.pow
import javax.xml.ws.WebServiceException
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
 * The Mediawiki API connector
 * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
 * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
 */
class MediaWikiConnectorRest(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) extends MediaWikiConnectorAbstract(connectionConfig, xmlPath ) {

  protected val apiAccept: String = connectionConfig.accept
  protected val apiCharset: String = connectionConfig.charset
  protected val apiProfile: String = connectionConfig.profile
  protected val userAgent: String = connectionConfig.useragent

  /**
   * Retrieves a Wikipedia page.
   *
   * @param pageTitle The encoded title of the page
   * @return The page as an Option
   */
  override def retrievePage(pageTitle: WikiTitle, apiParameterString: String, isRetry: Boolean = false): Option[String] = {
    val retryFactor = if (isRetry) 2 else 1
    var SuccessParsing = false
    var parsedAnswer: Try[String] = null
    var waitingTime = sleepFactorMs


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
    val url: String = connectionConfig.apiUrl.replace("{{LANG}}", pageTitle.language.wikiCode)

    val parameters = "redirect=true"
    val apiUrl: URL = new URL(url.concat(titleParam).concat("?"+parameters))

    //println(s"mediawikiurl: $apiUrl")

    for (counter <- 1 to maxRetries) {

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
      val answerHeader = conn.getHeaderFields()
      val answerClean = answerHeader.asScala.filterKeys(_ != null)

      if(conn.getHeaderField(null).contains("HTTP/1.1 200 OK") ){
        val end = java.time.LocalTime.now()
        conn match {
          case connection: HttpURLConnection =>
            log.debug("Request type: " + connection.getRequestMethod + "; URL: " + connection.getURL +
              "; Parameters: " + parameters + "; HTTP code: " + connection.getHeaderField(null) +
              "; Request time: " + start + "; Response time: " + end + "; Time needed: " +
              start.until(end, ChronoUnit.MILLIS))
          case _ =>
        }
        // Read answer
        parsedAnswer = readInAbstract(inputStream)
        SuccessParsing = parsedAnswer match {
          case Success(str) => true
          case Failure(_) => false
        }
      }
      if(!SuccessParsing){
        var sleepMs = sleepFactorMs
        if (retryAfter && answerClean.contains("retry-after")) {
          //println("GIVEN RETRY-AFTER > "+ answer_clean("retry-after").get(0))
          waitingTime = Integer.parseInt(answerClean("retry-after").get(0)) * 1000

          // exponential backoff test
          sleepMs = pow(waitingTime, counter).toInt
          //println("WITH EXPONENTIAL BACK OFF" + counter)
          //println("Sleeping time double >>>>>>>>>>>" + pow(waiting_time, counter))
          //println("Sleeping time int >>>>>>>>>>>" + sleepMs)
        }
        if (counter < maxRetries)
          Thread.sleep(sleepMs)
        else
          throw new Exception("Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries.")
      } else {
        //println(s"mediawikiurl: $apiUrl?$parameters")
        return parsedAnswer match {
          case Success(str) => Option(str)
          case Failure(e) => throw e
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
    try{
      val htmlAnswer = Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("")
      //var text = XML.loadString(xmlAnswer).asInstanceOf[NodeSeq]

      //test for errors
      val pattern = "(<error[^>]+info=\")([^\\\"]+)".r
      if (htmlAnswer.contains("error code=")) {
        return Failure(new WebServiceException(pattern.findFirstMatchIn(htmlAnswer) match {
          case Some(m) => m.group(2)
          case None => "An unknown exception occurred while retrieving the source XML from the mediawiki API."
        }))
      }

      Success(htmlAnswer)
    }


  }
}