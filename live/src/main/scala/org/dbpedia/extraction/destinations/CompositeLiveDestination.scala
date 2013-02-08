package org.dbpedia.extraction.destinations

/**
 * A destination that is composed of different child destinations.
 * Each statement is forwarded to all child destinations.
 *
 * This class does not use synchronization, but if the target datasets are thread-safe then
 * so is this destination. The write() method may be executed concurrently by multiple threads. 
 */
class CompositeLiveDestination(destinations: LiveDestination*) extends LiveDestination {

  override def open() = destinations.foreach(_.open())

  override def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) =
    destinations.foreach(_.write(extractor, hash, graphAdd, graphRemove, graphUnmodified))

  override def close() = destinations.foreach(_.close())

}