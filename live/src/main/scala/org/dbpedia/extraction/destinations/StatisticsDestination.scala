package org.dbpedia.extraction.destinations

import org.slf4j.LoggerFactory

import scala.collection.Seq

/**
 * Writes extraction results to statistics
 */
class StatisticsDestination extends LiveDestination{

  private val logger = LoggerFactory.getLogger(classOf[StatisticsDestination].getName)

  private var addedTriples = 0
  private var unmodifiedTriples = 0
  private var now = System.currentTimeMillis

  /**
   * Opens this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def open() {}

  override def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {
    addedTriples += graphAdd.length
    unmodifiedTriples += graphUnmodified.length
  }

  override def close = {
    val total = addedTriples + unmodifiedTriples
    logger.info("produced " + total  + " in " + (System.currentTimeMillis - now) + "ms.")
  }
}
