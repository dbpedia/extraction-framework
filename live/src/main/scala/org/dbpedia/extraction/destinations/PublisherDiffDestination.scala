package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import collection.mutable
import org.dbpedia.extraction.live.main.Main
import org.dbpedia.extraction.live.publisher.DiffData

/**
 * This class publishes the triples to files (added / deleted)
 */
class PublisherDiffDestination(pageID: Long) extends LiveDestination {

  var added = new java.util.HashSet[Quad]() // Set to remove duplicates
  var deleted = new java.util.HashSet[Quad]() // Set to remove duplicates


  def open() { }

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {
    for (quad <- graphAdd)
      added.add(quad)

    for (quad <- graphRemove)
      deleted.add(quad)
  }

  def close() {
    Main.publishingDataQueue.add(new DiffData(pageID, added,deleted))
  }


}
