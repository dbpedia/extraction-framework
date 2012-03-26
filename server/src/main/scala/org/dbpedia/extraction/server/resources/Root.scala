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
          <head></head>
          <body>
            <h2>Server</h2>
            <p><a href="ontology/">Ontology</a></p>
            <p><a href="statistics/">Statistics</a> - <a href="mappings/">Mappings</a> - <a href="extraction/">Extractors</a></p>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={"statistics/" + code + "/"}>Statistics for {code}</a> - <a href={"mappings/" + code + "/"}>Mappings in {code}</a> - <a href={"extraction/" + code + "/"}>Extractor in {code}</a></p>
              }
            }
          </body>
        </html>
    }
    
    @GET @Path("statistics/") @Produces(Array("application/xhtml+xml"))
    def statistics = list("DBpedia Mapping Statistics", "Statistics", "Mapping Statistics for")

    @GET @Path("mappings/") @Produces(Array("application/xhtml+xml"))
    def mappings = list("DBpedia Template Mappings", "Mappings", "Mappings for")
    
    @GET @Path("extraction/") @Produces(Array("application/xhtml+xml"))
    def extraction = list("DBpedia Test Extractor", "Extractors", "Extractor for")
    
    private def list(title : String, header : String, prefix : String) =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head><title>DBpedia Test Extractor</title></head>
          <body>
            <h2>Extractors</h2>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={code + "/"}>Extractor in {code}</a></p>
              }
            }
          </body>
        </html>
    }
}
