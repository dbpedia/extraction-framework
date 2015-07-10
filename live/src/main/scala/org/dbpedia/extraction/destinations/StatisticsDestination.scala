package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.live.statistics.StatisticsData
import org.slf4j.LoggerFactory

import scala.collection.Seq

/**
 * Writes extraction results to statistics
 */
class StatisticsDestination(pageTitle: String) extends LiveDestination{
  private var total = 0
  private var now = System.currentTimeMillis

  /**
   * Opens this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def open() {}

  override def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {
    total += graphAdd.length
    total += graphUnmodified.length
  }

  override def close = {
    StatisticsData.addItem(pageTitle, total, now)
  }
}
