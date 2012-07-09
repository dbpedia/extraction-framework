package org.dbpedia.extraction.dump.extract

import java.util.logging.{Level, Logger}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.dbpedia.extraction.util.StringUtils

/**
 * Keeps track of the extraction progress.
 */
class ExtractionProgress(label: String, description: String)
{
  private val logger = Logger.getLogger(getClass.getName)

  /**
   * The time when the page extraction was started. Milliseconds since midnight, January 1, 1970 UTC.
   */
  private val startTime = new AtomicLong()
  
  /**
   * The number of pages which have been extracted successfully
   */
  private val allPages = new AtomicInteger()
  
  /**
   * The number of pages for which the extraction failed
   */
  private val failedPages = new AtomicInteger()
  
  def start() {
    startTime.set(System.currentTimeMillis)
    logger.info("started: "+description)
  }
  
  def countPage(success: Boolean) {
    if (! success) failedPages.incrementAndGet
    if (allPages.incrementAndGet % 2000 == 0) log()
  }
  
  def end() {
    log()
    logger.info("finished: "+description)
  }
  
  def log() {
    val time = (System.currentTimeMillis - startTime.get)
    println(label+": extracted "+allPages.get+" pages in "+StringUtils.prettyMillis(time)+" (per page: " + (time.toDouble / allPages.get) + " ms; failed pages: "+failedPages.get+").")
  }
  
}
