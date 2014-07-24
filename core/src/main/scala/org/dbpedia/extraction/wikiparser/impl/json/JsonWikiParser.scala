package org.dbpedia.extraction.wikiparser.impl.json

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.{Node, PageNode, WikiTitle,JsonNode}

import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import org.json
import org.json.JSONObject
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument
import org.wikidata.wdtk.dumpfiles.JsonConverter

import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.matching.Regex
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.util.Language

import JsonWikiParser._


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
  private val WikiLanguageRegex = """([^\s]+)wiki$""".r
}

class JsonWikiParser {

  implicit val formats = DefaultFormats

  def apply(page : WikiPage) : Option[JsonNode] =
  {
    if (page.format == null || page.format != "application/json")
    {
      None
    }
    else
    {
        JSONObject jsonObject = new JSONObject(page.source)
      ItemDocument itemDocument = new JsonConverter
            .convertToItemDocument(jsonObject, page.title)

      //new PageNode(page.title, page.id, page.revision, page.timestamp, page.contributorID, page.contributorName, false, false, nodes)
      Some(new JsonNode(page,itemDocument))
    }
  }

}
