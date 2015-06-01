package org.dbpedia.extraction.live.export

import java.io.{File, Writer}
import java.net.Authenticator
import java.sql._
import java.util.concurrent._

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.{TerseFormatter, UriPolicy}
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage.{JDBCUtil, JSONCache, DBpediaSQLQueries, JDBCPoolConnection}
import org.dbpedia.extraction.util.RichFile._
import org.dbpedia.extraction.util.{IOUtils, ProxyAuthenticator}
import org.slf4j.{Logger, LoggerFactory}


/**
 * Description
 *
 * @author Dimitris Kontokostas
 * @since 9/18/14 4:33 PM
 */
class DumpExport(val filename: String, val threads: Integer) {
  val logger: Logger = LoggerFactory.getLogger(classOf[DumpExport])

  val policies = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }

  val destination: Destination = new WriterDestination(writer(new File(filename)), new TerseFormatter(false, true, policies))

  // Max threads in thread pool queu 4 x running threads
  val linkedBlockingDeque: BlockingQueue[Runnable] = new LinkedBlockingDeque[Runnable](threads * 4);
  val executorService: ExecutorService = new ThreadPoolExecutor(threads, threads, 30,
      TimeUnit.SECONDS, linkedBlockingDeque, new ThreadPoolExecutor.CallerRunsPolicy());

  def export() {

    JDBCUtil.execSQL("SET names utf8");

    destination.open()


    var conn: Connection = null
    var stmt: Statement = null
    var rs: ResultSet = null

    try {
      conn = JDBCPoolConnection.getCachePoolConnection
      conn.setAutoCommit(false);

      //http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html
      stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(Integer.MIN_VALUE);

      rs = stmt.executeQuery(DBpediaSQLQueries.getJSONCacheSelectAll);

      while (rs.next()) {

        val jsonBlob: Blob = rs.getBlob("json")
        val jsonData = jsonBlob.getBytes(1, jsonBlob.length.asInstanceOf[Int])
        val jsonString = new String(jsonData).trim
        if (!jsonString.isEmpty) {
          // Submit job to thread pool
          executorService.execute(new QuadProcessWorker(destination,jsonString))
          //val quads = JSONCache.getTriplesFromJson(new String(jsonString))
          //destination.write(quads)
        }
      }
      rs.close();
      stmt.close();
    } catch {
      case e: Exception => {
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

      executorService.shutdown();
      try {
        executorService.awaitTermination(Long.MaxValue, TimeUnit.NANOSECONDS);
      } catch {
        case e : InterruptedException  => {
          logger.error("Error in thread termination", e)
        }
      }

      destination.close()
    }


  }




  private def writer(file: File): () => Writer = {
    () => IOUtils.writer(wrapFile(file))
  }


}

class QuadProcessWorker(val destination: Destination, val jsonString: String) extends Runnable {

  override def run(): Unit = {
    val quads = JSONCache.getTriplesFromJson(new String(jsonString))
    destination.write(quads)
  }
}

object DumpExport {


  def main(args: scala.Array[String]): Unit = {

    require(args != null && args.length == 2 && args(0).nonEmpty && args(1).nonEmpty, "missing required argument: $ {dump file name} {threads Number}")
    val threads: Int = Integer.parseInt(args(1))

    new DumpExport(args(0), threads).export()
  }
}
