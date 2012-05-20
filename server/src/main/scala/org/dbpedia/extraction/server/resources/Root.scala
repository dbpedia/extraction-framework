package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.server.Server
import javax.ws.rs.{GET, Path, Produces}
import org.dbpedia.extraction.server.util.PageUtils._
import scala.xml.Elem
import org.dbpedia.extraction.util.Language

@Path("/")
class Root
{
    @GET @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <title>DBpedia Server</title>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>DBpedia Server</h2>
            <p>
            <a href="ontology/">Ontology</a>
            </p>
            <p>
            <a href="statistics/">Statistics</a> - 
            <a href="mappings/">Mappings</a> - 
            <a href="extraction/">Extractors</a> - 
            <a href="ontology/labels/missing/">Missing labels</a>
            </p>
            {
              // we need toArray here to keep languages ordered.
              for(lang <- Server.instance.managers.keys.toArray; code = lang.wikiCode) yield
              {
                <p>
                <a href={"statistics/" + code + "/"}>Statistics for {code}</a> - 
                <a href={"mappings/" + code + "/"}>Mappings in {code}</a> - 
                <a href={"extraction/" + code + "/"}>Extractor in {code}</a> - 
                <a href={"ontology/labels/missing/" + code + "/"}>Missing labels for {code}</a>
                </p>
              }
            }
          </body>
        </html>
    }
    
    /**
     * List of all languages. Currently for the sprint code, may be useful for others.
     */
    @GET @Path("languages/") @Produces(Array("text/plain"))
    def languages = Server.instance.managers.keys.toArray.map(_.wikiCode).mkString(" ")
    
    @GET @Path("statistics/") @Produces(Array("application/xhtml+xml"))
    def statistics: Elem = {
      // we need toBuffer here to keep languages ordered.
      val links = Server.instance.managers.keys.toBuffer[Language].map(lang => (lang.wikiCode+"/", "Mapping Statistics for "+lang.wikiCode))
      links.insert(0, ("*/", "Mapping Statistics for all languages"))
      linkList("DBpedia Mapping Statistics", "Statistics", links)
    }

    @GET @Path("mappings/") @Produces(Array("application/xhtml+xml"))
    def mappings = languageList("DBpedia Template Mappings", "Mappings", "Mappings for")
    
    @GET @Path("extraction/") @Produces(Array("application/xhtml+xml"))
    def extraction = languageList("DBpedia Test Extractors", "Extractors", "Extractor for")
}
