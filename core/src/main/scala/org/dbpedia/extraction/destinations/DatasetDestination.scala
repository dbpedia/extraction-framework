package org.dbpedia.extraction.destinations

import org.apache.commons.lang3.concurrent.ConcurrentException

import scala.collection.{mutable, Map}

/**
 * A destination which groups quads by dataset and writes them to different destinations.
 * 
 * This class does not use synchronization, but if the target datasets are thread-safe then
 * so is this destination. The write() method may be executed concurrently by multiple threads. 
 * 
 * @param destinations All quads for a dataset are written to the destination given in this map. 
 * If the map contains no destination for a dataset, all quads for that dataset will be ignored.
 */
class DatasetDestination(destinations: Map[String, Destination]) 
extends Destination
{
  override def open() = {
    for(dest <- destinations)
    {
      DatasetDestination.openedDatasets.get(dest._1) match{
        case Some(x) => throw new ConcurrentException("The following Dataset is already in progress: " + dest._1, null)
        case None => {
          dest._2.open()
          DatasetDestination.openedDatasets(dest._1) = true
        }
      }
    }
  }

  override def write(graph : Traversable[Quad]) : Unit = {
    for((dataset, quads) <- graph.groupBy(_.dataset)) {
      destinations.get(dataset).foreach(_.write(quads))
    }
  }

  override def close() = {
    for(dest <- destinations)
    {
      dest._2.close()
      DatasetDestination.openedDatasets.remove(dest._1)
    }
  }

}

object DatasetDestination{
  //maybe use the map to add some provenance ?
  private val openedDatasets = mutable.HashMap[String, Boolean]()
}