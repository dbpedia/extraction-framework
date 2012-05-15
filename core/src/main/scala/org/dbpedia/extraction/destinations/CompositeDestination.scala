package org.dbpedia.extraction.destinations

import scala.collection.immutable.Traversable

/**
 * A destination that is composed of different child destinations.
 * Each statement is forwarded to all child destinations.
 */
class CompositeDestination(destinations : Destination*) extends Destination
{
    /**
     * Writes a new statement to all child destinations.
     */
    override def write(graph : Seq[Quad]) = destinations.foreach(_.write(graph))

    /**
     * Closes all child destinations.
     */
    override def close() = destinations.foreach(_.close())
}
