package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.server.Server
import javax.ws.rs.{GET, Path, Produces}

@Path("/")
class Root
{
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head></head>
          <body>
            <h2>Server</h2>
            <p><a href="ontology/">Ontology</a></p>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={"mappings/" + code + "/"}>Mappings in {code}</a> - <a href={"extraction/" + code + "/"}>Extractor in {code}</a></p>
              }
            }
          </body>
        </html>
    }
    
    @GET
    @Path("mappings/")
    @Produces(Array("application/xhtml+xml"))
    def mappings =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head></head>
          <body>
            <h2>Mappings</h2>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={code + "/"}>Mappings in {code}</a></p>
              }
            }
          </body>
        </html>
    }
    
    @GET
    @Path("extraction/")
    @Produces(Array("application/xhtml+xml"))
    def extraction =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head></head>
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
