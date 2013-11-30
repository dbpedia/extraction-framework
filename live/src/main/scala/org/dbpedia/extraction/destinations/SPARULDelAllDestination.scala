package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.live.storage.JDBCUtil
import scala.collection.JavaConversions._
import scala.collection.Seq
import org.apache.log4j.Logger
import java.util.HashSet
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.destinations.formatters.SPARULFormatter

/**
 * Deletes all existing triples for this resource from the DB
 */

class SPARULDelAllDestination(subjects: HashSet[String], policies: Array[Policy]) extends LiveDestination {

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
    val formatter = new SPARULFormatter(false, "", policies)
    val language = LiveOptions.options.get("language")
    for (res <- subjects) {
      if (!res.contains("dbpedia.org/property") && !res.trim.isEmpty) {
        //hack! we only need the uri escape here otherwise we need to create a new formatter / builder
        val dummyTriple = formatter.render(new Quad(language,"",res, res, res,"", ""))
        val uri = dummyTriple.substring(dummyTriple.indexOf("<")+1, dummyTriple.indexOf("> "))
        val sparul = "SPARQL DELETE FROM <" + LiveOptions.options.get("graphURI") + "> \n{?s ?p ?o} \nWHERE {?s ?p ?o. \nFILTER(?s = <" + uri + ">)\n}"
        JDBCUtil.execSPARUL(sparul)
      }

    }
  }
}