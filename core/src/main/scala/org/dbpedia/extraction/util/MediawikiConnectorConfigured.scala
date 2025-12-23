package org.dbpedia.extraction.util

import org.dbpedia.extraction.config.Config.MediaWikiConnection
import org.dbpedia.extraction.wikiparser.WikiTitle

import java.io.{InputStream, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import java.time.temporal.ChronoUnit
import java.util.zip._
import scala.math.pow
import javax.xml.ws.WebServiceException
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
/**
  * The Mediawiki API connector
  * @param connectionConfig - Collection of parameters necessary for API requests (see Config.scala)
  * @param xmlPath - An array of XML tag names leading from the root (usually 'api') to the intended content of the response XML (depending on the request query used)
  */
class MediawikiConnectorConfigured(connectionConfig: MediaWikiConnection, xmlPath: Seq[String]) extends MediaWikiConnectorAbstract(connectionConfig, xmlPath ){

  protected val userAgent: String = connectionConfig.useragent
  require(userAgent != "" , "userAgent must be declared !")
  protected val gzipCall: Boolean = connectionConfig.gzip
  protected val maxLag: Int = connectionConfig.maxlag
  private val osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean
  private val availableProcessors = osBean.getAvailableProcessors

  /**
    * Retrieves a Wikipedia page.
    *
    * @param pageTitle The encoded title of the page
    * @return The page as an Option
    */
  override def retrievePage(pageTitle : WikiTitle, apiParameterString: String, isRetry: Boolean = false) : Option[String] =
  {
    val retryFactor = if(isRetry) 2 else 1
    var waitingTime = sleepFactorMs
    var SuccessParsing = false
    var currentMaxLag= maxLag
    var gzipok = true
    var parsedAnswer: Try[String] = null
    //replaces {{lang}} with the language
    val apiUrl: URL = new URL(connectionConfig.apiUrl.replace("{{LANG}}",pageTitle.language.wikiCode))

    // The encoded title may contain some URI-escaped characters (e.g. "5%25-Klausel"),
    // so we can't use URLEncoder.encode(). But "&" is not escaped, so we do this here.
    // TODO: test this in detail!!! there may be other characters that need to be escaped.
    // TODO central string management
    var titleParam = pageTitle.encodedWithNamespace
    this.CHARACTERS_TO_ESCAPE foreach {
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
        // NEED TO BE ABLE TO MANAGE <redirects /> parsing
        //parameters += "&redirects=1"

        val conn = apiUrl.openConnection
        val start = java.time.LocalTime.now()
        conn.setDoOutput(true)
        conn.setConnectTimeout(retryFactor * connectMs)
        conn.setReadTimeout(retryFactor * readMs)
        conn.setRequestProperty("User-Agent",userAgent)
        if (gzipCall) conn.setRequestProperty("Accept-Encoding","gzip")

        //println(s"mediawikiurl: $apiUrl?$parameters")
        val writer = new OutputStreamWriter(conn.getOutputStream)
        writer.write(parameters)
        writer.flush()
        writer.close()
        val answerHeader = conn.getHeaderFields();
        val answerClean = answerHeader.asScala.filterKeys(_ != null);

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
          } catch {
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
        parsedAnswer = readInAbstract(inputStream)
        SuccessParsing = parsedAnswer match {
          case Success(str) => true
          case Failure(e) => false
        }
      }
      if(!SuccessParsing) {
        //println("ERROR DURING PARSING" )
        var sleepMs = sleepFactorMs
        if (retryAfter && answerClean.contains("retry-after")) {
          //println("GIVEN RETRY-AFTER > "+ answer_clean("retry-after").get(0))
          waitingTime = Integer.parseInt(answerClean("retry-after").get(0)) * 1000

          // exponential backoff test
          sleepMs = pow(waitingTime, counter).toInt
          //println("WITH EXPONENTIAL BACK OFF" + counter)
          //println("Sleeping time double >>>>>>>>>>>" + pow(waiting_time, counter))
          //println("Sleeping time int >>>>>>>>>>>" + sleepMs)
          if (currentMaxLag < 15) {
            // INCREMENT MaxLag
            currentMaxLag = currentMaxLag + 1
            //println("> INCREASE MAX LAG : " + currentMaxLag)
          }
          if (counter < maxRetries)
            Thread.sleep(sleepMs)
          else
            throw new Exception("Timeout error retrieving abstract of " + pageTitle + " in " + counter + " tries.")
        }
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
    *  ///  <api> <parse> <text> ABSTRACT_TEXT </text> </parse> </api>
    */
  override def readInAbstract(inputStream : InputStream) : Try[String] =
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


    // REDIRECT CASE
    // Implemented but useful?
    //xmlAnswer = xmlAnswer.replaceAll("<redirects />", "")
    /*if (xmlAnswer.contains("<redirects>" ) && xmlAnswer.contains("</redirects>")) {
      val indexBegin = xmlAnswer.indexOf("<redirects>")
      val indexEnd = xmlAnswer.indexOf("</redirects>", indexBegin + "<redirects>".length())
      xmlAnswer=xmlAnswer.substring(0, indexBegin)+xmlAnswer.substring(indexEnd + 1, xmlAnswer.length())
    }*/

    //get rid of surrounding tags
    // I limited the regex and I added here a second replace because some pages like the following returned malformed triples :
    //<http://dbpedia.org/resource/123Movies> <http://dbpedia.org/ontology/abstract> "xml version=\"1.0\"?>123Movies, GoMovies, GoStream, MeMovies or 123movieshub was a network of file streaming websites operating from Vietnam which allowed users to watch films for free. It was called the world's \"most popular illegal site\" by the Motion Picture Association of America (MPAA) in March 2018, before being shut down a few weeks later on foot of a criminal investigation by the Vietnamese authorities. As of July 2022, the network is still active via clone sites."@en

    xmlAnswer = xmlAnswer.replaceFirst("""<\?xml version=\"\d.\d\"\?>""", "").replaceFirst("""xml version=\"\d.\d\"\?>""","")

    for (child <- xmlPath) {
      if (xmlAnswer.contains("<" + child) && xmlAnswer.contains("</" + child)) {
        xmlAnswer = xmlAnswer.replaceFirst("<" + child + "[^>]*>", "")
        xmlAnswer = xmlAnswer.substring(0, xmlAnswer.lastIndexOf("</" + child + ">"))
      }
      else
        return Failure(new WebServiceException("The response from the mediawiki API does not contain the expected XML path: " + xmlPath))
    }

    decodeHtml(xmlAnswer.trim)
  }

}
