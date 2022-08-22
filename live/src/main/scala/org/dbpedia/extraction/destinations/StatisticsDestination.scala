package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.transform.Quad

/**
 * Writes extraction results to statistics
 */
class StatisticsDestination extends LiveDestination{

  def open() {}

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {}

  def close() {}
}
