package org.dbpedia.extraction.destinations

/**
 * Holds a set of statements.
 * 
 * TODO: make this mutable - allow adding quads to an existing graph. 
 * There are hundreds of places where we need this. If concurrency
 * is a problem for dbpedia live, use concurrent collections or
 * synchronization, but it probably isn't - usually, a graph is 
 * extracted by one extractor and then sent to a destination.
 * 
 * TODO: remove this class. It offers no advantage over List[Quad].
 */
class Graph(val quads : List[Quad] = Nil)
{
    /**
     * Constructs a graph from a single statements.
     */
    def this(quad : Quad) = this(List(quad))

    /**
     * Tests if this graph is empty.
     */
    def isEmpty = quads.isEmpty

    /**
     * Merges this graph with another graph and returns the result.
     * TODO: remove this method. Use flatMap instead.
     */
    def merge(graph : Graph) : Graph = new Graph(quads ::: graph.quads)
}
