package org.dbpedia.extraction.destinations

import scala.collection.Map

/**
 * A destination which groups quads by dataset and writes them to different destinations.
 * 
 * This class does not use synchronization, but if the target datasets are thread-safe then
 * so is this destination. The write() method may be executed concurrently by multiple threads. 
 * 
 * @param destinations All quads for a dataset are written to the destination given in this map. 
 * If the map contains no destination for a dataset, all quads for that dataset will be ignored.
 */
class DatasetDestination(destinations: Map[Dataset, Destination]) 
extends Destination
{
  override def open() = destinations.values.foreach(_.open())

  override def write(graph : Seq[Quad]) : Unit = {
    for((dataset, quads) <- graph.groupBy(_.dataset)) {
      destinations.get(dataset).foreach(_.write(quads))
    }
  }

  override def close() = destinations.values.foreach(_.close())

}
