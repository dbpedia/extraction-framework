package org.dbpedia.extraction.dump.extract

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Keeps track of the extraction progress.
 */
class ExtractionProgress()
{
  /**
   * The time when the page extraction was started. Milliseconds since midnight, January 1, 1970 UTC.
   */
  val startTime = new AtomicLong()
  
  /**
   * The number of pages which have been extracted successfully
   */
  val extractedPages = new AtomicInteger()
  
  /**
   * The number of pages for which the extraction failed
   */
  val failedPages = new AtomicInteger()
  
//    why should we override this? Just to make it synchronized?
//    override def clone = synchronized
//    {
//        new ExtractionProgress(startTime, extractedPages, failedPages)
//    }
}
