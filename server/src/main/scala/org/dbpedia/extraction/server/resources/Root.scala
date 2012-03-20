package org.dbpedia.extraction.server.resources

import _root_.org.dbpedia.extraction.server.Server
import javax.ws.rs.{GET, Path, Produces}
import scala.xml.Text

@Path("/")
class Root
{
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">        
          <head>
          </head>
          <body>
            <h2>Server</h2>
            <p><a href="ontology">Ontology</a></p>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={"mappings/" + code + "/"}>Mappings in {code}</a> - <a href={"extraction/" + code + "/"}>Extractor in {code}</a></p>
              }
            }
          </body>
        </html>
    }
}
