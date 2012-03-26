package org.dbpedia.extraction.server.resources

import org.dbpedia.extraction.server.Server
import javax.ws.rs._

@Path("/statistics/")
class Statistics 
{

  @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head></head>
          <body>
            <h2>Statistics</h2>
            {
              for(lang <- Server.config.languages; code = lang.wikiCode) yield
              {
                  <p><a href={code + "/"}>Mapping Statistics for {code}</a></p>
              }
            }
          </body>
        </html>
    }
    
}