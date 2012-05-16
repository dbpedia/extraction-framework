package org.dbpedia.extraction.server.providers

import javax.ws.rs.ext.Provider
import javax.ws.rs.core.Response

@Provider
class ExceptionMapper extends javax.ws.rs.ext.ExceptionMapper[Throwable]
{
   	override def toResponse(exception : Throwable) : Response =
    {
        val html =
            <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
              </head>
              <body>
                <h2>Error</h2>
                <table>
                  <tr>
                    <td valign="top"><strong>Exception: </strong></td>
                    <td>{exception}</td>
                  </tr>
                  <tr>
                    <td valign="top"><strong>Stacktrace: </strong></td>
                    <td>{exception.getStackTraceString}</td>
                  </tr>
                </table>
              </body>
            </html>

        Response.serverError().entity(html).build()
    }
}
