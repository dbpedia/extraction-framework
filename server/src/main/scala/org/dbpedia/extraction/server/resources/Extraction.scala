package org.dbpedia.extraction.server.resources

import java.net.{URI, URL}
import java.io.IOException

import org.dbpedia.extraction.destinations.formatters.{Formatter, RDFJSONFormatter, TerseFormatter}
import org.dbpedia.extraction.util.Language
import javax.ws.rs._
import javax.ws.rs.core.{Context, HttpHeaders, MediaType, Response}
import java.util.logging.{Level, Logger}

import scala.xml.Elem
import scala.io.{Codec, Source}
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.destinations.{DeduplicatingDestination, Destination, WriterDestination}
import org.dbpedia.extraction.server.resources.Extraction.getClass
import org.dbpedia.extraction.sources.{Source, WikiSource, XMLSource}
import stylesheets.TriX
import org.dbpedia.extraction.transform.Quad

import java.io.StringWriter
import scala.collection.mutable.ArrayBuffer

object Extraction
{
    private val logger = Logger.getLogger(getClass.getName)
}

/**
 * TODO: merge Extraction.scala and Mappings.scala
 */
@Path("/extraction/{lang}/")
class Extraction(@PathParam("lang") langCode : String)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

  private def getTitle: String = {
    // Get default page title from server config - no fallback, let it fail if config is broken
    Server.config.getDefaultPageTitle(langCode)
  }

  private val logger = Logger.getLogger(getClass.getName)

    @GET
    @Produces(Array("application/xhtml+xml"))
  def get = {
    try {
val extractors = Server.getInstance().getAvailableExtractorNames(language)
       <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
         {ServerHeader.getHeader("Extractor a page")}
         <body>
           <div class="row">
             <div class="col-md-3 col-md-offset-5">
              <h2>Extract a page</h2>
               <form action="extract" method="get">
                Page title<br/>
                <input type="text" name="title" value={ getTitle }/><br/>
                Revision ID (optional, overrides title)<br/>
                <input type="text" name="revid"/><br/>
                XML URL (optional, fetch XML from URL instead of title)<br/>
                <input type="text" name="xmlUrl" placeholder="https://example.com/wiki/Special:Export/PageTitle"/><br/>
                Output format<br/>
                <select name="format">
                  <option value="trix">Trix</option>
                  <option value="turtle-triples">Turtle-Triples</option>
                  <option value="turtle-quads">Turtle-Quads</option>
                  <option value="n-triples">N-Triples</option>
                  <option value="n-quads">N-Quads</option>
                  <option value="rdf-json">RDF/JSON</option>
                </select><br/>
              Select Extractors:<br/>
              <label for="mappings">Mappings Only <input type="checkbox" name="extractors" value="mappings" id="mappings"/></label><br/>
              <label for="custom">All Enabled Extractors <input type="checkbox" name="extractors" value="custom" id="custom"/></label><br/>
              {extractors.map(extractor =>
                <span>
                  <label for={extractor}>{extractor} <input type="checkbox" name="extractors" value={extractor} id={extractor}/></label><br/>
                </span>
              )}
              <input type="submit" value="Extract" />
            </form>
            </div>
           </div>
         </body>
       </html>
    } catch {
      case e: IllegalArgumentException =>
        // Language not enabled - return error page
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {ServerHeader.getHeader("Configuration Error")}<body>
          <div class="row">
            <div class="col-md-6 col-md-offset-3">
              <h2>Configuration Error</h2>
              <div class="alert alert-danger">
                <strong>Error:</strong> {e.getMessage}
              </div>
              <p>Please check the server configuration and try again.</p>
            </div>
          </div>
        </body>
        </html>
      case e: IllegalStateException =>
        // Language enabled but no extractors - return error page
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {ServerHeader.getHeader("Configuration Error")}<body>
          <div class="row">
            <div class="col-md-6 col-md-offset-3">
              <h2>Configuration Error</h2>
              <div class="alert alert-danger">
                <strong>Error:</strong> {e.getMessage}
              </div>
              <p>Please check the extractor configuration for this language.</p>
            </div>
          </div>
        </body>
        </html>
      case e: Exception =>
        // Unexpected error - return generic error page
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          {ServerHeader.getHeader("Server Error")}<body>
          <div class="row">
            <div class="col-md-6 col-md-offset-3">
              <h2>Server Error</h2>
              <div class="alert alert-danger">
                <strong>Error:</strong> Failed to load extraction interface: {e.getMessage}
              </div>
              <p>Please contact the administrator if this problem persists.</p>
            </div>
          </div>
        </body>
        </html>
    }
  }

  /**
   * Custom destination that collects quads in memory
   */
  private class QuadCollectorDestination extends Destination {
    val quads = new ArrayBuffer[Quad]()

    override def open(): Unit = {}

    override def write(graph: Traversable[Quad]): Unit = {
      quads ++= graph
    }

    override def close(): Unit = {}
  }

  /**
   * Helper method to create source from xmlUrl or fallback to WikiSource
   */
  private def createSource(xmlUrl: String, revid: Long, title: String): org.dbpedia.extraction.sources.Source = {
    if (xmlUrl != null && !xmlUrl.isEmpty) {
          // Fetch XML from custom URL with user agent
          import org.apache.http.impl.client.HttpClients
          import org.apache.http.client.methods.HttpGet
          val client = HttpClients.createDefault
          try {
            val request = new HttpGet(xmlUrl)
            // Apply user agent if enabled (same logic as WikiApi)
            val customUserAgentEnabled = try {
              System.getProperty("extract.wikiapi.customUserAgent.enabled", "false").toBoolean
            } catch {
              case ex: Exception => false
            }
            val customUserAgentText = try {
              System.getProperty("extract.wikiapi.customUserAgent.text", "curl/8.6.0")
            } catch {
              case ex: Exception => "DBpedia-Extraction-Framework/1.0 (https://github.com/dbpedia/extraction-framework; dbpedia@infai.org)"
            }
            if (customUserAgentEnabled) {
              request.setHeader("User-Agent", customUserAgentText)
            }
            val response = client.execute(request)
            val statusCode = response.getStatusLine.getStatusCode
            if (statusCode != 200) {
              throw new IOException(s"HTTP error ${statusCode}: ${response.getStatusLine.getReasonPhrase} for URL: $xmlUrl")
            }
            val xmlContent = scala.io.Source.fromInputStream(response.getEntity.getContent).mkString
            val xml = scala.xml.XML.loadString(xmlContent)
            XMLSource.fromXML(xml, language)
          } finally {
            client.close()
          }
        } else {
          // Use WikiSource with default API
          if (revid >= 0) WikiSource.fromRevisionIDs(List(revid), new URL(language.apiUri), language)
          else WikiSource.fromTitles(List(WikiTitle.parse(title, language)), new URL(language.apiUri), language)
        }
  }

    /**
     * Extracts a MediaWiki article
     */
    @GET
    @Path("extract")
def extract(@QueryParam("title") title: String, @QueryParam("revid") @DefaultValue("-1") revid: Long, @QueryParam("format") format: String, @QueryParam("extractors") extractors: String, @QueryParam("xmlUrl") xmlUrl: String, @Context headers: HttpHeaders): Response = {    import scala.collection.JavaConverters._
    import java.io.IOException

    if (title == null && revid < 0 && (xmlUrl == null || xmlUrl.isEmpty))
      throw new WebApplicationException(new Exception("title, revid, or xmlUrl must be given"), Response.Status.BAD_REQUEST)

    val requestedTypesList = headers.getAcceptableMediaTypes.asScala.map(_.toString).toList
    val browserMode = requestedTypesList.isEmpty || requestedTypesList.contains("text/html") || requestedTypesList.contains("application/xhtml+xml") || requestedTypesList.contains("text/plain")

    var finalFormat = format
    val acceptContentBest = requestedTypesList.map(selectFormatByContentType).headOption.getOrElse("unknownAcceptFormat")

    if (!acceptContentBest.equalsIgnoreCase("unknownAcceptFormat") && !browserMode)
      finalFormat = acceptContentBest
    val contentType = if (browserMode) selectInBrowserContentType(finalFormat) else selectContentType(finalFormat)

    // Keep as string, will be parsed later if it contains commas
val extractorName = Option(extractors).getOrElse("mappings")

    try {
      extractorName match {
        case "mappings" =>
          val writer = new StringWriter
          val formatter = createFormatter(finalFormat, writer)
          val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))

          val source = createSource(xmlUrl, revid, title)

          Server.getInstance().extractor.extract(source, destination, language, false)

          Response.ok(writer.toString)
            .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
            .build()

        case "custom" =>
          val writer = new StringWriter
          val formatter = createFormatter(finalFormat, writer)
          val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))

          val source = createSource(xmlUrl, revid, title)

          Server.getInstance().extractor.extract(source, destination, language, true)

          Response.ok(writer.toString)
            .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
            .build()

        case specificExtractor if specificExtractor.contains(",") =>
          // Handle comma-separated list of extractors
          val extractorNames = specificExtractor.split(",").map(_.trim).filter(_.nonEmpty)

          logger.info(s"Processing ${extractorNames.length} extractors: ${extractorNames.mkString(", ")}")

          // Accumulate errors instead of swallowing them
          val errors = new ArrayBuffer[String]()
          val collector = new QuadCollectorDestination()

          for (name <- extractorNames) {
            try {
              logger.info(s"Processing extractor: $name")

              val freshSource = createSource(xmlUrl, revid, title)

              Server.getInstance().extractWithSpecificExtractor(freshSource, collector, language, name)
              logger.info(s"Completed extractor: $name")
            } catch {
              case e: IllegalArgumentException =>
                val errorMsg = s"Extractor '$name' not found"
                logger.warning(s"$errorMsg: ${e.getMessage}")
                errors += errorMsg
              case e: Exception =>
                val errorMsg = s"Failed to extract with '$name': ${e.getMessage}"
                logger.severe(errorMsg)
                e.printStackTrace()
                errors += errorMsg
            }
          }

          // If any errors occurred, fail the request with detailed error message
          if (errors.nonEmpty) {
            val errorMessage = if (errors.size == 1) {
              errors.head
            } else {
              s"Multiple extractor errors:\n${errors.mkString("\n")}"
            }

            // Determine appropriate status code
            val statusCode = if (errors.exists(_.contains("not found"))) {
              Response.Status.BAD_REQUEST // 400 for invalid extractor names
            } else {
              Response.Status.INTERNAL_SERVER_ERROR // 500 for processing failures
            }

            throw new WebApplicationException(new Exception(errorMessage), statusCode)
          }

          val writer = new StringWriter
          val formatter = createFormatter(finalFormat, writer)
          val finalDestination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))

          finalDestination.open()
          finalDestination.write(collector.quads)
          finalDestination.close()

          Response.ok(writer.toString)
            .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
            .build()

        case specificExtractor =>
          val writer = new StringWriter
          val formatter = createFormatter(finalFormat, writer)
          val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))

          val source = createSource(xmlUrl, revid, title)

          Server.getInstance().extractWithSpecificExtractor(source, destination, language, specificExtractor)

          Response.ok(writer.toString)
            .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
            .build()
      }
    } catch {
      case e: IllegalArgumentException =>
        throw new WebApplicationException(e, 400)
      case e: IllegalStateException =>
        throw new WebApplicationException(e, 500)
      case e: WebApplicationException =>
        throw e
      case e: Exception =>
        val errorMsg = s"Extraction failed for language '${language.wikiCode}' with extractor '$extractorName': ${e.getMessage}"
        Extraction.logger.severe(errorMsg)
        // Log the full (unshortened) stack trace
        val sw = new java.io.StringWriter()
        val pw = new java.io.PrintWriter(sw)
        e.printStackTrace(pw)
        Extraction.logger.severe("Full stack trace:\n" + sw.toString())
        throw new WebApplicationException(new Exception(errorMsg, e), 500)
    }
  }

/**
 * Helper method to create formatter based on format type
 */
private def createFormatter(finalFormat: String, writer: StringWriter): Formatter = {
  finalFormat match {
    case "turtle-triples" => new TerseFormatter(false, true)
    case "turtle-quads" => new TerseFormatter(true, true)
    case "n-triples" => new TerseFormatter(false, false)
    case "n-quads" => new TerseFormatter(true, false)
    case "rdf-json" => new RDFJSONFormatter()
    case "trix" => TriX.writeHeader(writer, 2) // Returns TriXFormatter
    case _ => TriX.writeHeader(writer, 2) // Default to TriX
  }
}

  // map
  private def selectFormatByContentType(format: String): String = {

    (format match
    {
      case "text/xml" => "trix"
      case "text/turtle" => "turtle-triples"
      //case "text/nquads" => "turtle-quads" // this does not exist as mimetype
      case "application/n-triples" => "n-triples"
      case "application/n-quads" => "n-quads"
      case MediaType.APPLICATION_JSON => "rdf-json"
      //case "application/ld+json" => MediaType.APPLICATION_JSON
      case _ => "unknownAcceptFormat"
    })
  }

  // override content type in browser for some formats to display text instead of downloading a file, or
  private def selectInBrowserContentType(format: String): String = {

    format match
    {
      case "trix" => MediaType.APPLICATION_XML
      case "rdf-json" => MediaType.APPLICATION_JSON
      case _ => MediaType.TEXT_PLAIN
    }
  }

  // map format parameters to regular content types
    private def selectContentType(format: String): String = {

      format match
      {
        case "trix" => MediaType.APPLICATION_XML
        case "turtle-triples" => "text/turtle"
        case "turtle-quads" => "text/nquads"
        case "n-triples" => "application/n-triples"
        case "n-quads" => "application/n-quads"
        case "rdf-json" => MediaType.APPLICATION_JSON
        case _ => MediaType.TEXT_PLAIN
      }
    }

    /**
     * Extracts a MediaWiki article
     */
    @POST
    @Path("extract")
    @Consumes(Array("application/xml"))
    def extract(xml : Elem, @QueryParam("extractors") extractors: String, @Context headers: HttpHeaders): Response = {
        import scala.collection.JavaConverters._
        val requestedTypesList = headers.getAcceptableMediaTypes.asScala.map(_.toString).toList
        val browserMode = requestedTypesList.isEmpty || requestedTypesList.contains("text/html") || requestedTypesList.contains("application/xhtml+xml") || requestedTypesList.contains("text/plain")

        val acceptContentBest = requestedTypesList.map(selectFormatByContentType).headOption.getOrElse("unknownAcceptFormat")
        val finalFormat = if (!acceptContentBest.equalsIgnoreCase("unknownAcceptFormat") && !browserMode) acceptContentBest else "trix"
        val contentType = if (browserMode) selectInBrowserContentType(finalFormat) else selectContentType(finalFormat)

        val extractorName = Option(extractors).getOrElse("mappings")

        extractorName match {
          case "mappings" =>
            val writer = new StringWriter
            val formatter = createFormatter(finalFormat, writer)
            val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))
            val source = XMLSource.fromXML(xml, language)
            Server.getInstance().extractor.extract(source, destination, language, false)

            Response.ok(writer.toString)
              .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
              .build()

          case "custom" =>
            val writer = new StringWriter
            val formatter = createFormatter(finalFormat, writer)
            val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))
            val source = XMLSource.fromXML(xml, language)
            Server.getInstance().extractor.extract(source, destination, language, true)

            Response.ok(writer.toString)
              .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
              .build()

          case specificExtractor if specificExtractor.contains(",") =>
            // Handle comma-separated list of extractors
            val extractorNames = specificExtractor.split(",").map(_.trim).filter(_.nonEmpty)

            logger.info(s"POST: Processing ${extractorNames.length} extractors: ${extractorNames.mkString(", ")}")

            // Accumulate errors instead of swallowing them
            val errors = new ArrayBuffer[String]()
            val collector = new QuadCollectorDestination()

            extractorNames.foreach { name =>
              try {
                logger.info(s"POST: Processing extractor: $name")
                val freshSource = XMLSource.fromXML(xml, language)
                Server.getInstance().extractWithSpecificExtractor(freshSource, collector, language, name)
                logger.info(s"POST: Completed extractor: $name")
              } catch {
                case e: IllegalArgumentException =>
                  val errorMsg = s"Extractor '$name' not found"
                  logger.warning(s"POST: $errorMsg: ${e.getMessage}")
                  errors += errorMsg
                case e: Exception =>
                  val errorMsg = s"Failed to extract with '$name': ${e.getMessage}"
                  logger.severe(s"POST: $errorMsg")
                  e.printStackTrace()
                  errors += errorMsg
              }
            }

            // If any errors occurred, fail the request with detailed error message
            if (errors.nonEmpty) {
              val errorMessage = if (errors.size == 1) {
                errors.head
              } else {
                s"Multiple extractor errors:\n${errors.mkString("\n")}"
              }

              // Determine appropriate status code
              val statusCode = if (errors.exists(_.contains("not found"))) {
                Response.Status.BAD_REQUEST // 400 for invalid extractor names
              } else {
                Response.Status.INTERNAL_SERVER_ERROR // 500 for processing failures
              }

              throw new WebApplicationException(new Exception(errorMessage), statusCode)
            }

            // Write all collected quads at once
            val writer = new StringWriter
            val formatter = createFormatter(finalFormat, writer)
            val finalDestination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))

            finalDestination.open()
            finalDestination.write(collector.quads)
            finalDestination.close()

            Response.ok(writer.toString)
              .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
              .build()

          case specificExtractor =>
            val writer = new StringWriter
            val formatter = createFormatter(finalFormat, writer)
            val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))
            val source = XMLSource.fromXML(xml, language)
            Server.getInstance().extractWithSpecificExtractor(source, destination, language, specificExtractor)

            Response.ok(writer.toString)
              .header(HttpHeaders.CONTENT_TYPE, contentType + "; charset=UTF-8")
              .build()
        }
    }
}
