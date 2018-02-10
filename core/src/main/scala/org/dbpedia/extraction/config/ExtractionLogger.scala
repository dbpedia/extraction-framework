package org.dbpedia.extraction.config

import org.apache.log4j.spi.LoggerRepository
import org.apache.log4j.{Category, Level, Logger, Priority}
import org.dbpedia.extraction.annotations.SoftwareAgentAnnotation
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.ontology.DBpediaNamespace
import org.dbpedia.extraction.util.Language

import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect._
import scala.util.{Failure, Success, Try}

class ExtractionLogger protected(logger: Logger, val recorder: ExtractionRecorder[_]) extends Logger(logger.getName){

  def record(recordable: Recordable[_]): Unit ={
    this.record(recordable.recordEntries:_*)
  }

  def record(records: RecordEntry[_] *): Unit ={
    records.foreach(r => {
      super.log(ExtractionLogger.RECORD, overrideRecordLevel(r))
    })
  }

  /**
    * decides if and which record entries to publish
    * @param recordable
    * @param level
    * @param lang
    * @param msg
    * @param t
    * @tparam T
    */
  private def recordableX[T](recordable: Recordable[T], level: Level, lang: Language, msg: String = null, t: Throwable = null): Unit ={
    //record each contained RecordEntry
    if(recordable.recordEntries != null)
      recordable.recordEntries.foreach(r => super.log(level, overrideRecordLevel(r, level)))
    //if there is something to tell create a new RecordEntry
    if(msg != null || t != null)
      super.log(level, new RecordEntry[T](recordable, lang, msg, t, level))
  }

  def record(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, ExtractionLogger.RECORD, lang, msg, t)
  }

  def trace(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.TRACE, lang, msg, t)
  }

  def info(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.INFO, lang, msg, t)
  }

  def warn(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.WARN, lang, msg, t)
  }

  def error(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.ERROR, lang, msg, t)
  }

  def fatal(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.FATAL, lang, msg, t)
  }

  def debug(recordable: Recordable[_], lang: Language, t: Throwable = null, msg: String = null): Unit = {
    recordableX(recordable, Level.DEBUG, lang, msg, t)
  }

  private def overrideRecordLevel(record: RecordEntry[_], level: Level = ExtractionLogger.RECORD): RecordEntry[_] ={
    if(record.level == null || level.isGreaterOrEqual(record.level))
      record.copy(level = level)
    else
      record
  }

  override def forcedLog(fqcn: String, level: Priority, message: scala.Any, t: Throwable): Unit = {
    val m = message match{
      case re: RecordEntry[_] =>
        overrideRecordLevel(re, level.asInstanceOf[Level])
      case re: Recordable[_] =>
        re.recordEntries.map(x => overrideRecordLevel(x, level.asInstanceOf[Level]))
      case re =>  re
    }
    super.forcedLog(fqcn, level, m, t)
  }
}


object ExtractionLogger{
  /**
    * The fully qualified name of the Category class. See also the
    * getFQCN method. */
  private val extractionRecorder = new mutable.HashMap[ClassTag[_], mutable.HashMap[Language, ExtractionRecorder[_]]]()

  private val RECORD_INT = 2000

  val RECORD = getNewLevelInstance(RECORD_INT, "RECORD")

  def getExtractionRecorder[T: ClassTag](lang: Language, datasets : Seq[Dataset] = Seq()): ExtractionRecorder[T] = {
    extractionRecorder.get(classTag[T]) match{
      case Some(s) => s.get(lang) match {
        case None =>
          s(lang) = Config.universalConfig.getDefaultExtractionRecorder[T](lang, 2000, null, null, datasets)
          s(lang).asInstanceOf[ExtractionRecorder[T]]
        case Some(er) => er.asInstanceOf[ExtractionRecorder[T]]
      }
      case None =>
        extractionRecorder(classTag[T]) = new mutable.HashMap[Language, ExtractionRecorder[_]]()
        getExtractionRecorder[T](lang, datasets)
    }
  }

  private val loggerMap = new mutable.HashMap[ExtractionRecorder[_], ExtractionLogger]()
  def getLogger[T](clazz: Class[T], lang: Language, datasets: Seq[Dataset] = Seq()): ExtractionLogger ={
    val er = getExtractionRecorder[T](lang, datasets)(ClassTag.apply(clazz))
    if(!er.isInitialized)
      er.initialize(lang, getTaskName(clazz), datasets)
    loggerMap.get(er) match{
      case Some(l) => l
      case None =>
        val orig = Logger.getLogger(clazz)
        val logger = new ExtractionLogger(orig, er)
        setHierarchy(logger, orig.getLoggerRepository)
        logger.addAppender(er)
        logger.setLevel(RECORD)
        logger
    }
  }

  private val ExtractorNodePattern = ("^" + DBpediaNamespace.PROVDOMAIN + "(extractor|node|parser).*").r
  private def getTaskName[T](clazz: Class[T]): String ={
    Try{SoftwareAgentAnnotation.getAnnotationIri(clazz)} match{
      case Success(iri) => iri.toString match{
        case ExtractorNodePattern(_) => "extraction"
        case _ => "transformation"
      }
      case Failure(_) => "transformation"
    }
  }

  private def setHierarchy(logger: ExtractionLogger, repo: LoggerRepository): Unit ={
    val method = classOf[Category].getDeclaredMethod("setHierarchy", classOf[LoggerRepository])
    method.setAccessible(true)
    method.invoke(logger, repo)
  }

  private def getNewLevelInstance(int: Int, name: String): Level ={
    val const = classOf[Level].getDeclaredConstructor(classOf[Int], classOf[String], classOf[Int])
    if(const == null)
      throw new IllegalStateException("Level constructor was not found")
    const.setAccessible(true)
    const.newInstance(new Integer(int), name, new Integer(7))
  }
}