package org.dbpedia.extraction.util

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.dbpedia.extraction.config.Config.MediaWikiConnection
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.util.text.html.{HtmlCoder, XmlCodes}
import org.slf4j.LoggerFactory

import java.io.{InputStream, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.time.temporal.ChronoUnit
import java.util
import java.util.zip._
import javax.xml.ws.WebServiceException
import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
class MediaWikiConnector4(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) {

  protected val log = LoggerFactory.getLogger(classOf[MediaWikiConnector4])
  protected val log2 = LoggerFactory.getLogger("apiCalls")
  //protected def apiUrl: URL = new URL(connectionConfig.apiUrl)
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
  def retrievePage(pageTitle : WikiTitle, apiParameterString: String, isRetry: Boolean = false) : Option[String] =
  {
    val retryFactor = if(isRetry) 2 else 1
    var waiting_time = sleepFactorMs
    //replaces {{lang}} with the language
    val apiUrl: URL = new URL(connectionConfig.apiUrl.replace("{{LANG}}",pageTitle.language.wikiCode))

    // The encoded title may contain some URI-escaped characters (e.g. "5%25-Klausel"),
    // so we can't use URLEncoder.encode(). But "&" is not escaped, so we do this here.
    // TODO: test this in detail!!! there may be other characters that need to be escaped.
    // TODO central string management
    var titleParam = pageTitle.encodedWithNamespace
    MediaWikiConnector2.CHARACTERS_TO_ESCAPE foreach {
      case (search, replacement) =>  titleParam = titleParam.replace(search, replacement);
    }


    // Fill parameters
    var parameters = "uselang=" + pageTitle.language.wikiCode

    parameters += (pageTitle.id match{
      case Some(id) if apiParameterString.contains("%d") =>
        apiParameterString.replace("&page=%s", "").format(id)
      case _ => apiParameterString.replaceAll("&pageid=[^&]+", "").format(titleParam)
    })
    //parameters += apiParameterString.replaceAll("&pageid=[^&]+", "").format(titleParam)
    //parameters += "&maxlag=1"
    println(s"mediawikiurl: $apiUrl?$parameters")

    for(counter <- 1 to maxRetries)
    {

        val conn = apiUrl.openConnection
        val start = java.time.LocalTime.now()
        conn.setDoOutput(true)
        conn.setConnectTimeout(retryFactor * connectMs)
        conn.setReadTimeout(retryFactor * readMs)
        //conn.setRequestProperty("User-Agent","(https://dbpedia.org/; dbpedia@infai.org) DIEF")
        //conn.setRequestProperty("Accept-Encoding","gzip")

        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(parameters)
        writer.flush()
        writer.close()

        var mapper = new ObjectMapper()
        mapper.registerModule(DefaultScalaModule)
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)

        var answer_header = conn.getHeaderFields();
        var answer_clean = answer_header.asScala.filterKeys(_ != null);

        answer_clean += ("parameters" -> util.Arrays.asList(parameters))
        answer_clean += ("url" -> util.Arrays.asList(apiUrl.toString))
        answer_clean += ("titleParam" -> util.Arrays.asList(titleParam.toString))
        //answer_clean += ("status" -> conn.getHeaderField(null).toString)
        var jsonString = mapper.writeValueAsString(answer_clean);
        log2.info(jsonString)

        if(conn.getHeaderField(null) == "HTTP/1.1 200 OK" ){
          val inputStream = conn.getInputStream

          //val gz = new GZIPInputStream(inputStream)
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

        }else{
          if (answer_clean.contains("retry-after")) {
            waiting_time = Integer.parseInt(answer_clean("retry-after").get(0)) * 1000
          }

          var sleepMs = waiting_time

          println("Sleeping time >>>>>>>>>>>" + sleepMs)
          if (counter < maxRetries)
            Thread.sleep(sleepMs)
          else
            throw new Exception("Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries.")

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

  object MediaWikiConnector2 {
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
