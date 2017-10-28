package org.dbpedia.extraction.dump.sql

import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util._
import java.lang.StringBuilder
import java.sql.Connection
import java.sql.SQLException

import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause, WikiPageEntry}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiPage}

import scala.util.control.ControlThrowable

/**
 * This class is basically mwdumper's SqlWriter ported to Scala.
 */
class Importer(conn: Connection, lang: Language, recorder: ExtractionRecorder[PageNode]) {


  def process(source: Source): Int = {
    for (page <- source) {
      insertPageContent(page)
      recorder.record(new WikiPageEntry(page))
    }

    recorder.printLabeledLine("Retrying all failed pages:", RecordCause.Warning, lang, null, noLabel = true)

    recorder.listFailedPages(lang) match{
      case None =>
      case Some(fails) => for(fail <- fails)
        fail match{
          case w: WikiPage => if(!insertPageContent(w))
            recorder.printLabeledLine("Retrying all failed pages:", RecordCause.Warning, lang)
          case _ =>
        }
    }

    recorder.successfulPages(lang).toInt
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
  
  private def insertPageContent(page: WikiPage): Boolean = {
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
      true
    }
    catch {
      // throw our own exception that our XML parser won't catch
      case e: Throwable =>
        recorder.failedRecord(page, e, page.title.language)
        false
    }
    finally stmt.close()
  }
}

/**
 * An exception that our XML parser won't catch - it does't catch ControlThrowable.
 */
class ImporterException(cause: SQLException)
extends SQLException(cause.getMessage, cause.getSQLState, cause.getErrorCode, cause)
with ControlThrowable
