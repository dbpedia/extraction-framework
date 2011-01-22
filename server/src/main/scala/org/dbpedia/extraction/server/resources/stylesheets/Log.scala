package org.dbpedia.extraction.server.resources.stylesheets

import javax.ws.rs.{Produces, GET, Path}
import xml.Elem

@Path("/stylesheets/log.xsl")
class Log
{
    @GET
    @Produces(Array("application/xslt+xml"))
    def get : Elem =
    {
        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
          <xsl:template match="log">
            <html>
              <body>
                <h2>Validation Results</h2>
                  <xsl:choose>
                    <xsl:when test="count(record) = 0">
                      <span style="color:#04B404">no validation errors</span>
                    </xsl:when>
                    <xsl:otherwise>
                      <ul>
                        <xsl:for-each select="record">
                          <li><xsl:value-of select="message"/></li>
                        </xsl:for-each>
                      </ul>
                    </xsl:otherwise>
                  </xsl:choose>
              </body>
            </html>
          </xsl:template>
        </xsl:stylesheet>
    }
}
