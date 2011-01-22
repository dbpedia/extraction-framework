package org.dbpedia.extraction.destinations

/**
 * Holds a set of statements, organized in different datasets
 */
class Graph(val quads : List[Quad] = Nil)
{
    /**
     * Constructs a graph from a single statements.
     */
    def this(quad : Quad) = this(List(quad))

    /**
     * Retrieves all statements, grouped by their dataset.
     */
    lazy val quadsByDataset : scala.collection.Map[Dataset, List[Quad]] = quads.groupBy(quad => quad.dataset)

    /**
     * Tests if this graph is empty.
     */
    def isEmpty = quads.isEmpty

    /**
     * Merges this graph with another graph and returns the result.
     */
    def merge(graph : Graph) : Graph = new Graph(quads ::: graph.quads)
}
