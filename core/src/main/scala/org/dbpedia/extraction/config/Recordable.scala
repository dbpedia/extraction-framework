package org.dbpedia.extraction.config

import org.apache.log4j.Level
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode}

/**
  * Interface for objects which need to ba handled by the ExtractionRecorder (e.g. WikiPage, PageNode, Quad, Provenance etc.)
  */
trait Recordable[T] {
  def id: Long
  def recordEntries: Seq[RecordEntry[T]]
}


/**
  * This class provides the necessary attributes to record either a successful or failed extraction
  *
  * @param record - the Recordable
  * @param language - optional language of the recordable
  * @param msg - optional message
  * @param error - the throwable causing this record
  * @param level - log4j level
  */
case class RecordEntry[T] (
    record: Recordable[T],
    language: Language = Language.None,
    msg: String= null,
    error:Throwable = null,
    level: Level = Level.TRACE
 )

object RecordEntry{
  def copyEntry[T](record: RecordEntry[T]): RecordEntry[T] ={
    RecordEntry[T] (
      null.asInstanceOf[Recordable[T]],
      record.language,
      record.msg,
      record.error,
      record.level
    )
  }
}

class NodeEntry(p : Node, level: Level = Level.TRACE) extends RecordEntry[Node](
  record = p,
  level = level
)

class WikiPageEntry(p : PageNode, level: Level = Level.TRACE) extends RecordEntry[Node](
  record = p,
  level = level,
  language = p.title.language
)

class QuadEntry(q : Quad, level: Level = Level.TRACE) extends RecordEntry[Quad](
  record = q,
  level = level,
  language = Option(q.language) match{
    case Some(l) => Language(l)
    case None => Language.None
  }
)

class DefaultEntry(
  msg: String,
  error:Throwable = null,
  language: Language = Language.None,
  level: Level
) extends RecordEntry[DefaultRecordable](
  record = new DefaultRecordable,
  language = Language.None,
  msg,
  error,
  level
)

private[config] class DefaultRecordable extends Recordable[DefaultRecordable]{
  override val id: Long = DefaultRecordable.getId

  override def recordEntries = List()
}

private object DefaultRecordable{
  private var ids = 0L
  private def getId = {
    ids = ids+1
    ids
  }
}