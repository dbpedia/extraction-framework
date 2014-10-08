package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.destinations.formatters.RDFJSONFormatter
import org.slf4j.LoggerFactory
import scala.collection.Seq
import util.Sorting
import collection.mutable.ArrayBuffer
import org.dbpedia.extraction.live.storage.JSONCache
import java.util.HashSet


/*
* This destination stores a json file with all the extractor output to compare for next extraction
* */
class JSONCacheUpdateDestination(cache: JSONCache) extends LiveDestination {
  private val logger = LoggerFactory.getLogger(classOf[JSONCacheUpdateDestination].getName)


  var extractors = new ArrayBuffer[String](20)
  var hashes = new ArrayBuffer[String](20)
  var isModified = false
  val formatter: RDFJSONFormatter = new RDFJSONFormatter
  val subjects = new HashSet[String]

  def open {
  }

  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {

    hashes += hash
    extractors += extractor
    for (quad <- graphAdd) subjects.add(quad.subject);
    for (quad <- graphRemove) subjects.add(quad.subject);
    for (quad <- graphUnmodified) subjects.add(quad.subject);

    if (graphAdd.size > 0 || graphRemove.size > 0) {
      isModified = true
    }
  }

  def close {
    val sb = new java.lang.StringBuilder
    sb append "{"
    for (i <- 0 until extractors.size) {
      sb append "\"" append extractors(i) append "\" : {\n"
      sb append " \"hash\": \"" append hashes(i) append "\",\n"
      sb append " \"triples\": [" append cache.getExtractorJSON(extractors(i)) append "]"
      sb append "},"
    }
    sb append "}"

    if (sb.length > 2)
      sb.setCharAt(sb.lastIndexOf(","), ' ')


    val success = cache.updateCache(sb.toString, subjects, "", isModified) //TODO add subjects / diffs
    // TODO better logging
    if (!success) logger.info( "Updating JSON Cache failed")

  }


}