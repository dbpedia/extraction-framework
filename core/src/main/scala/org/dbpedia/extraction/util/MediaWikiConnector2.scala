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
import java.util.zip._
import java.util
import scala.math.pow
import javax.xml.ws.WebServiceException
import scala.io.Source
import scala.util.{Failure, Success, Try}
import org.slf4j.LoggerFactory
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature

import java.util
import scala.collection.JavaConverters._
/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
class MediaWikiConnector2(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) {

  protected val log = LoggerFactory.getLogger(classOf[MediaWikiConnector2])
  // UNCOMMENT FOR LOG
  //  protected val log2 = LoggerFactory.getLogger("apiCalls")

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


  protected val userAgent: String = connectionConfig.useragent
  require(userAgent != "" , "userAgent must be declared !")


  protected val gzipCall: Boolean = connectionConfig.gzip
  //require(gzipCall != "", "gzipCall must be declared !")


  protected val retryAfter: Boolean = connectionConfig.retryafter
  //require(gzipCall != "", "gzipCall must be declared !")


  protected val maxLag: Int = connectionConfig.maxlag
  //require(maxLag != "", "maxLag must be declared !")

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
    var Success_parsing = false
    var currentMaxLag= maxLag
    var gzipok=true
    var parsed_answer: Try[String] = null
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



    for(counter <- 1 to maxRetries)
    {
        // Fill parameters
        var parameters = "uselang=" + pageTitle.language.wikiCode

        parameters += (pageTitle.id match {
          case Some(id) if apiParameterString.contains("%d") =>
            apiParameterString.replace("&page=%s", "").format(id)
          case _ => apiParameterString.replaceAll("&pageid=[^&]+", "").format(titleParam)
        })
        //parameters += apiParameterString.replaceAll("&pageid=[^&]+", "").format(titleParam)

        parameters += "&maxlag=" + currentMaxLag


        val conn = apiUrl.openConnection
        val start = java.time.LocalTime.now()
        conn.setDoOutput(true)
        conn.setConnectTimeout(retryFactor * connectMs)
        conn.setReadTimeout(retryFactor * readMs)
        conn.setRequestProperty("User-Agent",userAgent)
        if ( gzipCall ) conn.setRequestProperty("Accept-Encoding","gzip")

        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(parameters)
        writer.flush()
        writer.close()
        var answer_header = conn.getHeaderFields();
        var answer_clean = answer_header.asScala.filterKeys(_ != null);

       // UNCOMMENT FOR LOG
       /* var mapper = new ObjectMapper()
        mapper.registerModule(DefaultScalaModule)
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)


        answer_clean += ("parameters" -> util.Arrays.asList(parameters) )
        answer_clean += ("url" -> util.Arrays.asList(apiUrl.toString) )
        answer_clean += ("titleParam" -> util.Arrays.asList(titleParam.toString) )
        answer_clean += ("status" ->  util.Arrays.asList(conn.getHeaderField(null)))

        var jsonString = mapper.writeValueAsString(answer_clean);

        log2.info(jsonString)*/
      if(conn.getHeaderField(null).contains("HTTP/1.1 200 OK") ){
        var inputStream = conn.getInputStream
        // IF GZIP
        if ( gzipCall ){
          try {
            inputStream = new GZIPInputStream(inputStream)
          }catch {
            case x:ZipException =>{
              gzipok = false
            }
          }
        }
        val end = java.time.LocalTime.now()
        conn match {
          case connection: HttpURLConnection => {
            log.debug("Request type: "+ connection.getRequestMethod + "; URL: " + connection.getURL +
              "; Parameters: " + parameters +"; HTTP code: "+ connection.getHeaderField(null) +
              "; Request time: "+start+"; Response time: " + end + "; Time needed: " +
              start.until(end, ChronoUnit.MILLIS))
          }
          case _ =>
        }
        // Read answer
        parsed_answer = readInAbstract(inputStream)
        Success_parsing = parsed_answer match {
          case Success(str) => true
          case Failure(e) => false
        }


      }
      if(!Success_parsing || !gzipok){

        println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXFAIL")
        println(s"mediawikiurl: $apiUrl?$parameters")
        var sleepMs = sleepFactorMs
        if (retryAfter && answer_clean.contains("retry-after") ){
            println("GIVEN RETRY-AFTER > "+ answer_clean("retry-after").get(0))
            waiting_time = Integer.parseInt(answer_clean("retry-after").get(0)) * 1000

            // exponential backoff test
            sleepMs = pow(waiting_time, counter).toInt
            println("WITH EXPONENTIAL BACK OFF" + counter)
            println("Sleeping time double >>>>>>>>>>>" + pow(waiting_time, counter))
            println("Sleeping time int >>>>>>>>>>>" + sleepMs)

        }
        if( currentMaxLag <15 ){
          currentMaxLag = currentMaxLag + 1
          println("> INCREASE MAX LAG : " + currentMaxLag)
        }
        if (counter < maxRetries)
          Thread.sleep(sleepMs)
        else
          throw new Exception("Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries.")
      }else{

        println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXOK")

        println(s"mediawikiurl: $apiUrl?$parameters")
        return parsed_answer match {
          case Success(str) => Option(str)
          case Failure(e) => throw e
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
