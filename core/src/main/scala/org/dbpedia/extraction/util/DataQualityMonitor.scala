package org.dbpedia.extraction.util

import java.util.logging.{Level, Logger}
import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap

/**
 * Monitors data quality issues during extraction.
 * Tracks errors per extractor and provides export capabilities.
 */
object DataQualityMonitor {

  private val logger = Logger.getLogger(classOf[DataQualityMonitor].getName)
  private val errorCounts = new TrieMap[String, AtomicLong]()
  private val errorDetails = new TrieMap[String, collection.mutable.ListBuffer[ExtractionError]]()

  def forExtractor(extractorName: String): DataQualityMonitor = {
    new DataQualityMonitor(extractorName)
  }

  def getGlobalMetrics(): Map[String, Long] = {
    errorCounts.map { case (key, counter) => (key, counter.get()) }.toMap
  }

  def getErrorDetails(errorType: String, limit: Int = 100): List[ExtractionError] = {
    errorDetails.get(errorType) match {
      case Some(errors) => errors.take(limit).toList
      case None => List.empty
    }
  }

  def exportToCsv(errorType: String, limit: Int = 1000): String = {
    val errors = getErrorDetails(errorType, limit)
    val header = "Extractor,PageTitle,ErrorMessage,Timestamp\n"
    val rows = errors.map(e =>
      s"${e.extractorName},${e.pageTitle},${e.message.replaceAll(",", ";")},${e.timestamp}"
    ).mkString("\n")
    header + rows
  }

  def reset(): Unit = {
    errorCounts.clear()
    errorDetails.clear()
  }
}

class DataQualityMonitor(val extractorName: String) {

  private val logger = Logger.getLogger(s"org.dbpedia.extraction.monitor.$extractorName")

  def logInvalidData(
    pageTitle: String,
    reason: String,
    exception: Option[Throwable] = None,
    data: Option[String] = None
  ): Unit = {
    val message = buildMessage(pageTitle, reason, data)
    exception match {
      case Some(ex) => logger.log(Level.WARNING, message, ex)
      case None => logger.warning(message)
    }
    recordError(pageTitle, reason, exception)
  }

  def logSkipped(pageTitle: String, reason: String): Unit = {
    logger.fine(s"[$extractorName] Skipped '$pageTitle': $reason")
  }

  def logSuccess(pageTitle: String, triplesCount: Int): Unit = {
    logger.fine(s"[$extractorName] Extracted $triplesCount triples from '$pageTitle'")
  }

  def getMetrics(): Map[String, Long] = {
    DataQualityMonitor.errorCounts
      .filter { case (key, _) => key.startsWith(s"$extractorName:") }
      .map { case (key, counter) => (key, counter.get()) }
      .toMap
  }

  def getTotalErrors(): Long = getMetrics().values.sum

  private def buildMessage(pageTitle: String, reason: String, data: Option[String]): String = {
    val dataStr = data.map(d => s" | Data: ${truncate(d, 200)}").getOrElse("")
    s"[$extractorName] Invalid data in '$pageTitle': $reason$dataStr"
  }

  private def recordError(pageTitle: String, reason: String, exception: Option[Throwable]): Unit = {
    val errorType = s"$extractorName:${categorizeError(reason, exception)}"

    DataQualityMonitor.errorCounts
      .getOrElseUpdate(errorType, new AtomicLong(0))
      .incrementAndGet()

    val errorDetail = ExtractionError(
      extractorName = extractorName,
      pageTitle = pageTitle,
      message = reason,
      exceptionType = exception.map(_.getClass.getSimpleName),
      timestamp = System.currentTimeMillis()
    )

    DataQualityMonitor.errorDetails.synchronized {
      val buffer = DataQualityMonitor.errorDetails
        .getOrElseUpdate(errorType, collection.mutable.ListBuffer.empty)
      if (buffer.size < 10000) buffer += errorDetail
    }
  }

  private def categorizeError(reason: String, exception: Option[Throwable]): String = {
    exception match {
      case Some(ex) => ex.getClass.getSimpleName
      case None if reason.toLowerCase.contains("invalid") => "InvalidData"
      case None if reason.toLowerCase.contains("malformed") => "MalformedData"
      case None if reason.toLowerCase.contains("missing") => "MissingData"
      case None => "Other"
    }
  }

  private def truncate(str: String, maxLength: Int): String = {
    if (str.length <= maxLength) str
    else str.substring(0, maxLength) + "..."
  }
}

case class ExtractionError(
  extractorName: String,
  pageTitle: String,
  message: String,
  exceptionType: Option[String],
  timestamp: Long
)
