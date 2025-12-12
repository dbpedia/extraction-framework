package org.dbpedia.extraction.util

import java.util.logging.{Level, Logger}
import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent.TrieMap

/**
 * Centralized monitoring and logging system for extraction quality and errors.
 *
 * Features:
 * - Structured logging with context (extractor, page, error type)
 * - Metrics collection for error rates
 * - Thread-safe error counting
 * - Export capabilities for failed extractions
 *
 * Usage:
 * {{{
 *   val monitor = DataQualityMonitor.forExtractor("HomepageExtractor")
 *   monitor.logInvalidData("Einstein", "Invalid IRI: malformed URL", exception)
 *   monitor.getMetrics() // Get error statistics
 * }}}
 */
object DataQualityMonitor {

  private val logger = Logger.getLogger(classOf[DataQualityMonitor].getName)

  // Global metrics storage (thread-safe)
  private val errorCounts = new TrieMap[String, AtomicLong]()
  private val errorDetails = new TrieMap[String, collection.mutable.ListBuffer[ExtractionError]]()

  /**
   * Create a monitor for a specific extractor
   */
  def forExtractor(extractorName: String): DataQualityMonitor = {
    new DataQualityMonitor(extractorName)
  }

  /**
   * Get global extraction statistics
   */
  def getGlobalMetrics(): Map[String, Long] = {
    errorCounts.map { case (key, counter) => (key, counter.get()) }.toMap
  }

  /**
   * Get detailed errors for analysis
   */
  def getErrorDetails(errorType: String, limit: Int = 100): List[ExtractionError] = {
    errorDetails.get(errorType) match {
      case Some(errors) => errors.take(limit).toList
      case None => List.empty
    }
  }

  /**
   * Export errors to CSV format for analysis
   */
  def exportToCsv(errorType: String, limit: Int = 1000): String = {
    val errors = getErrorDetails(errorType, limit)
    val header = "Extractor,PageTitle,ErrorMessage,Timestamp\n"
    val rows = errors.map(e =>
      s"${e.extractorName},${e.pageTitle},${e.message.replaceAll(",", ";")},${e.timestamp}"
    ).mkString("\n")
    header + rows
  }

  /**
   * Reset all metrics (useful for testing)
   */
  def reset(): Unit = {
    errorCounts.clear()
    errorDetails.clear()
  }
}

/**
 * Monitor instance for a specific extractor
 */
class DataQualityMonitor(val extractorName: String) {

  private val logger = Logger.getLogger(s"org.dbpedia.extraction.monitor.$extractorName")

  /**
   * Log invalid data with context
   *
   * @param pageTitle The Wikipedia page being processed
   * @param reason Description of why the data is invalid
   * @param exception Optional exception that caused the error
   * @param data Optional invalid data for debugging
   */
  def logInvalidData(
    pageTitle: String,
    reason: String,
    exception: Option[Throwable] = None,
    data: Option[String] = None
  ): Unit = {
    val message = buildMessage(pageTitle, reason, data)

    // Log with appropriate level
    exception match {
      case Some(ex) => logger.log(Level.WARNING, message, ex)
      case None => logger.warning(message)
    }

    // Record metrics
    recordError(pageTitle, reason, exception)
  }

  /**
   * Log skipped extraction with reason
   */
  def logSkipped(pageTitle: String, reason: String): Unit = {
    logger.fine(s"[$extractorName] Skipped '$pageTitle': $reason")
  }

  /**
   * Log successful extraction with statistics
   */
  def logSuccess(pageTitle: String, triplesCount: Int): Unit = {
    logger.fine(s"[$extractorName] Extracted $triplesCount triples from '$pageTitle'")
  }

  /**
   * Get metrics for this extractor
   */
  def getMetrics(): Map[String, Long] = {
    DataQualityMonitor.errorCounts
      .filter { case (key, _) => key.startsWith(s"$extractorName:") }
      .map { case (key, counter) => (key, counter.get()) }
      .toMap
  }

  /**
   * Get total error count for this extractor
   */
  def getTotalErrors(): Long = {
    getMetrics().values.sum
  }

  // Private helper methods

  private def buildMessage(pageTitle: String, reason: String, data: Option[String]): String = {
    val dataStr = data.map(d => s" | Data: ${truncate(d, 200)}").getOrElse("")
    s"[$extractorName] Invalid data in '$pageTitle': $reason$dataStr"
  }

  private def recordError(pageTitle: String, reason: String, exception: Option[Throwable]): Unit = {
    val errorType = s"$extractorName:${categorizeError(reason, exception)}"

    // Increment counter
    DataQualityMonitor.errorCounts
      .getOrElseUpdate(errorType, new AtomicLong(0))
      .incrementAndGet()

    // Store details (limit to prevent memory issues)
    val errorDetail = ExtractionError(
      extractorName = extractorName,
      pageTitle = pageTitle,
      message = reason,
      exceptionType = exception.map(_.getClass.getSimpleName),
      timestamp = System.currentTimeMillis()
    )

    DataQualityMonitor.errorDetails
      .getOrElseUpdate(errorType, collection.mutable.ListBuffer.empty)
      .synchronized {
        val buffer = DataQualityMonitor.errorDetails(errorType)
        if (buffer.size < 10000) { // Limit storage
          buffer += errorDetail
        }
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

/**
 * Case class representing an extraction error
 */
case class ExtractionError(
  extractorName: String,
  pageTitle: String,
  message: String,
  exceptionType: Option[String],
  timestamp: Long
)
