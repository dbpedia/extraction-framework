package org.dbpedia.extraction.destinations

import collection.mutable.HashMap

/**
 * A destination which groups quads by dataset and writes them to different destinations -
 * in other words, it de-multiplexes the quad stream (hence the name). 
 * 
 * This class is thread-safe.
 * 
 * @param factory Called when a dataset is encountered for the first time, produces a destination. 
 * All quads for the dataset are written to that destination. If it returns null for a dataset, 
 * all quads for that dataset will be ignored.
 */
class DemuxDestination(factory: Dataset => Destination) 
extends Destination
{
  private val destinations = new HashMap[Dataset, Destination]()

  private var closed = false

  override def write(graph : Seq[Quad]) : Unit = synchronized {
    if (closed) throw new IllegalStateException("Trying to write to a closed destination")
    for((dataset, quads) <- graph.groupBy(_.dataset)) {
      val destination = destinations.getOrElseUpdate(dataset, factory(dataset))
      if (destination != null) destination.write(quads)
    }
  }

  override def close() = synchronized {
    if (! closed) {
      for(destination <- destinations.values) destination.close()
      closed = true
    }
  }

}
