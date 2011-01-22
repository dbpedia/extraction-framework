package org.dbpedia.extraction.destinations

/**
 * Represents an abstraction over a destination of RDF statements.
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
    def write(graph : Graph) : Unit

    /**
     * Closes this destination.
     */
    def close() : Unit = {}
}
