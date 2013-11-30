package org.dbpedia.extraction.destinations

import org.apache.log4j.Logger
import formatters.UriPolicy._
import org.dbpedia.extraction.destinations.formatters.SPARULFormatter
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage.JDBCUtil
import scala.collection.Seq
import org.dbpedia.extraction.live.main.Main

/**
 * Writes modified triples to triple store. Can do one operation (insert / delete) at a time so initialize accordingly
 */

class SPARULDestination(insOrDel: Boolean, policies: Array[Policy] = null) extends LiveDestination {

  protected val logger = Logger.getLogger(classOf[SPARULDestination].getName)

  protected val formatter = new SPARULFormatter(insOrDel, LiveOptions.options.get("graphURI"), policies)
  protected var sparql = new StringBuffer
  protected var tripleSize = 0

  def open {
    sparql.append(formatter.header)
  }

  def write(extractor: String, hash: String, addGraph: Seq[Quad], deleteGraph: Seq[Quad], unmodifiedGraph: Seq[Quad]) {
    if (insOrDel == true) {
      tripleSize += addGraph.length
      for (quad <- addGraph) {
        sparql.append(formatter.render(quad))
      }
    }
    else {
      tripleSize += deleteGraph.length
      for (quad <- deleteGraph) {
        sparql.append(formatter.render(quad))
      }
    }
  }

  def close {
    sparql.append(formatter.footer)
    if (tripleSize == 0) return
    val success = JDBCUtil.execSPARUL("SPARQL " + sparql.toString)
    // TODO Better logging
    if (!success) {
      logger.error("SPARUL Update for page failed")
      Main.stopLive
      System.exit(1)
    }

  }
}