package org.dbpedia.extraction.wikiparser.impl.json

import java.nio.channels.NonReadableChannelException

import com.fasterxml.jackson.databind.{DeserializationFeature, JsonMappingException, ObjectMapper}
import org.dbpedia.extraction.util.WikidataUtil
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace, WikiPage}
import org.wikidata.wdtk.datamodel.helpers.{DatamodelMapper, JsonDeserializer}
//import org.wikidata.wdtk.datamodel.json.jackson.{JacksonItemDocument, JacksonPropertyDocument, JacksonTermedStatementDocument}

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
 * wikidata json parsed and converted to wikidata JsonDeserializer
 */

class JsonWikiParser {

  def apply(page: WikiPage): Option[JsonNode] = {
    if (page.format == null || page.format != "application/json") {
      None
    }
    else {

      try {
        getDocument(page,page.source)
      } catch {
        case e: JsonMappingException => {
          if (page.isRedirect){
            None //redirect page, nothing to extract
          } else {
            getDocument(page,fixBrokenJson(page.source))
          }
        }
      }
    }
  }

  private def getDocument(page: WikiPage, jsonString: String): Option[JsonNode] = {
    val document = new JsonDeserializer(WikidataUtil.wikidataDBpNamespace)

    Some(new JsonNode(page, document))
  }

  private def fixBrokenJson(jsonString: String): String = {
    jsonString.replace("claims\":[]", "claims\":{}").
      replace("descriptions\":[]", "descriptions\":{}").
      replace("sitelinks\":[]", "sitelinks\":{}").
      replace("labels\":[]", "labels\":{}")
  }
}
