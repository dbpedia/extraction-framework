package org.dbpedia.extraction.wikiparser.impl.json

import com.fasterxml.jackson.databind.{JsonMappingException, DeserializationFeature, ObjectMapper}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.WikidataUtil
import org.dbpedia.extraction.wikiparser.{Namespace, JsonNode}
import org.wikidata.wdtk.datamodel.json.jackson.{JacksonTermedStatementDocument, JacksonPropertyDocument, JacksonItemDocument}

import scala.util.matching.Regex


/**
 * Created with IntelliJ IDEA.
 * User: andread
 * Date: 08/04/13
 * Time: 14:22
 * To change this template use File | Settings | File Templates.
 */
object JsonWikiParser {
  /* the regex should search for languageslinks like "enwiki" only
  so that "enwikivoyage" for example wouldn't be acceptable because they wouldn't match a DBpedia entity
  */
  private val WikiLanguageRegex = """([^\s]+)wiki$"""
}

/**
 * JsonWikiParser class use wikidata Toolkit to parse wikidata json
 * wikidata json parsed and converted to wikidata ItemDocument
 */

class JsonWikiParser {

  def apply(page: WikiPage): Option[JsonNode] = {
    if (page.format == null || page.format != "application/json") {
      None
    }
    else {

      val mapper = new ObjectMapper()
      val jsonString = fixBrokenJson(page.source)

      try {
        val jacksonDocument = mapper.readValue(jsonString, classOf[JacksonTermedStatementDocument])
        jacksonDocument.setSiteIri(WikidataUtil.wikidataDBpNamespace)
        Some(new JsonNode(page, jacksonDocument))

      } catch {
        case e: JsonMappingException => throw new JsonMappingException("no data in redirect pages")
      }


    }
  }

  private def fixBrokenJson(jsonString:String): String = {
    jsonString.replace("claims\":[]","claims\":{}").
      replace("descriptions\":[]","descriptions\":{}").
      replace("sitelinks\":[]","sitelinks\":{}").
      replace("labels\":[]","labels\":{}")
  }
}