package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, URLEncoder}
import javax.xml.stream.events.StartElement

import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.{Set, mutable}
import scala.collection.mutable.ListBuffer
import org.dbpedia.extraction.util.RichStartElement.richStartElement

class WikiCategoryMemberReader {

  /**
    * This heuristic will execute a query for each language link to collect all redirects.
    * @param language - language of the given page
    * @param category - title of the given page
    * @return - Map of languages to a collection of all alias names (redirects, incl. the given page and language)
    */
  def execute(language: Language, category: String, ns: Seq[Namespace] = Seq(Namespace.Main, Namespace.Category)): Set[String] ={

    var (memberSet, categorySet) = internalExecute(language, category, ns)

    var knowCats = categorySet
    while(categorySet.nonEmpty){
      val zw = categorySet.map(c => internalExecute(language, c, ns))
      categorySet = zw.flatMap(x => x._2).diff(knowCats)
      knowCats ++= categorySet
      memberSet ++= zw.flatMap(x => x._1)
    }
    //finally adding origin lang
    memberSet
  }

  private def internalExecute(language: Language, category: String, ns: Seq[Namespace] = Seq(Namespace.Main, Namespace.Category)): (Set[String], Set[String]) ={
    val url = new URL(language.apiUri + "?" + WikiCategoryMembers.getCategoryMemberQuery(category))
    val caller = new WikiCaller(url, WikiDisambigReader.followRedirects)
    val nsCodes = ns.map(n => n.code)
    val members = new ListBuffer[String]()
    val categories = new ListBuffer[String]()

    caller.execute { stream =>
      val in = new XMLEventAnalyzer(WikiDisambigReader.factory.createXMLEventReader(stream))
      in.document { _ =>
        in.element("api") { _ =>
          in.ifElement("error") { error => throw new IOException(error.attr("info")) }
          in.ifElement("continue") { error => }
          in.ifElement("query") { _ =>
            in.element("pages") { _ =>
              in.elements("page") { page =>
                val nss = page.attr("ns").toInt
                if( nss == Namespace.Category.code)
                  readPageOrRedirect(page).foreach(p => categories.append(p))
                else if(nsCodes.contains(nss))
                  readPageOrRedirect(page).foreach(p => members.append(p))
                in.ifElement("redirects") { _ =>
                  in.elements("rd"){ rd =>
                    readPageOrRedirect(rd).foreach(r => members.append(r))
                  }
                }
              }
            }
          }
        }
      }
    }
    (members.toSet, categories.toSet)
  }

  /**
    * reads redirect tags
    * @param page - page element
    * @return - page title
    */
  private def readPageOrRedirect(page: StartElement): Option[String] ={
    page.getAttr("title")
  }
}

object WikiCategoryMembers{
  private val query = "action=query&format=xml&prop=redirects&generator=categorymembers&rdprop=pageid%7Ctitle%7Cfragment&rdnamespace=10%7C14&rdlimit=500&gcmnamespace=10%7C14&gcmlimit=500&gcmtitle="
  def getCategoryMemberQuery(category: String): String = query + URLEncoder.encode(category, "UTF-8")

}