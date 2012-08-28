package org.dbpedia.extraction.destinations

/**
 * A destination for RDF quads.
 */
trait Destination
{
  /**
   * Opens this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def open(): Unit
  
  /**
   * Writes quads to this destination. Implementing classes should make sure that this method
   * can safely be executed concurrently by multiple threads.
   */
  def write(graph : Seq[Quad]): Unit

  /**
   * Closes this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def close(): Unit
}
