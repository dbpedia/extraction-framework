package org.dbpedia.extraction.live.export

import java.io.{File, Writer}
import java.net.Authenticator
import java.sql._

import org.apache.log4j.Logger
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, UriPolicy}
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage.{JSONCache, DBpediaSQLQueries, JDBCPoolConnection}
import org.dbpedia.extraction.util.{ConfigUtils, ProxyAuthenticator, IOUtils}
import org.dbpedia.extraction.util.RichFile._

/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/18/14 4:33 PM
 */
class DumpExport {
  val logger: Logger = Logger.getLogger(classOf[DumpExport])

  def export(filename: String) {

    val destination: Destination = new WriterDestination(writer(new File(filename)), new TerseFormatter(false,true,policies))

    destination.open()

    var conn: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null

    try {
      conn = JDBCPoolConnection.getCachePoolConnection
      conn.setAutoCommit(false);

      //http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html
      stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(50);//Integer.MIN_VALUE);

      rs = stmt.executeQuery(DBpediaSQLQueries.getJSONCacheSelectAll);

      while (rs.next()) {

        val jsonBlob: Blob = rs.getBlob("json")
        val jsonData = jsonBlob.getBytes(1, jsonBlob.length.asInstanceOf[Int])
        val jsonString = new String(jsonData).trim
        if (!jsonString.isEmpty) {
          val quads = JSONCache.getTriplesFromJson(new String(jsonData))
          destination.write(quads)
        }
      }
      rs.close();
      stmt.close();
    } catch {
      case  e: Exception => {
        logger.warn(e.getMessage(), e);
      }
    }
    finally {
      try {
        if (rs != null) rs.close
      } catch {
        case e: Exception => {
          logger.warn(e.getMessage, e)
        }
      }

      try {
        if (stmt != null) stmt.close
      } catch {
        case e: Exception => {
          logger.warn(e.getMessage, e)
        }
      }

      try {
        if (conn != null) conn.close
      } catch {
        case e: Exception => {
          logger.warn(e.getMessage, e)
        }
      }
    }

    destination.close()
  }

  val policies = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }


  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(wrapFile(file))
  }

}

object DumpExport {


  def main(args: scala.Array[String]): Unit = {

    require(args != null && args.length >= 1 && args(0).nonEmpty, "missing required argument: dump file name")
    Authenticator.setDefault(new ProxyAuthenticator())

    new DumpExport().export(args(0))
  }
}
