//package org.dbpedia.extraction.server.resources
//
//import javax.ws.rs.{Produces, GET, Path}
//import org.dbpedia.extraction.server.Server
//import org.dbpedia.extraction.dump.ExtractionJob
//import java.util.Date
//
//@ Path("/dump")
//@Produces(Array("application/xml"))
//class Dump extends Base
//{
//    @GET
//    def get() =
//    {
//        <?xml-stylesheet type="text/xsl" href="/stylesheet.xsl"?>
//        <Server>{Server.currentJob.map(getJob).getOrElse("")}</Server>
//    }
//
//    private def getJob(job : ExtractionJob) =
//    {
//        val progress = job.progress
//
//        <Task>
//            <name>{job.label}</name>
//            <startTime>{new Date(progress.startTime)}</startTime>
//            <extractedPages>{progress.extractedPages}</extractedPages>
//            <failedPages>{progress.failedPages}</failedPages>
//            <pageTime>{(System.currentTimeMillis - progress.startTime).toDouble / progress.extractedPages}</pageTime>
//        </Task>
//    }
//
//    @GET
//    @Path("/stylesheet.xsl")
//    def getXsl() =
//    {
//        <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
//
//        <xsl:template match="/">
//          <html>
//          <head>
//            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
//            <meta http-equiv="refresh" content="1" />
//          </head>
//          <body>
//            <h2>Server</h2>
//            <xsl:for-each select="Server/Task">
//              <table border="1" cellpadding="3" cellspacing="0">
//                <tr bgcolor="#CCCCFF">
//                  <th colspan="2"><xsl:value-of select="name"/></th>
//                </tr>
//                <tr>
//                  <td bgcolor="#EEEEFF">Start time</td>
//                  <td><xsl:value-of select="startTime"/></td>
//                </tr>
//                <tr>
//                  <td bgcolor="#EEEEFF">Extracted pages</td>
//                  <td><xsl:value-of select="extractedPages"/></td>
//                </tr>
//                <tr>
//                  <td bgcolor="#EEEEFF">Failed pages</td>
//                  <td><xsl:value-of select="failedPages"/></td>
//                </tr>
//                <tr>
//                  <td bgcolor="#EEEEFF">Time per page</td>
//                  <td><xsl:value-of select="pageTime"/> ms</td>
//                </tr>
//              </table>
//            </xsl:for-each>
//          </body>
//          </html>
//        </xsl:template>
//
//        </xsl:stylesheet>
//    }
//}
