package org.dbpedia.extraction.dump.sql

import java.io.File
import org.dbpedia.extraction.sources.{Source,XMLSource}
import org.dbpedia.extraction.util.{Language,StringUtils}
import org.dbpedia.extraction.wikiparser.{Namespace,WikiTitle}
import scala.collection.mutable.HashMap
import java.lang.StringBuilder
import java.sql.Connection

/**
 * These classes are basically the object arrays used by mwdumper's SqlWriter15
 * ported to Scala.
 */
class Insert(val table: String, val columns: Array[String], val values: Array[Any])

class Page(pageId: Long, revId: Long, title: WikiTitle, redirect: WikiTitle, length: Int)
extends Insert(Page.table, Page.columns, Page.values(pageId, revId, title, redirect, length))
    
class Revision(pageId: Long, revId: Long, length: Int)
extends Insert(Revision.table, Revision.columns, Revision.values(pageId, revId, length))

class Text(revId: Long, text: String)
extends Insert(Text.table, Text.columns, Text.values(revId, text))

object Page {
  
  val table = "page"
    
  val columns = Array(
    "page_id", // int, page ID
    "page_namespace", // int, namespace code
    "page_title", // string, title without namespace, with _ for space
    "page_restrictions", // string, ignored (empty) for us
    "page_is_redirect", // 1 or 0
    "page_random", // 0 for us
    "page_latest", // int, revision ID
    "page_len" // int, text length in bytes. TODO: do we need this? can we set it to 0?
    // the other columns have default values
  )

  def values(pageId: Long, revId: Long, title: WikiTitle, redirect: WikiTitle, length: Int): Array[Any] = Array(
    pageId,
    title.namespace.code,
    title.encoded,
    "",
    if (redirect != null) 1 else 0,
    0,
    revId,
    length
  )
}

object Revision {
  
  val table = "revision"
    
  val columns = Array(
    "rev_id", // int, revision ID
    "rev_page", // int, page ID
    "rev_text_id", // int, we use revision ID
    "rev_comment", // string, ignored (empty) for us
    "rev_len" // text length... same as page.page_len???
    // the other columns have default values
  )

  def values(pageId: Long, revId: Long, length: Int): Array[Any] = Array(
    revId,
    pageId,
    revId,
    "",
    length
  )

}

object Text {
  
  val table = "text"
    
  val columns = Array(
    "old_id", // int, same as revision.rev_text_id, we use revision ID
    "old_text", // string, wikitext
    "old_flags" // string, always "utf-8" in our case
  )
    
  def values(revId: Long, text: String): Array[Any] = Array(
    revId,
    text,
    "utf-8"
  )
  
}
