package org.dbpedia.extraction.util

import scala.collection.mutable.{ArrayBuffer, HashMap, HashSet, Map, Seq, Set}

/**
 * Resolves transitive relations in a graph and removes cycles.
 */
class TransitiveClosure[T](val graph: Map[T, T]) {
  
  /**
   * stores all found cycles.
   */
  private val cycleList = new ArrayBuffer[Set[T]]()
  
  /**
   * map from node to the cycle this node is part of
   */
  private val cycleMap = new HashMap[T, Set[T]]()
  
  /**
   * Resolve transitive relations in graph, remove cycles, return list of cycles.
   */
  def resolve(): Seq[Set[T]] = {
    for (node <- graph.keys) resolve(node)
    cycleList
  }
  
  /**
   * Resolve relations of current node and all its descendants. To speed things up and to avoid
   * duplicate work, we resolve all transitive relations, not just the one for the current node,
   * and we remove cyclic nodes from the graph as soon as possible and remember their cycle in
   * the cycle map. This means that this method may be called for a node that no longer exists
   * in the map. It also means that if the map contains no mapping for a node, we either found
   * a target node, or we found a node that was in a cycle.
   */
  private def resolve(node: T): Unit = {
    var current = node
    val seen = new HashSet[T]()
    while (true) {
      graph.get(current) match {
        case None => {
          cycleMap.get(current) match {
            // we found an old cycle. mark all seen nodes as cyclic.
            case Some(cycle) => addCycle(seen, cycle)
            // we found a target. resolve all seen nodes.
            case None => for (key <- seen) graph(key) = current
          }
          return
        }
        case Some(next) => {
          if (! seen.add(current)) {
            // we found a new cycle. mark all seen nodes as cyclic.
            addCycle(seen)
            return
          }
          current = next
        }
      }
    }
  }
  
  /**
   * Remember that the seen nodes are part of a cycle, either an old or a new cycle.
   */
  private def addCycle(seen: Set[T], old: Set[T] = null): Unit = {
    var cycle = old 
    if (cycle == null) {
      cycle = new HashSet[T]()
      cycleList += cycle
    }
    cycle ++= seen
    for (node <- seen) {
      cycleMap(node) = cycle
      graph.remove(node)
    }
  }
  
}