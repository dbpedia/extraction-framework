package org.dbpedia.extraction.dump.sql

import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.StringUtils
import scala.collection.mutable.HashMap
import java.lang.StringBuilder
import java.sql.Connection
import java.sql.SQLException
import scala.util.control.ControlThrowable

/**
 * This class is basically mwdumper's SqlWriter ported to Scala.
 */
class Importer(conn: Connection) {
  
  private val builders = new HashMap[String, StringBuilder]()
  
  private val blockSize = 2 << 20 // SQL statements are 1M chars long
  
  private var pages = 0
  
  private val time = System.currentTimeMillis
    
  def process(source: Source): Unit = {
    
    for (page <- source) {
      
      pages += 1
      
      val length = lengthUtf8(page.source)
      insert(new Page(page.id, page.revision, page.title, page.redirect, length))
      insert(new Revision(page.id, page.revision, length))
      insert(new Text(page.revision, page.source))
      
      if (pages % 2000 == 0) logPages()
    }
    
    flush()
    logPages()
  }
  
  private def lengthUtf8(text: String): Int = {
    var len = 0
    for (c <- text) {
      if (c < 0x80)
        len += 1
      else if (c < 0x800)
        len += 2
      else if (c < 0xD800 || c >= 0xE000)
        len += 3
      else {
        // All surrogate pairs are assumed to need 4 bytes. Simply count 2 bytes for each part. 
        // We could count 4 bytes and skip the low surrogate, but skipping while looping is ugly.
        len += 2
      }
    }
    len
  }
  
  private def insert(insert: Insert): Unit = {
    var builder = builders.getOrElse(insert.table, null)
    if (builder == null) {
      // add 64K - statement is built until it *exceeds* block size
      builder = new StringBuilder(blockSize + (2 << 16)) 
      builders.put(insert.table, builder)
      appendStatement(builder, insert)
    }
    else {
      // because of this silly comma, we can't really use getOrElseUpdate() above...
      builder.append(',')
      appendValues(builder, insert)
    }

    if (builder.length >= blockSize) {
      flush(insert.table)
    }
  }
  
  private def appendStatement(sql: StringBuilder, insert: Insert): Unit = {
    sql.append("INSERT INTO ")
    sql.append(insert.table)
    sql.append(" (")
    
    var first = true
    for (field <- insert.columns) {
      if (first) first = false else sql.append(',')
      sql.append(field)
    }
    sql.append(") VALUES ")
    appendValues(sql, insert)
  }
  
  private val replacements = {
    val rep = StringUtils.replacements('\\', "\"\'\\")
    rep(0) = "\\0"
    rep('\n') = "\\n"
    rep('\r') = "\\r"
    rep(27) = "\\Z" // ESC
    rep
  }
  
  private def appendValues(sql: StringBuilder, insert: Insert): Unit = {
    sql.append('(')
    
    var first = true
    for (field <- insert.values) {
      if (first) first = false else sql.append(',')
      field match {
        case null =>
          sql.append("NULL")
        case string: String =>
          sql.append('\'')
          StringUtils.escape(sql, string, replacements)
          sql.append('\'')
        case num: Int =>
          sql.append(num)
        case num: Long =>
          sql.append(num)
        case _ =>
          throw new IllegalArgumentException("unknown type '"+field.getClass+"'")
      }
    }
    
    sql.append(')')
  }
  
  private def flush(): Unit = {
    for (table <- builders.keys) flush(table)
  }
  
  private def flush(table: String): Unit = {
    val sql = builders.remove(table).get.toString
    val stmt = conn.createStatement()
    stmt.setEscapeProcessing(false)
    try {
      stmt.execute(sql)
    }
    catch {
      // throw our own exception that our XML parser won't catch
      case sqle: SQLException => throw new ImporterException(sqle)
    }
    finally stmt.close
  }
  
  private def logPages(): Unit = {
    val millis = System.currentTimeMillis - time
    println("imported "+pages+" pages in "+millis+" millis ("+(millis.toDouble/pages)+" millis per page)")
  }
  
}

/**
 * An exception that our XML parser won't catch - it does't catch ControlThrowable.
 */
class ImporterException(cause: SQLException)
extends SQLException(cause.getMessage(), cause.getSQLState(), cause.getErrorCode(), cause)
with ControlThrowable