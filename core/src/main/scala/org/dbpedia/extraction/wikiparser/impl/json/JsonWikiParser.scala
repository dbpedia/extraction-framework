package org.dbpedia.extraction.wikiparser.impl.json

import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.{WikidataInterWikiLinkNode, Node, PageNode, WikiTitle}
import net.liftweb.json.DefaultFormats
import net.liftweb.json.JsonParser._
import scala.collection.mutable.{ArrayBuffer, HashMap}
import scala.util.matching.Regex
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.util.Language

import JsonWikiParser._
import net.liftweb.json.JsonAST._

/**
 * Created with IntelliJ IDEA.
 * User: andread
 * Date: 08/04/13
 * Time: 14:22
 * To change this template use File | Settings | File Templates.
 */
object JsonWikiParser {
  private val WikiLanguageRegex = """([^\s]+)wiki""".r
}

class JsonWikiParser {

  implicit val formats = DefaultFormats

  def apply(page : WikiPage) : PageNode =
  {
    val nodes = collectInterLanguageLinks(page)
    // Return page node
    new PageNode(page.title, page.id, page.revision, page.timestamp, page.contributorID, page.contributorName, false, false, nodes)
  }

  def collectInterLanguageLinks(page: WikiPage) : List[Node] = {

    var nodes = List[Node]()
    val json = page.source

    val parsedText = parseOpt(json)

    val jsonObjMap = parsedText match {
      case Some(map) => map
      case _ => throw new IllegalStateException("Invalid JSON representation!")
    }

    val interLinks = (jsonObjMap \ "links") match {
      case JObject(links) => links
      case _ => List()
    }

    val interLinksMap = collection.mutable.Map[String, String]()

    interLinks.foreach { interLink : JField =>
      interLink.name match {
          case WikiLanguageRegex(lang) =>  interLinksMap += lang -> interLink.value.extract[String]
          case _ =>
      }
    }

    if (! interLinksMap.contains("en")) return nodes

    val sourceTitle = WikiTitle.parse(interLinksMap.get("en").get, Language.English)
    // Do not generate a link to the defaul language itself
    interLinksMap -= "en"

    interLinksMap.foreach {
      case (key, value) =>
        Language.map.get(key) match {
          case Some(lang) =>
            val destinationTitle = WikiTitle.parse(key + ":" + value, lang)
            nodes ::= WikidataInterWikiLinkNode(sourceTitle, destinationTitle)
          case _ =>
        }
      case _ =>
    }

    nodes
  }
}
