package org.dbpedia.extraction.live.export

import java.sql._
import org.dbpedia.extraction.live.storage._
import org.slf4j.{Logger, LoggerFactory}

/**
 * Migrates the cache from MySQL to MongoDB.
 *
 * @author Andre Pereira
 * @since 06/17/15 21:33 PM
 */
class Migrator() {
  val logger: Logger = LoggerFactory.getLogger(classOf[Migrator])

  def migrate() {
    var conn: Connection = null
    var stmt: Statement = null
    var all: ResultSet = null
    try {
      JDBCUtil.execSQL("SET names utf8");
      conn = JDBCPoolConnection.getCachePoolConnection
      conn.setAutoCommit(false);

      //http://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html
      stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
      stmt.setFetchSize(Integer.MIN_VALUE);

      all = stmt.executeQuery("SELECT * FROM DBPEDIALIVE_CACHE");

      while (all.next()) {
        val jsonBlob: Blob = all.getBlob("json")
        val jsonData = jsonBlob.getBytes(1, jsonBlob.length.asInstanceOf[Int])
        val jsonString = new String(jsonData).trim

        val subjectsBlob: Blob = all.getBlob("subjects")
        val subjectsData = subjectsBlob.getBytes(1, subjectsBlob.length.asInstanceOf[Int])
        val subjectsString = new String(subjectsData)

        val diffBlob: Blob = all.getBlob("diff")
        val diffData = diffBlob.getBytes(1, diffBlob.length.asInstanceOf[Int])
        val diffString = new String(diffData)

        val pageID = all.getInt("pageID")
        val title = all.getString("title")
        val updated_sql = all.getTimestamp("updated")
        val timesUpdated = all.getInt("timesUpdated")
        val error = all.getInt("error")

        val updated = sql2calendar(updated_sql);

        MongoUtil.full_insert(pageID, title, updated, "" + timesUpdated, jsonString, subjectsString, diffString, error)
      }
    } catch {
      case e: Exception => {
        logger.warn(e.getMessage(), e);
      }
    }finally{
      try {
        if (all != null) all.close
      }
      catch {
        case e: Exception => {
          logger.warn(e.getMessage)
        }
      }
      try {
        if (conn != null) conn.close
      }
      catch {
        case e: Exception => {
          logger.warn(e.getMessage)
        }
      }
    }
  }

  /*
  * SQL:        2015-03-06 18:57:16
  * Our format: 2015/03/06 18:57:16
  * */
  private def sql2calendar(timestamp: Timestamp): String = {
    var ts = timestamp.toString();
    ts = ts.replace('-', '/');
    //toString appends '.0' to the seconds value, but we don't want it
    ts = ts.replace(".0", "");
    return ts;
  }
}
