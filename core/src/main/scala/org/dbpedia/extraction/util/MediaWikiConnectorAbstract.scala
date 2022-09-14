package org.dbpedia.extraction.util

import org.dbpedia.extraction.config.Config.MediaWikiConnection
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.slf4j.LoggerFactory

import java.io.{InputStream, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.time.temporal.ChronoUnit
import scala.util.{Failure, Success, Try}

/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
abstract class MediaWikiConnectorAbstract(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) {

  protected val log = LoggerFactory.getLogger(classOf[MediaWikiConnectorAbstract])
  //protected def apiUrl: URL = new URL(connectionConfig.apiUrl)
  //require(Try{apiUrl.openConnection().connect()} match {case Success(x)=> true case Failure(e) => false}, "can not connect to the apiUrl")

  protected val maxRetries: Int = connectionConfig.maxRetries
  require(maxRetries <= 10 && maxRetries > 0, "maxRetries has to be in the interval of [1,10]")

  protected val retryAfter: Boolean = connectionConfig.retryafter
  /** timeout for connection to web server, milliseconds */
  protected val connectMs: Int = connectionConfig.connectMs
  require(connectMs > 200, "connectMs shall be more than 200 ms!")

  /** timeout for result from web server, milliseconds */
  protected val readMs: Int = connectionConfig.readMs
  require(readMs > 1000, "readMs shall be more than 1000 ms!")

  /** sleep between retries, milliseconds, multiplied by CPU load */
  protected val sleepFactorMs: Int = connectionConfig.sleepFactor
  require(sleepFactorMs > 200, "sleepFactorMs shall be more than 200 ms!")

  //protected val xmlPath = connectionConfig.abstractTags.split(",").map(_.trim)

  private val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean
  private val availableProcessors = osBean.getAvailableProcessors

  protected val CHARACTERS_TO_ESCAPE = List(
    (";", "%3B"),
    ("/", "%2F"),
    ("?", "%3F"),
    (":", "%3A"),
    ("@", "%40"),
    ("&", "%26"),
    ("=", "%3D"),
    ("+", "%2B"),
    (",", "%2C"),
    ("$", "%24")
  )
  /**
    * Retrieves a Wikipedia page.
    *
    * @param pageTitle The encoded title of the page
    * @return The page as an Option
    */
  def retrievePage(pageTitle : WikiTitle, apiParameterString: String, isRetry: Boolean = false) : Option[String]

  def decodeHtml(text: String): Try[String] = {
    val coder = new HtmlCoder(XmlCodes.NONE)
    Try(coder.code(text))
  }

  /**
    * Get the parsed and cleaned abstract text from the MediaWiki instance input stream.
    * It returns
    * <api> <query> <pages> <page> <extract> ABSTRACT_TEXT <extract> <page> <pages> <query> <api>
    *  ///  <api> <parse> <text> ABSTRACT_TEXT </text> </parse> </api>
    */
  protected def readInAbstract(inputStream : InputStream) : Try[String]

}
