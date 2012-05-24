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
  /**
   * This is basically an extension of .withDefault(factory): don't just return the default value,
   * also remember it. It allows us to avoid calling getOrElseUpdate() below, which is a bit 
   * unwieldy and creates a new function object for every call (just to wrap the dataset argument).
   * BUT: This code is unwieldy too, and I don't know enough about the Scala collections
   * to actually use it. I'll just save it here for posteriority.
   */
  private val destinations = new HashMap[Dataset, Destination]() {
    override def default(dataset: Dataset): Destination = {
      val destination = factory(dataset)
      addEntry(new Entry(dataset, destination))
      destination
    }
  }

  private var closed = false

  override def write(graph : Seq[Quad]) : Unit = synchronized {
    if (closed) throw new IllegalStateException("Trying to write to a closed destination")
    for((dataset, quads) <- graph.groupBy(_.dataset)) {
      val destination = destinations(dataset)
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
