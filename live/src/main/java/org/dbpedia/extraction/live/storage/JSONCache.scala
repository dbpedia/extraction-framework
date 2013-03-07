package org.dbpedia.extraction.live.storage

import org.dbpedia.extraction.destinations.{SPARULDestination, Quad}
import java.util.logging.{Level, Logger}
import scala.util.parsing.json._
import collection.mutable.{ListBuffer, ArrayBuffer, HashMap}
import collection.mutable
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.destinations.formatters.SPARULFormatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._

/**
 * This class retrieves the stored cache for a resource extraction.
 * The cache is per extractor together with a hash string for faster compare
 */

class JSONCache(pageID: Long, pageTitle: String) {
  private val logger = Logger.getLogger(classOf[JSONCache].getName)

  var extractorHash = new HashMap[String, String]
  var extractorTriples = new HashMap[String, List[Any]]
  var extractorJSON = new HashMap[String, String]

  var cacheObj : JSONCacheObject = null
  var cacheExists = false

  initCache

  def getHashForExtractor(extractor: String): String = {
    extractorHash.getOrElse(extractor, "")
  }

  def getTriplesForExtractor(extractor: String): Seq[Quad] = {
    val list: List[Any] = extractorTriples.getOrElse(extractor, List())
    if (list.isEmpty)
      return Seq()


    var quads = new ArrayBuffer[Quad](list.length)

    list.foreach ( i => {
      val sm = i.asInstanceOf[Map[String, Any]]
      sm.foreach{
        case(ks,vs) =>
        val subject = ks.toString
        vs.asInstanceOf[Map[String, Any]].foreach{
          case(kp,vp) =>
          val predicate = kp.toString
          val objLsit = vp.asInstanceOf[List[Map[String,String]]]
          for (obj <- objLsit) {
                val objValue: String = obj.getOrElse("value","")
                val objType: String = obj.getOrElse("type","")
                val objLang: String = obj.getOrElse("lang", LiveOptions.options.get("language"))
                val objDatatype: String = obj.getOrElse("datatype", "http://www.w3.org/2001/XMLSchema#string")

                val finalDatatype = if (objType.equals("uri")) null else objDatatype // null datatype if uri
                quads += new Quad(objLang,"",subject, predicate, objValue, "", finalDatatype)

          }
        }
      }
    })

    quads
  }

  def getAllHashedTriples(): Seq[Quad] = {
    var quads = ListBuffer[Quad]()
    for(key: String <- extractorTriples.keySet.seq) {
      quads ++= getTriplesForExtractor(key)
    }
    quads.distinct
  }

  def setExtractorJSON(extractor: String, json: String) {
    extractorJSON += (extractor -> json)
  }

  def getExtractorJSON(extractor: String): String = {
    extractorJSON.getOrElse(extractor, "")
  }

  def updateCache(json: String, subjects: String, diff: String): Boolean = {
    val updatedTimes = if ( cacheObj == null) "0" else (cacheObj.updatedTimes + 1).toString
    // Check wheather to update or insert
    if (cacheExists) {
      return JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheUpdate, Array[String](this.pageTitle, updatedTimes,  json, subjects, diff,  "" + this.pageID))
    }
    else
      return JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheInsert, Array[String]("" + this.pageID, this.pageTitle, updatedTimes,  json, subjects, diff))
  }

  private def initCache {
    try {
      cacheObj = JDBCUtil.getCacheContent(DBpediaSQLQueries.getJSONCacheSelect, this.pageID)
      if (cacheObj == null) return
      cacheExists = true
      if (cacheObj.json.equals("")) return

      val json: Option[Any] = JSON.parseFull(cacheObj.json)
      val map: Map[String, Any] = json.get.asInstanceOf[Map[String, Any]]

      map.foreach {
        case (key, value) => {
          val extractor: String = key.toString
          val contents = value.asInstanceOf[Map[String, Any]]
          val hash: String = contents.getOrElse("hash", "").asInstanceOf[String]
          // Do not convert quads now, maybe they exist in cache
          val triples: List[Any] = contents.getOrElse("triples", List()).asInstanceOf[List[Any]]
          extractorHash += (extractor -> hash)
          extractorTriples += (extractor -> triples)
        }
      }
    }
    catch {
      case e: Exception => {
        logger.log(Level.INFO, e.getMessage)
      }
    }
  }


}

object JSONCache {
  def setErrorOnCache(pageID: Long, error: Int) {
    JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheUpdateError, Array[String]("" + error, "" + pageID))
  }

  def deleteCacheItem(pageID: Long, policies: Array[Policy] = null) {
    val cache = new JSONCache(pageID, "")
    val triples = cache.getAllHashedTriples()

    val dest = new SPARULDestination(false, policies)

    dest.open
    dest.write("dummy extractor","dummy hash", Seq(), triples, Seq())
    dest.close

    deleteCacheOnlyItem(pageID)
  }

  def deleteCacheOnlyItem(pageID: Long) {
    JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheDelete, Array[String]("" + pageID))
  }
}

class JSONCacheObject(val pageID: Long, val updatedTimes: Int, val json: String, val subjects: String) {

}