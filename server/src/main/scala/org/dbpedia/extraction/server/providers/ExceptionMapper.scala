package org.dbpedia.extraction.server.providers

import java.io.{PrintWriter, StringWriter}
import javax.ws.rs.ext.Provider
import javax.ws.rs.core.Response
import org.dbpedia.extraction.server.resources.ServerHeader

@Provider
class ExceptionMapper extends javax.ws.rs.ext.ExceptionMapper[Throwable]
{
      var sw = new StringWriter()
      sw.synchronized()
       override def toResponse(exception : Throwable) : Response =
    {
        val html =
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
              {ServerHeader.getHeader("DBpedia Test Extractors")}
              <body>
                <div class="row">
                  <div class="col-md-3 col-md-offset-5">
                    <h2>Error</h2>
                    <table>
                      <tr>
                        <td valign="top"><strong>Exception: </strong></td>
                        <td>{exception}</td>
                      </tr>
                      <tr>
                        <td valign="top"><strong>Stacktrace: </strong></td>
                        <td>{exception.printStackTrace(new PrintWriter(sw));sw.toString()}</td>
                      </tr>
                    </table>
                  </div>
                </div>
              </body>
            </html>

        Response.serverError().entity(html).build()
    }
}
