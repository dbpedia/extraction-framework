package org.dbpedia.extraction.dump.sql

import org.dbpedia.extraction.sources.{WikiPage, Source}
import org.dbpedia.extraction.util.{Language, StringUtils}
import java.lang.StringBuilder
import java.sql.Connection
import java.sql.SQLException
import scala.util.control.ControlThrowable
import scala.Console._

/**
 * This class is basically mwdumper's SqlWriter ported to Scala.
 */
class Importer(conn: Connection, lang: Language = null) {
  
  private var failedPages = Map[Long, String]()
  
  private var pages = 0

  private var fails = 0
  
  private val time = System.currentTimeMillis
    
  def process(source: Source): Int = {
    
    for (page <- source) {
      pages += 1
      insertPageContent(page)
      if (pages % 2000 == 0) logPages()
    }

    logPages()
    println("The following pages were not correctly imported:")
    for(fail <- failedPages)
      println("page failed to import: " + fail._1)

    pages
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
  
  private def writeInsertStatement(insert: Insert): StringBuilder = {
    val sql = new StringBuilder()
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
    sql
  }
  
  private val replacements = {
    val rep = new Array[String](128)
    rep(0) = "\\0"
    rep('\n') = "\\n"
    rep('\r') = "\\r"
    rep(27) = "\\Z" // ESC
    rep('\"') = "\\\""
    rep('\'') = "\\\'"
    rep('\\') = "\\\\"
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
  
  private def insertPageContent(page: WikiPage): Unit = {
    val builder = new StringBuilder()
    val length = lengthUtf8(page.source)
    builder.append(writeInsertStatement(new Page(page.id, page.revision, page.title, page.redirect, length)))
    builder.append(';')
    builder.append(writeInsertStatement(new Revision(page.id, page.revision, length)))
    builder.append(';')
    builder.append(writeInsertStatement(new Text(page.id, page.revision, page.source)))
    builder.append(';')
    val stmt = conn.createStatement()
    stmt.setEscapeProcessing(false)
    try {
      stmt.execute(builder.toString)
    }
    catch {
      // throw our own exception that our XML parser won't catch
      case e: Throwable => println((if (lang != null) lang.wikiCode else "") + ": " + e.getClass.getSimpleName + " occurred for page: " + page.id + " - " + e.getMessage)      //catch unique key violations (which will occur...)
      failedPages += (page.id -> e.getMessage)
    }
    finally stmt.close()
  }
  
  private def logPages(): Unit = {
    val millis = System.currentTimeMillis - time
    println(if (lang != null) lang.wikiCode else "" + "imported "+(pages - (failedPages.size - fails))+
      " pages in "+StringUtils.prettyMillis(millis)+" ("+(millis.toDouble/pages)+" millis per page)")
    fails = failedPages.size
  }
}

/**
 * An exception that our XML parser won't catch - it does't catch ControlThrowable.
 */
class ImporterException(cause: SQLException)
extends SQLException(cause.getMessage(), cause.getSQLState(), cause.getErrorCode(), cause)
with ControlThrowable
