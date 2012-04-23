package org.dbpedia.extraction.destinations

/**
 * Holds a set of statements, organized in different datasets.
 * 
 * TODO: make this mutable - allow adding quads to an existing graph. 
 * There are hundreds of places where we need this. If concurrency
 * is a problem for dbpedia live, use concurrent collections or
 * synchronization, but it probably isn't - usually, a graph is 
 * extracted by one extractor and then sent to a destination.
 * 
 * TODO: make this a Traversable so we can use flatMap to collect all quads
 * from many extractors.
 * 
 * TODO: just remove this class. It offers no advantage over List[Quad] except quadsByDataset,
 * which can easily be inlined.
 */
class Graph(val quads : List[Quad] = Nil)
{
    /**
     * Constructs a graph from a single statements.
     */
    def this(quad : Quad) = this(List(quad))

    /**
     * Retrieves all statements, grouped by their dataset.
     * TODO: if this class is made mutable, this should return
     * a read-only view of a private map . When a quad is added 
     * to the list, it must also be added to a private map.
     */
    lazy val quadsByDataset : scala.collection.Map[Dataset, List[Quad]] = quads.groupBy(_.dataset)

    /**
     * Tests if this graph is empty.
     */
    def isEmpty = quads.isEmpty

    /**
     * Merges this graph with another graph and returns the result.
     */
    def merge(graph : Graph) : Graph = new Graph(quads ::: graph.quads)
}
