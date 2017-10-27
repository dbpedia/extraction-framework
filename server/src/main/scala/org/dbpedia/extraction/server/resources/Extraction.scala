package org.dbpedia.extraction.server.resources

import java.net.{URI, URL}

import org.dbpedia.extraction.destinations.formatters.{RDFJSONFormatter, TerseFormatter}
import org.dbpedia.extraction.util.{IOUtils, Language, RichFile}
import javax.ws.rs._
import javax.ws.rs.core.{HttpHeaders, MediaType, Response}
import java.util.logging.{Level, Logger}

import scala.xml.Elem
import scala.io.{Codec, Source}
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.destinations.{DeduplicatingDestination, DestinationUtils, WriterDestination}
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import stylesheets.TriX
import java.io.{File, StringWriter}

import scala.collection.mutable.ListBuffer

object Extraction
{
    private val logger = Logger.getLogger(getClass.getName)
      
    val lines : Map[String, String] = {
        val file = "/extractionPageTitles.txt"
        try {
            // ugly - returns null if file not found, which leads to NPE later
            val in = getClass.getResourceAsStream(file)
            try {
                val titles = 
                  for (line <- Source.fromInputStream(in)(Codec.UTF8).getLines 
                    if line.startsWith("[[") && line.endsWith("]]") && line.contains(':')
                  ) yield {
                    val colon = line.indexOf(':')
                    (line.substring(2, colon), line.substring(colon + 1, line.length - 2))
                  }
                titles.toMap
            } 
            finally in.close
        }
        catch { 
            case e : Exception =>
                logger.log(Level.WARNING, "could not load extraction page titles from classpath resource "+file, e)
                Map()
        }
    }

  def getFormatter(format: String, writer: StringWriter = null) = format match
  {
    case "turtle-triples" => new TerseFormatter(false, true)
    case "turtle-quads" => new TerseFormatter(true, true)
    case "n-triples" => new TerseFormatter(false, false)
    case "n-quads" => new TerseFormatter(true, false)
    case "rdf-json" => new RDFJSONFormatter()
    case _ => TriX.writeHeader(writer, 2)
  }
}

/**
 * TODO: merge Extraction.scala and Mappings.scala
 */
@Path("/extraction/{lang}/")
class Extraction(@PathParam("lang") langCode : String)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if(!Server.instance.managers.contains(language))
        throw new WebApplicationException(new Exception("language "+langCode+" not configured in server"), 404)
    
    private def getTitle : String = Extraction.lines.getOrElse(langCode, "Berlin")

    @GET
    @Produces(Array("application/xhtml+xml"))
    def get = 
    {
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
                Output format<br/>
                <select name="format">
                  <option value="trix">Trix</option>
                  <option value="turtle-triples">Turtle-Triples</option>
                  <option value="turtle-quads">Turtle-Quads</option>
                  <option value="n-triples">N-Triples</option>
                  <option value="n-quads">N-Quads</option>
                  <option value="rdf-json">RDF/JSON</option>
                </select><br/>
                <select name="extractors">
                <option value="mappings">Mappings Only</option>
                <option value="custom">All Enabled Extractors </option>
                </select><br/>
              <input type="submit" value="Extract" />
            </form>
            </div>
           </div>
         </body>
       </html>
    }

    /**
     * Extracts a MediaWiki article
     */
    @GET
    @Path("extract")
    def extract(@QueryParam("title") title: String, @QueryParam("revid") @DefaultValue("-1") revid: Long, @QueryParam("format") format: String, @QueryParam("extractors") extractors: String) : Response =
    {
        if (title == null && revid < 0) throw new WebApplicationException(new Exception("title or revid must be given"), Response.Status.NOT_FOUND)
        
        val writer = new StringWriter

        val formatter = Extraction.getFormatter(format, writer)

      val customExtraction = extractors match
      {
        case "mappings" => false
        case "custom" => true
        case _ => false
      }

        val source = 
          if (revid >= 0) WikiSource.fromRevisionIDs(List(revid), new URL(language.apiUri), language)
          else WikiSource.fromTitles(List(WikiTitle.parse(title, language)), new URL(language.apiUri), language)

        // See https://github.com/dbpedia/extraction-framework/issues/144
        // We should mimic the extraction framework behavior
        val destination = new DeduplicatingDestination(new WriterDestination(() => writer, formatter))
        Server.instance.extractor.extract(source, destination, language, customExtraction)

        Response.ok(writer.toString)
          .header(HttpHeaders.CONTENT_TYPE, selectContentType(format)+"; charset=UTF-8" )
          .build()
    }

  @GET
  @Path("extract-all")
  def extractAll(@QueryParam("url") url: String, @QueryParam("format") format: String, @QueryParam("extractors") extractors: String){
    val titles = new ListBuffer[String]()
    var lastTitle = ""
    val zw = """^\s*<http://dbpedia.org/resource/([^>]+).*""".r

    val formatter = Extraction.getFormatter(format, null)
    val destination = DestinationUtils.createWriterDestination(new RichFile(new File("/home/chile/sci-graph-links/LD/data-samples/random/DBpedia90000-enriched.ttl.bz2")), formatter)

    val u = if(new URI(url).getRawAuthority != null)
      new URI(url).toURL.openStream()
        else
      new URI("file://" + url).toURL.openStream()

    IOUtils.readLines(u, Codec.UTF8.charSet){ line =>
      if(line != null) {
        zw.findFirstMatchIn(line) match {
          case Some(m) if m.group(1) != lastTitle =>
            titles.append(m.group(1))
            lastTitle = m.group(1)
          case _ =>
        }
      }
    }

    destination.open()
    var c = 0
    for(title <- titles) {
      Server.instance.extractor.extract(title, destination, Language.English)
      c = c+1
      if(c%100 == 0)
        System.out.println(c + " titles extracted")
    }
    destination.close()
  }

    private def selectContentType(format: String): String = {

      format match
      {
        case "trix" => MediaType.APPLICATION_XML
        case _ => MediaType.TEXT_PLAIN
      }
    }

    /**
     * Extracts a MediaWiki article
     */
    @POST
    @Path("extract")
    @Consumes(Array("application/xml"))
    @Produces(Array("application/xml"))
    def extract(xml : Elem) =
    {
        val writer = new StringWriter
        val formatter = TriX.writeHeader(writer, 2)
        val source = XMLSource.fromXML(xml, language)
        val destination = new WriterDestination(() => writer, formatter)
        
        Server.instance.extractor.extract(source, destination, language)
        
        writer.toString
    }
}
