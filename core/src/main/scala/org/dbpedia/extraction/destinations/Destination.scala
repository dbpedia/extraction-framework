package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.transform.Quad

/**
 * A destination for RDF quads.
 */
trait Destination extends Serializable
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
  def write(graph : Traversable[Quad]): Unit

  /**
   * Closes this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def close(): Unit
}
