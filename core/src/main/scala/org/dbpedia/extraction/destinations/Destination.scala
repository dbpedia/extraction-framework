package org.dbpedia.extraction.destinations

/**
 * A destination for RDF quads.
 *
 * Implementing classes should be thread-safe.
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
