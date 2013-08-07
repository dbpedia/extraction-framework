package org.dbpedia.extraction.destinations

/**
 * Passes quads through to the target destination until a maximum number of quads is reached. 
 * After that, additional quads are ignored.
 * 
 * TODO: Maybe this should be a mixin trait?
 */
class LimitingDestination(destination: Destination, limit: Int)
extends Destination
{
    private var count = 0
    
    override def open() = destination.open()
    
    override def write(graph : Traversable[Quad]) = if (count < limit) {
      val quads = if (count + graph.size <= limit) graph else graph.take(limit - count)
      destination.write(quads)
      count += quads.size
    }

    override def close() = destination.close()
}
