package org.dbpedia.extraction.destinations

/**
 * A destination for Live extraction.
 * The write function separates add, remove & unmodified quads
 */
abstract class LiveDestination {

  /**
   * Opens this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def open(): Unit
  
  /**
   * Writes quads to all child destinations.
   */
  def write(graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]): Unit = {
    write("", "", graphAdd, graphRemove, graphUnmodified)
  }

  /**
   * Writes quads to all child destinations.
   */
  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]): Unit

  /**
   * Closes this destination. This method should only be called once during the lifetime
   * of a destination, and it should not be called concurrently with other methods of this class.
   */
  def close(): Unit

}
