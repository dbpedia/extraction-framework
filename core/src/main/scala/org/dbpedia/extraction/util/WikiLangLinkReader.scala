package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, URLEncoder}
import javax.xml.stream.events.StartElement

import org.dbpedia.extraction.dataparser.RedirectFinder
import org.dbpedia.extraction.util.RichStartElement.richStartElement
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.{Set, mutable}
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

class WikiLangLinkReader {

  def execute(language: Language, title: String): Map[Language, Set[String]] ={

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
                      queryRedirects(ll._1, ll._2) match{
                        case Success(s) => langMap.put(ll._1, s)
                        case Failure(_) =>
                      }
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

  def queryRedirects(language: Language, title: String): Try[Set[String]] ={

    val url = new URL(language.apiUri + "?" + WikiLangLinkReader.getRedirectQuery(title))
    val caller = new WikiCaller(url, WikiDisambigReader.followRedirects)
    WikiDisambigReader.redirectExecute(caller)(readRedirect)
  }

  /**
    * reads normal page tag or redirect
    * @param page - page or rd element
    * @return
    */
  private def readRedirect(page: StartElement): Option[String] ={
    if(page.attr("ns").toInt == Namespace.Template.code) {
      page.getAttr("title")
    }
    else
      None
  }

  /**
    * reads normal page tag or redirect
    * @param page - page or rd element
    * @return
    */
  private def readLangLink(page: StartElement, in: XMLEventAnalyzer): Option[(Language, String)] ={
      page.getAttr("lang") match{
        case Some(l) => Some(Language(l), in.text(link => link))
        case None =>  None
      }
  }
}

object WikiLangLinkReader{

  /**
    * The query for api.php, without the leading '?'.
    */
  def getLangLinkQuery(title: String) = "action=query&format=xml&prop=redirects%7Clanglinks&redirects=1&rdlimit=500&lllimit=500&titles=" + URLEncoder.encode(title, "UTF-8")

  def getRedirectQuery(title: String) = "action=query&format=xml&prop=redirects&redirects=1&rdlimit=500&titles=" + URLEncoder.encode(title, "UTF-8")

}
