package org.dbpedia.extraction.config

import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, PageNode}

/**
  * Interface for objects which need to ba handled by the ExtractionRecorder (e.g. WikiPage, PageNode, Quad, Provenance etc.)
  */
trait Recordable[T] {
  def id: Long
  def recordEntries: List[RecordEntry[T]]
}


/**
  * This class provides the necessary attributes to record either a successful or failed extraction
  *
  * @param record - the Recordable
  * @param cause - the cause for recording it
  * @param language - optional language of the recordable
  * @param msg - optional message
  * @param error - the throwable causing this record
  */
case class RecordEntry[T] (
     record: Recordable[T],
     cause: RecordCause.Value = RecordCause.Info,
     language: Language = Language.None,
     msg: String= null,
     error:Throwable = null
 )

object RecordEntry{
  def copyEntry[T](record: RecordEntry[T]): RecordEntry[T] ={
    RecordEntry[T] (
      null.asInstanceOf[Recordable[T]],
      record.cause,
      record.language,
      record.msg,
      record.error
    )
  }
}
/**
  *
  */
object RecordCause extends Enumeration {
  val Provenance, Internal, Info, Warning, Exception, Fatal = Value
}

class NodeEntry(p : Node, cause: RecordCause.Value = RecordCause.Info) extends RecordEntry[Node](
  record = p,
  cause = cause
)

class WikiPageEntry(p : PageNode, cause: RecordCause.Value = RecordCause.Info) extends RecordEntry[Node](
  record = p,
  cause = cause,
  language = p.title.language
)

class QuadEntry(q : Quad, cause: RecordCause.Value = RecordCause.Info) extends RecordEntry[Quad](
  record = q,
  cause = cause,
  language = Option(q.language) match{
    case Some(l) => Language(l)
    case None => Language.None
  }
)

class DefaultEntry(
  msg: String,
  cause: RecordCause.Value = RecordCause.Info,
  error:Throwable = null,
  language: Language = Language.None
) extends RecordEntry[DefaultRecordable](
  record = new DefaultRecordable,
  cause = cause,
  language = Language.None,
  msg,
  error
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