package org.dbpedia.extraction.util

import java.io.{InputStream, OutputStreamWriter}
import java.net.URL
import javax.xml.ws.WebServiceException

import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}

import scala.io.Source
import scala.util.{Failure, Success, Try}
import org.dbpedia.extraction.config.Config.MediaWikiConnection

/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
class MediaWikiConnector(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) {


  protected def apiUrl: URL = new URL(connectionConfig.apiUrl)
  //require(Try{apiUrl.openConnection().connect()} match {case Success(x)=> true case Failure(e) => false}, "can not connect to the apiUrl")

  protected val maxRetries: Int = connectionConfig.maxRetries
  require(maxRetries <= 10 && maxRetries > 0, "maxRetries has to be in the interval of [1,10]")

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

  /**
    * Retrieves a Wikipedia page.
    *
    * @param pageTitle The encoded title of the page
    * @return The page as an Option
    */
  def retrievePage(pageTitle : WikiTitle, apiParameterString: String) : Option[String] =
  {
    // The encoded title may contain some URI-escaped characters (e.g. "5%25-Klausel"),
    // so we can't use URLEncoder.encode(). But "&" is not escaped, so we do this here.
    // TODO: test this in detail!!! there may be other characters that need to be escaped.
    var titleParam = pageTitle.encodedWithNamespace
    MediaWikiConnector.CHARACTERS_TO_ESCAPE foreach {
      case (search, replacement) =>  titleParam = titleParam.replace(search, replacement);
    }

    // Fill parameters
    val parameters = "uselang=" + pageTitle.language.wikiCode + (pageTitle.id match{
      case Some(id) if apiParameterString.contains("%d") =>
        apiParameterString.replace("&page=%s", "").format(id)
      case _ => apiParameterString.replaceAll("&pageid=[^&]+", "").format(titleParam)
    })

    for(counter <- 1 to maxRetries)
    {
      try
      {
        val conn = apiUrl.openConnection
        conn.setDoOutput(true)
        conn.setConnectTimeout(connectMs)
        conn.setReadTimeout(readMs)

        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(parameters)
        writer.flush()
        writer.close()

        // Read answer
        return readInAbstract(conn.getInputStream) match{
          case Success(str) => Option(str)
          case Failure(e) => throw e
        }
      }
      catch
      {
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
            case e : java.net.SocketTimeoutException =>
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
  private def readInAbstract(inputStream : InputStream) : Try[String] =
  {
    // for XML format
    var xmlAnswer = Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("")
    //var text = XML.loadString(xmlAnswer).asInstanceOf[NodeSeq]

    //test for errors
    val pattern = "(<error[^>]+info=\")([^\\\"]+)".r
    if(xmlAnswer.contains("error code=")) {
      return Failure(new WebServiceException(pattern.findFirstMatchIn(xmlAnswer) match {
        case Some(m) => m.group(2)
        case None => "An unknown exception occurred while retrieving the source XML from the mediawiki API."
      }))
    }

    //get rid of surrounding tags
    xmlAnswer = xmlAnswer.replaceFirst("<\\?xml[^>]*>", "")
    for(child <- xmlPath){
      if(xmlAnswer.contains("<" + child) && xmlAnswer.contains("</" + child)) {
        xmlAnswer = xmlAnswer.replaceFirst("<" + child + "[^>]*>", "")
        xmlAnswer = xmlAnswer.substring(0, xmlAnswer.lastIndexOf("</" + child + ">"))
      }
      else
        return Failure(new WebServiceException("The response from the mediawiki API does not contain the expected XML path: " + xmlPath))
    }

    decodeHtml(xmlAnswer.trim)
  }

  object MediaWikiConnector {
    /**
      * List of all characters which are reserved in a query component according to RFC 2396
      * with their escape sequences as determined by the JavaScript function encodeURIComponent.
      */
    val CHARACTERS_TO_ESCAPE = List(
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
  }
}
