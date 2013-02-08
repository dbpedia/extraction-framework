package org.dbpedia.extraction.destinations

/**
 * A destination for Live extraction.
 * The write function separates add, remove & unmodified quads
 */
abstract class LiveDestination extends Destination {

  /**
   * Writes quads to all child destinations.
   */
  override def write(graph: Seq[Quad]): Unit = {
    throw new Exception("Function: \"write(graph : Seq[Quad])\" is not supported in LiveDestination")
  }

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

}
