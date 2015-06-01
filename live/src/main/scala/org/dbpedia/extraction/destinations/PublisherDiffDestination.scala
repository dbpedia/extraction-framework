package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.live.core.LiveOptions

import scala.collection.mutable
import org.dbpedia.extraction.live.main.Main
import org.dbpedia.extraction.live.publisher.DiffData

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * This class publishes the triples to files (added / deleted)
 * when cleanUpdate is true
 *    - added contains 'added+unmodified'
 *    - deleted is empty
 *    - subjects contains all the distinct subjects that are to be deleted with a graph pattern "<subject> ?p ?o"
 */
class PublisherDiffDestination(val pageID: Long, val cleanUpdate: Boolean, val subjects: java.util.Set[String]) extends LiveDestination {

  var added = new ArrayBuffer[Quad]()
  var deleted = new ArrayBuffer[Quad]()
  var unmodified = new ArrayBuffer[Quad]()


  def open() { }

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {
    for (quad <- graphAdd)
      added += quad

    for (quad <- graphRemove)
      deleted += quad

    for (quad <- graphUnmodified)
      unmodified += quad
  }

  def close() {

    val toAdd = new java.util.HashSet[Quad](added)
    val toDelete = new java.util.HashSet[Quad](deleted)
    val toReInsert = if (! cleanUpdate) new java.util.HashSet[Quad]() else new java.util.HashSet[Quad](unmodified)

    var resourceToClear = new java.util.HashSet[Quad]()
    if (cleanUpdate) {

      // We also create a list of resources to delete completely with "<...> ?p ?o"
      var subjectURIs = new mutable.HashSet[String]()
      for (quad <- added)
        subjectURIs.add(quad.subject);
      for (quad <- deleted)
        subjectURIs.add(quad.subject);
      for (quad <- unmodified)
        subjectURIs.add(quad.subject);

      for (uri: String <- subjectURIs) {
        if (!uri.contains("dbpedia.org/property") && uri.startsWith("http")) { // skip global property definitions or non-http uris
          resourceToClear.add(new Quad(LiveOptions.language, "", uri, "http://dbpedia.org/delete", " ?p ?o ", "", "http://www.w3.org/2001/XMLSchema#string"))
        }
      }
    }

    Main.publishingDataQueue.put(new DiffData(pageID, toAdd, toDelete, toReInsert, resourceToClear))
  }


}
