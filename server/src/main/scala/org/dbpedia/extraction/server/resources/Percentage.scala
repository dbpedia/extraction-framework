package org.dbpedia.extraction.server.resources

import javax.ws.rs.{Produces, GET, PathParam, Path}
import scala.io.Source
import org.dbpedia.extraction.server.Server
import org.dbpedia.extraction.util.Language

/**
 * Created by IntelliJ IDEA.
 * User: Paul
 * Date: 14.07.11
 * Time: 18:15
 * To change this template use File | Settings | File Templates.
 */
@Path("/p")
class Percentage {
  
    @GET
    @Produces(Array("application/xhtml+xml"))
    def get =
    {
        <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
          <head>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
          </head>
          <body>
            {   
              val source = Source.fromFile(Server.instance.managers(Language.English).percentageFile);
              try source.getLines.mkString finally source.close
            }
          </body>
        </html>
    }
}