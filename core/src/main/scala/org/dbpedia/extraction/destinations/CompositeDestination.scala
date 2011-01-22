package org.dbpedia.extraction.destinations

import scala.collection.immutable.Traversable

/**
 * A destination that is composed of different child destinations.
 * Each statement is forwarded to all child destinations.
 */
class CompositeDestination(destinations : Traversable[Destination]) extends Destination
{
    def this(destinations : Destination*) = this(destinations.toList)

    /**
     * Writes a new statement to all child destinations.
     */
    override def write(graph : Graph) = destinations.foreach(_.write(graph))

    /**
     * Closes all child destinations.
     */
    override def close() = destinations.foreach(_.close())
}
