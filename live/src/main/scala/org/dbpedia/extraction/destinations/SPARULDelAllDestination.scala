package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage.JDBCUtil
import scala.collection.JavaConversions._
import scala.collection.Seq
import org.apache.log4j.Logger
import java.util.HashSet


/**
 * Deletes all existing triples for this resource from the DB
 */

class SPARULDelAllDestination(subjects: HashSet[String]) extends LiveDestination {

  protected val logger = Logger.getLogger(classOf[SPARULDelAllDestination].getName)

  def open(): Unit = ()

  def write(extractor: String, hash: String, addGraph: Seq[Quad], deleteGraph: Seq[Quad], unmodifiedGraph: Seq[Quad]) {
    for (quad <- addGraph)
      subjects.add(quad.subject);
    for (quad <- deleteGraph)
      subjects.add(quad.subject);
    for (quad <- unmodifiedGraph)
      subjects.add(quad.subject);
    val t = 0
  }

  def close {
    for (res <- subjects) {
      if (!res.contains("dbpedia.org/property") && !res.trim.isEmpty) {
        val sparul = "SPARQL DELETE FROM <" + LiveOptions.options.get("graphURI") + "> \n{?s ?p ?o} \nWHERE {?s ?p ?o. \nFILTER(?s = <" + res + ">)\n}"
        JDBCUtil.execSPARUL(sparul)
      }

    }
  }
}