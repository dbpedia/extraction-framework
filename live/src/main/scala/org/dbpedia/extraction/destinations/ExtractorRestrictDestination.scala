package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.live.helper.{ExtractorStatus, ExtractorSpecification}
import scala.collection.Seq
import java.util.Map
import collection.mutable.ArrayBuffer

/**
 * Applies the extractor filters from the configuration file
 * TODO: Convert this class into something like a filter, no need for open close
 */
class ExtractorRestrictDestination(extractorSpecs: Map[String, ExtractorSpecification], pipe: LiveDestination) extends LiveDestination {

  override def open = pipe.open()

  /**
   * Writes quads to all child destinations.
   */
  def write(extractor: String, hash: String, graphAdd: Seq[Quad], graphRemove: Seq[Quad], graphUnmodified: Seq[Quad]) {

    var added = new ArrayBuffer[Quad]()
    var deleted = new ArrayBuffer[Quad]()
    var unmodified = new ArrayBuffer[Quad]()

    val spec: ExtractorSpecification = extractorSpecs.get(extractor)

    for (quad <- graphAdd) {
      if (spec != null && !spec.accept(quad)) deleted += quad.copy()
      else if (spec.status == ExtractorStatus.KEEP) unmodified += quad.copy()
      else added += quad.copy()
    }

    for (q <- graphRemove) {

    }

    pipe.write(extractor, hash, added, deleted ++ graphRemove, unmodified ++ graphUnmodified)
  }

  override def close = pipe.close()

}