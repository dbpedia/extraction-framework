package org.dbpedia.extraction.server.resources

import java.net.{URL, URI}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.util.Language
import javax.ws.rs._
import java.util.logging.{Logger,Level}
import scala.xml.Elem
import scala.io.{Source,Codec}
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.destinations.StringDestination
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}
import stylesheets.TriX

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
         <head>
           <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
         </head>
         <body>
           <h2>Extract a page</h2>
           <form action="extract" method="get">
             Title:
             <input type="text" name="title" value={ getTitle }/>
             <select name="format">
               <option value="Trix">Trix</option>
               <option value="N-Triples">N-Triples</option>
               <option value="N-Quads">N-Quads</option>
             </select>
             <input type="submit" value="Extract" />
           </form>
         </body>
       </html>
    }

    /**
     * Extracts a MediaWiki article
     */
    @GET
    @Path("extract")
    @Produces(Array("application/xml"))
    def extract(@QueryParam("title") title : String, @DefaultValue("trix") @QueryParam("format") format : String) : String =
    {
        //TODO return HTTP error code 400 - bad request
        if (title == null) return "<error>Title not defined</error>"

        val formatter = format.toLowerCase.replace("-", "") match
        {
            case "ntriples" => TerseFormatter.NTriplesIris
            case "nquads" => TerseFormatter.NQuadsIris
            case _ => TriX.formatter(2)
        }

        val source = WikiSource.fromTitles(
            WikiTitle.parse(title, language) :: Nil,
            new URL("http://" + language.wikiCode + ".wikipedia.org/w/api.php"),
            language)
        
        val destination = new StringDestination(formatter)
        Server.instance.extractor.extract(source, destination, language)
        destination.close()
        destination.toString
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
        val source = XMLSource.fromXML(xml)
        val destination = new StringDestination(TriX.formatter(2))
        Server.instance.extractor.extract(source, destination, language)
        destination.close()
        destination.toString
    }
}
