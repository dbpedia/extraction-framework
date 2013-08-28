package org.dbpedia.extraction.destinations

/**
 * Base class for destinations that forward most calls to another destination.
 */
abstract class WrapperDestination(destination: Destination)
extends Destination
{
    override def open() = destination.open()
    
    override def write(graph : Traversable[Quad]) = destination.write(graph)

    override def close() = destination.close()
}
