package org.dbpedia.extraction.server.resources

import _root_.java.net.{URL, URI}
import _root_.org.dbpedia.extraction.destinations.formatters.{NQuadsFormatter, NTriplesFormatter, TriXFormatter}
import _root_.org.dbpedia.extraction.util.Language
import javax.ws.rs._
import scala.xml.Elem
import scala.io.{Source,Codec}
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.wikiparser.WikiTitle
import org.dbpedia.extraction.destinations.StringDestination
import org.dbpedia.extraction.sources.{XMLSource, WikiSource}

/**
 * TODO: merge Extraction.scala and Mappings.scala
 */
@Path("/extraction/{lang}/")
class Extraction(@PathParam("lang") langCode : String)
{
    private val language = Language.getOrElse(langCode, throw new WebApplicationException(new Exception("invalid language "+langCode), 404))

    if(!Server.config.languages.contains(language))
        throw new WebApplicationException(new Exception("language "+langCode+" not configured in server"), 404)
    
    private def getTitle() : String = 
    {
      val default = "Berlin"
      val prefix = langCode+":"
      try
      {
        val in = getClass.getResourceAsStream("/extractionPageTitles.txt")
        try
        {
          val lines = Source.fromInputStream(in)(Codec.UTF8).getLines
          // just use get(), exception will be caught below
          lines.find(_.startsWith(prefix)).get.substring(prefix.length)
        } 
        finally in.close
      }
      catch { case e : Exception => default }
    }

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
        if(title == null) return "<error>Title not defined</error>"

        val formatter = format.toLowerCase.replace("-", "") match
        {
            case "ntriples" => new NTriplesFormatter()
            case "nquads" => new NQuadsFormatter()
            case _ => new TriXFormatter(new URI("../../stylesheets/trix.xsl"))
        }

        val source = WikiSource.fromTitles(
            WikiTitle.parse(title, language) :: Nil,
            new URL("http://" + language.wikiCode + ".wikipedia.org/w/api.php"),
            language)
        
        val destination = new StringDestination(formatter)
        Server.extractor.extract(source, destination, language)
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
        val destination = new StringDestination(new TriXFormatter(new URI("../../stylesheets/trix.xsl")))
        Server.extractor.extract(source, destination, language)
        destination.close()
        destination.toString
    }
}
