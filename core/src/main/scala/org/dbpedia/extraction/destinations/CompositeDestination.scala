package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.transform.Quad

/**
 * A destination that is composed of different child destinations.
 * Each statement is forwarded to all child destinations.
 * 
 * This class does not use synchronization, but if the target datasets are thread-safe then
 * so is this destination. The write() method may be executed concurrently by multiple threads. 
 */
class CompositeDestination(val destinations : Destination *) extends Destination
{
    /**
     * Opens all child destinations.
     */
    override def open() = destinations.foreach(
        _.open())
    
    /**
     * Writes quads to all child destinations.
     */
    override def write(graph : Traversable[Quad]) = destinations.foreach(_.write(graph))

    /**
     * Closes all child destinations.
     */
    override def close() = destinations.foreach(_.close())
}
