package org.dbpedia.extraction.live.storage

import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import org.apache.log4j.Logger
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.live.core.LiveOptions
import org.dbpedia.extraction.transform.Quad

import scala.collection.JavaConversions._
import scala.collection.mutable.{ArrayBuffer, HashMap, ListBuffer}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

/**
 * This class retrieves the stored cache for a resource extraction.
 * The cache is per extractor together with a hash string for faster compare
 */

class JSONCache(pageID: Long, pageTitle: String) {
  private val logger = Logger.getLogger(classOf[JSONCache].getName)

  var extractorHash = new HashMap[String, String]
  var extractorTriples = new HashMap[String, List[Any]]
  var extractorJSON = new HashMap[String, String]

  var cacheObj : JSONCacheItem = null
  var cacheExists = false

  initCache

  def performCleanUpdate() : Boolean = {
    return cacheObj != null && cacheObj.updatedTimes >=5
  }

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
        val subject = org.apache.commons.lang.StringEscapeUtils.unescapeJava(ks.toString)
        vs.asInstanceOf[Map[String, Any]].foreach{
          case(kp,vp) =>
          val predicate = org.apache.commons.lang.StringEscapeUtils.unescapeJava(kp.toString)
          val objLsit = vp.asInstanceOf[List[Map[String,String]]]
          for (obj <- objLsit) {
            val objType: String = obj.getOrElse("type","")
            val objLang: String = obj.getOrElse("lang", JSONCache.defaultLanguage)
            val objDatatype: String = if (objType.equals("uri"))  null
                                      else obj.getOrElse("datatype", "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")

            val objValue: String = obj.getOrElse("value","")
            // unescape if URI
            val finalValue = if (objDatatype == null) org.apache.commons.lang.StringEscapeUtils.unescapeJava(objValue) else objValue

            quads += new Quad(objLang ,"",subject, predicate, objValue, "", objDatatype)

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

  def updateCache(json: String, subjectsSet: java.util.Set[String], diff: String, isModified: Boolean): Boolean = {
    val updatedTimes = if ( cacheObj == null || performCleanUpdate()) "0" else (cacheObj.updatedTimes + 1).toString

    if ( ! isModified) {
      return JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheUpdateUnmodified, Array[String](updatedTimes, "" + this.pageID))
    }
    
    // On clean Update do not reuse existing subjects
    if (cacheObj != null && !performCleanUpdate())
      subjectsSet.addAll(cacheObj.subjects)
    
    var subjects = new StringBuilder;
    for(s <- subjectsSet) 
      subjects.append(s).append('\n');
    if (subjectsSet.size()>0)
      subjects.deleteCharAt(subjects.length-1); //delete last comma

    val escaped_json = org.apache.commons.lang.StringEscapeUtils.escapeJava(json)
    // Check wheather to update oΑr insert
    if (cacheExists) {
      return JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheUpdate, Array[String](this.pageTitle, updatedTimes,  escaped_json, subjects.toString, diff,  "" + this.pageID))
    }
    else
      return JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheInsert, Array[String]("" + this.pageID, this.pageTitle, updatedTimes,  escaped_json, subjects.toString, diff))
  }

  private def initCache {
    try {
      cacheObj = JDBCUtil.getCacheContent(DBpediaSQLQueries.getJSONCacheSelect, this.pageID)
      if (cacheObj == null) return
      cacheExists = true
      if (cacheObj.escaped_json.trim.isEmpty) return
      val unescaped_json = org.apache.commons.lang.StringEscapeUtils.unescapeJava(cacheObj.escaped_json)
      val map = JSONCache.mapper.readValue[Map[String, Any]](unescaped_json)

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
        logger.info(e.getMessage)
      }
    }
  }


}

object JSONCache {

  val defaultLanguage = LiveOptions.language
  val mapper = new ObjectMapper() with ScalaObjectMapper
  JSONCache.mapper.registerModule(DefaultScalaModule)

  def setErrorOnCache(pageID: Long, error: Int) {
    JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheUpdateError, Array[String]("" + error, "" + pageID))
  }

  def deleteCacheItem(pageID: Long, policies: Array[Policy] = null) {
    val cache = new JSONCache(pageID, "")
    val triples = cache.getAllHashedTriples()

    var destList = new ArrayBuffer[LiveDestination]()
    destList += new PublisherDiffDestination(pageID, true, if (cache.cacheObj != null) cache.cacheObj.subjects else new java.util.HashSet[String]()) //  unpublish in changesetes
    val compositeDest: LiveDestination = new CompositeLiveDestination(destList.toSeq: _*) // holds all main destinations


    compositeDest.open
    compositeDest.write("dummy extractor","dummy hash", Seq(), triples, Seq())
    compositeDest.close

    deleteCacheOnlyItem(pageID)
  }

  def deleteCacheOnlyItem(pageID: Long) {
    JDBCUtil.execPrepared(DBpediaSQLQueries.getJSONCacheDelete, Array[String]("" + pageID))
  }

  def getTriplesFromJson(jsonString: String) : Traversable[Quad] = {
    val map = mapper.readValue[Map[String, Any]](jsonString)

    val quads = new ArrayBuffer[Quad]()

    // Duplicated code JSONCache.init()
    map.foreach {
      case (key, value) => {
        val extractor: String = key.toString
        val contents = value.asInstanceOf[Map[String, Any]]
        val hash: String = contents.getOrElse("hash", "").asInstanceOf[String]
        // Do not convert quads now, maybe they exist in cache
        val triples: List[Any] = contents.getOrElse("triples", List()).asInstanceOf[List[Any]]

        // Duplicated code  JSONCache.getTriplesForExtractor
        if (triples != null && triples.nonEmpty) {


          triples.foreach(i => {
            val sm = i.asInstanceOf[Map[String, Any]]
            sm.foreach {
              case (ks, vs) =>
                val subject = ks.toString
                vs.asInstanceOf[Map[String, Any]].foreach {
                  case (kp, vp) =>
                    val predicate = kp.toString
                    val objLsit = vp.asInstanceOf[List[Map[String, String]]]
                    for (obj <- objLsit) {
                      val objValue: String = obj.getOrElse("value", "")
                      val objType: String = obj.getOrElse("type", "")
                      val objLang: String = obj.getOrElse("lang", defaultLanguage)
                      val objDatatype: String = if (objType.equals("uri"))  null
                                                else obj.getOrElse("datatype", "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")


                      quads += new Quad(objLang , "", subject, predicate, objValue, "", objDatatype)

                    }
                }
            }
          })
        }
      }
    }
    quads
  }
}

class JSONCacheItem(val pageID: Long, val updatedTimes: Int, val escaped_json: String, val subjects: java.util.Set[String]) {

}