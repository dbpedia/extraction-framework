package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.server.Server
import javax.ws.rs.{GET, Path, Produces}

@Path("/")
class Root
{
    @GET @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>Server</h2>
            <p><a href="ontology/">Ontology</a></p>
            <p><a href="statistics/">Statistics</a> - <a href="mappings/">Mappings</a> - <a href="extraction/">Extractors</a></p>
            {
              // we need toArray here to keep languages ordered.
              for(lang <- Server.instance.managers.keys.toArray; code = lang.wikiCode) yield
              {
                <p><a href={"statistics/" + code + "/"}>Statistics for {code}</a> - <a href={"mappings/" + code + "/"}>Mappings in {code}</a> - <a href={"extraction/" + code + "/"}>Extractor in {code}</a></p>
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
    def statistics = list("DBpedia Mapping Statistics", "Statistics", "Mapping Statistics for")

    @GET @Path("mappings/") @Produces(Array("application/xhtml+xml"))
    def mappings = list("DBpedia Template Mappings", "Mappings", "Mappings for")
    
    @GET @Path("extraction/") @Produces(Array("application/xhtml+xml"))
    def extraction = list("DBpedia Test Extractors", "Extractors", "Extractor for")
    
    private def list(title : String, header : String, prefix : String) =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <title>{title}</title>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            <h2>{header}</h2>
            {
              // we need toArray here to keep languages ordered.
              for(lang <- Server.instance.managers.keys.toArray; code = lang.wikiCode) yield
              {
                  <p><a href={code + "/"}>{prefix} {code}</a></p>
              }
            }
          </body>
        </html>
    }
}
