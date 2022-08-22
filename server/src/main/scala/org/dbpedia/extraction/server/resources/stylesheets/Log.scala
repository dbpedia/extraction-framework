package org.dbpedia.extraction.server.resources.stylesheets

import javax.ws.rs.{Produces, GET, Path}
import scala.xml.{Node,Elem,ProcInstr}
import org.dbpedia.extraction.server.Server

object Log
{
  /**
   * @param number of "../" steps to prepend to the path to "stylesheets/log.xsl"
   */
  def header(parents : Int): Node = {
    // if there are slashes in the title, the stylesheets are further up in the directory tree
    val stylesheetUri = "../" * parents + "stylesheets/log.xsl"
    // <?xml-stylesheet type="text/xsl" href="{logUri}"?>
    new ProcInstr("xml-stylesheet", "type=\"text/xsl\" href=\"" + stylesheetUri + "\"")
  }
}

@Path("/stylesheets/log.xsl")
class Log
{
    @GET
    @Produces(Array("text/xsl"))
    def get : Elem =
    {
        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
          <xsl:template match="log">
            <html>
              <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
              </head>
              <body>
                <h2>Validation Results</h2>
                  <xsl:choose>
                    <xsl:when test="count(record) = 0">
                      <span style="color:#04B404">no validation errors</span>
                    </xsl:when>
                    <xsl:otherwise>
                      <span style="color:#B40404"><xsl:value-of select="count(record)"/> validation error(s)</span>
                      <ul>
                        <xsl:for-each select="record">
                          <li>
                          <xsl:if test="param">
                            <a href={"{concat('"+Server.instance.paths.pagesUrl+"/', param)}"}><xsl:value-of select="param"/></a>: 
                          </xsl:if>
                          <xsl:value-of select="message"/>
                          </li>
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
