package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, URLEncoder}
import javax.xml.stream.events.StartElement

import org.dbpedia.extraction.util.RichStartElement.richStartElement
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.wikiparser.impl.wikipedia.GenerateWikiSettings

import scala.collection.{Set, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

/**
  * Collects all language links of a given wiki page
  */
class WikiLangLinkReader {

  /**
    * This heuristic will execute a query for each language link to collect all redirects.
    * @param language - language of the given page
    * @param title - title of the given page
    * @return - Map of languages to a collection of all alias names (redirects, incl. the given page and language)
    */
  def execute(language: Language, title: String, getRedirects: Boolean = true): Map[Language, Set[String]] ={

    val url = new URL(language.apiUri + "?" + WikiLangLinkReader.getLangLinkQuery(title))
    val caller = new WikiCaller(url, WikiDisambigReader.followRedirects)
    val langMap = new mutable.HashMap[Language, Set[String]]()
    val redirects = new ListBuffer[String]()

    caller.execute { stream =>
      val in = new XMLEventAnalyzer(WikiDisambigReader.factory.createXMLEventReader(stream))
      redirects.append(title)

      in.document { _ =>
        in.element("api") { _ =>
          in.ifElement("error") { error => throw new IOException(error.attr("info")) }
          in.ifElement("continue") { error => }
          in.ifElement("query") { _ =>
            in.element("pages") { _ =>
              in.element("page") { page =>
                in.ifElement("redirects") { _ =>
                  in.elements("rd"){ rd =>
                    readRedirect(rd).foreach(r => redirects.append(r))
                  }
                }
                in.ifElement("langlinks"){ _ =>
                  in.elements("ll"){ link =>
                    readLangLink(link, in).foreach(ll => {
                      if(getRedirects)
                        queryRedirects(ll._1, ll._2) match{
                          case Success(s) => langMap.put(ll._1, s ++ Set(ll._2))
                          case Failure(_) =>
                        }
                      else
                        langMap.put(ll._1, Set(ll._2))
                    })
                  }
                }
              }
            }
          }
        }
      }
    }
    //finally adding origin lang
    langMap.put(language, redirects.toSet)
    langMap.toMap
  }

  /**
    * Executes a new query to collect all redirects of a given page (and language)
    * @param language - language of the given page
    * @param title - title of the given page
    * @return - Set of aliases (redirects)
    */
  def queryRedirects(language: Language, title: String): Try[Set[String]] ={

    val url = new URL(language.apiUri + "?" + WikiLangLinkReader.getRedirectQuery(title))
    val caller = new WikiCaller(url, WikiDisambigReader.followRedirects)
    WikiDisambigReader.redirectExecute(caller)(readRedirect)
  }

  /**
    * reads redirect tags
    * @param page - page element
    * @return - page title
    */
  private def readRedirect(page: StartElement): Option[String] ={
    if(page.attr("ns").toInt == Namespace.Template.code) {
      page.getAttr("title")
    }
    else
      None
  }

  /**
    * reads langlink tag (ll), returning a tuple of language and title
    * @param page - langlink element
    * @param in - page or rd element
    * @return
    */
  private def readLangLink(page: StartElement, in: XMLEventAnalyzer): Option[(Language, String)] ={
      page.getAttr("lang") match{
        case Some(l) => Language.get(l) match{
          case Some(lang) => return Some(lang, in.text(link => link))
          case None =>
        }
        case None =>
      }
    ""  + in.text(link => link)  //collect link anyway
    None
  }
}

object WikiLangLinkReader{

  /**
    * The query for api.php, for all langlinks of a given page
    */
  def getLangLinkQuery(title: String) = "action=query&format=xml&prop=redirects%7Clanglinks&redirects=1&rdlimit=500&lllimit=500&titles=" + URLEncoder.encode(title, "UTF-8")

  /**
    * Query for all redirects of a given page
    * @param title
    * @return
    */
  def getRedirectQuery(title: String) = "action=query&format=xml&prop=redirects&redirects=1&rdlimit=500&titles=" + URLEncoder.encode(title, "UTF-8")

  def getLangLinksFor(title: String, lang: Language, targetLang: Language): Option[Set[String]] = {
    val map = new WikiLangLinkReader().execute(lang, title, getRedirects = false)
    map.get(targetLang)
  }
}
