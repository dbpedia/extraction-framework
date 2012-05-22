package org.dbpedia.extraction.destinations

/**
 * A destination for RDF quads.
 *
 * Implementing classes of this trait must override the write method.
 * Optionally, the close method can be overriden in order to finalize the destination.
 * Each implementing class must be thread-safe.
 */
trait Destination
{
  /**
   * Writes a new statement to this destination.
   */
  def write(graph : Seq[Quad]): Unit

  /**
   * Closes this destination.
   */
  def close(): Unit
}
