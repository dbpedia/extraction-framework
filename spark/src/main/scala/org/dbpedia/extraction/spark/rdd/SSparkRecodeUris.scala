package org.dbpedia.extraction.spark.rdd

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.UriUtils

/**
  * Created by Chile on 2/7/2017.
  */
class SSparkRecodeUris extends Transformer[Quad, Quad]{
  override def transform(in: Quad): Quad = {
      val quad = in
      try {
        var changed = false
        val subj = fixUri(quad.subject)
        changed = changed || subj != quad.subject
        val pred = fixUri(quad.predicate)
        changed = changed || pred != quad.predicate
        val obj = if (quad.datatype == null) fixUri(quad.value) else quad.value
        changed = changed || obj != quad.value
        val cont = if (quad.context != null) fixUri(quad.context) else quad.context
        changed = changed || cont != quad.context
        quad.copy(subject = subj, predicate = pred, value = obj, context = cont)
      }
  }

  def fixUri(uri: String): String =
    UriUtils.uriToIri(uri)
}
