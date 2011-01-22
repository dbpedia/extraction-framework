package org.dbpedia.extraction.server.resources

import _root_.org.dbpedia.extraction.server.Server
import javax.ws.rs.{GET, Path, Produces}

@Path("/")
class Root extends Base
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
            <a href="ontology">Ontology</a><br/>
            {
              for(lang <- Server.config.languages) yield
              {
                <a href={"mappings/" + lang.wikiCode + "/"}>{"Mappings in " + lang}</a><br/>
                <a href={"extraction/" + lang.wikiCode + "/"}>{"Extractor in " + lang}</a><br/>
              }
             }
          </body>
        </html>
    }
}
