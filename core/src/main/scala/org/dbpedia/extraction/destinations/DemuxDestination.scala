package org.dbpedia.extraction.destinations

import collection.mutable.HashMap

/**
 * A destination which groups quads by dataset and writes them to different destinations -
 * in other words, it de-multiplexes the quad stream (hence the name). 
 * 
 * This class is thread-safe. It's actually a bit too thread-safe: it would be nice if multiple
 * threads could execute write() concurrently, because they may well be writing to different
 * target destinations and won't get into each others way. Only the call getOrElseUpdate() 
 * needs to be synchronized really. But: the possibility that yet another thread may call
 * close() makes things much harder. In practice, close() will probably be called by a master
 * thread after all slave threads finished calling write(). But in theory, the calls may happen
 * concurrently, and we must make sure that close() is called on all target destinations.
 * That means that when close() is called, we cannot let any thread execute write().
 * Read-write-locks may be the way... But anyway, it probably won't really be a problem.
 * Usually, all destinations will access the same storage and that will be the bottleneck.
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
