package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.destinations.formatters.RDFJSONFormatter
import scala.collection.Seq
import util.Sorting
import org.dbpedia.extraction.live.storage.JSONCache
import org.dbpedia.extraction.util.StringUtils

/**
 * Checks with the JSONCache to find the diffs per extractor, it feeds the output to the @pipe LiveDestination
 * TODO: Convert this class into something like a filter, no need for open close
 */
class JSONCacheExtractorDestination(cache: JSONCache, pipe: LiveDestination) extends LiveDestination {

  val formatter: RDFJSONFormatter = new RDFJSONFormatter

  override def open = pipe.open()

  override def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {

    val sb = new java.lang.StringBuilder
    val quads = Sorting.stableSort(graphAdd ++ graphUnmodified)

    for (quad <- quads) {
      sb append formatter.render(quad)
    }

    if (quads.length > 0) {
      sb.setCharAt(sb.lastIndexOf(","), ' ') // remove last ','
    }
    // JSON ready, cache for later
    cache.setExtractorJSON(extractor, sb.toString)
    val newHash = StringUtils.md5sum(sb.toString) // calculate new md5sum
    val cachedHash = cache.getHashForExtractor(extractor) //get the cached md5sum
    if (!cachedHash.equals("") && cachedHash.equals(newHash)) {
      // everything is the same, pipe to unmodified
      pipe.write(extractor, cachedHash, Seq(), graphRemove.toSet.toSeq, quads.toSet.toSeq)
      return
    }

    val existing = Sorting.stableSort(cache.getTriplesForExtractor(extractor))

    val toAdd = quads.filterNot(q => existing.contains(q)).toSet.toSeq
    val toDelete = existing.filterNot(q => quads.contains(q)).toSet.toSeq
    val unmodified = quads.filter(q => existing.contains(q)).toSet.toSeq

    pipe.write(extractor, newHash, toAdd, toDelete, unmodified)
  }

  override def close = pipe.close()

}