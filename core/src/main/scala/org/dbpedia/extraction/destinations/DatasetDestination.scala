package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.transform.Quad

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
class DatasetDestination(val destinations: Map[Dataset, Destination])
extends Destination
{
  override def open() = {
    for(dest <- destinations)
    {
      dest._2.open()
    }
  }

  override def write(graph : Traversable[Quad]) : Unit = {
    for((dataset, quads) <- graph.groupBy(_.dataset)) {
      destinations.get(DBpediaDatasets.getDataset(dataset).get).foreach(_.write(quads))
    }
  }

  override def close() = {
    for(dest <- destinations)
    {
      dest._2.close()
    }
  }

}