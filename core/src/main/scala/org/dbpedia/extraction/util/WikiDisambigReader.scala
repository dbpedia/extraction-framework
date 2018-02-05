package org.dbpedia.extraction.util

import javax.xml.stream.XMLInputFactory

import scala.collection.Set
import org.dbpedia.extraction.util.RichStartElement.richStartElement
import java.io.{File, IOException}
import java.net.{URL, URLEncoder}
import javax.xml.stream.events.StartElement

import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.wikiparser.impl.wikipedia.{CuratedDisambiguation, DisambiguationCategories}

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}

object WikiDisambigReader {

  // If the page MediaWiki:Disambiguationspage doesn't exist, this query should return the 
  // template(s) set in languages/messages/MessagesXyz.php.

  /**
    * query for getting 500 category members with redirects(used to collect all disambiguation templates)
    * @param pageTitle - the categoriy title collecting such templates (see DisambiguationCategories)
    * @return
    */
  def getDisambTemplatesQuery(pageTitle: String) =
    "action=query&format=xml&prop=redirects&generator=categorymembers&rdprop=pageid%7Ctitle&rdlimit=500&gcmnamespace=10&gcmlimit=500&gcmtitle=" +
    URLEncoder.encode(pageTitle, "UTF-8")

  /**
    * query for collecting up to 500 occurrences of a given template (via pageid), together with external links of these pages
    * the number of external links per template is a good indicator whether a it is used as disambiguation template (disamb. pages have almost no external links)
    * @param pageid - pageid of a given template
    * @return
    */
  def getDisambTemplateEvalQuery(pageid: String) = "action=query&format=xml&prop=extlinks&generator=transcludedin&gtilimit=500&ellimit=500&pageids=" + pageid

  val followRedirects = false

  // newInstance is expensive, call it only once
  val factory = XMLInputFactory.newInstance

  /**
    * Some target domains of external links are exempt, since they are very close to Wikipedia
    */
  val domainLinkExceptions = Set("tools.wmflabs.org", "www.wikidata.org", "meta.wikimedia.org")

  // Let's use a regex since the main usage of this is in GenerateWikiSettings
  // and potentially we do not have Namespaces class yet, or we might not have a new
  // language yet
  // private val prefix = Namespaces.names(language)(Namespace.Template.code)+':'
  val TemplateNameRegex = """[^:]+:(.*)""".r

  // for every page with an external link at least 9 pages without have to exist
  // otherwise a template pointing to pages with a higher ratio can not be considered a disambiguation page
  private val MaxExtLinksToPageCountRatio = 0.1

  // is entered if no others are available
  val defaultDisambig = "Disambig"

  /**
    * Executes a new query to collect all redirects of a given page (and language)
    * @param caller - a WikiCaller set up with the necessary query URL
    * @param redirectCallback - callback to process the redirects collected
    * @return - Set of redirects
    */
  def redirectExecute(caller: WikiCaller)(redirectCallback: StartElement => Option[String]): Try[Set[String]] = {
    caller.execute { stream =>
      val in = new XMLEventAnalyzer(WikiDisambigReader.factory.createXMLEventReader(stream))
      val links = new ListBuffer[String]()
      in.document { _ =>
        in.element("api") { _ =>
          in.ifElement("error") { error => throw new IOException(error attr "info") }
          in.ifElement("query") { _ =>
            in.element("pages") { _ =>
              in.elements("page") { page =>
                redirectCallback(page).foreach(r => links.append(r))
                in.ifElement("redirects") { _ =>
                  in.elements("rd") { rd =>
                    redirectCallback(rd).foreach(r => links.append(r))
                  }
                }
              }
            }
          }
        }
      }
      //return set
      links.toSet
    }
  }
}

/**
 * Reads result of the api.php query above.
 */
class WikiDisambigReader(language: Language) {

  /**
    * reads all members of a given category and their redirects
    * @param file - target file
    * @param overwrite
    * @return
    */
  def execute(file: File, overwrite: Boolean = true): Try[Set[String]] ={
    val categoryPage = DisambiguationCategories.get(language) match{
      case Some(s) => s
      case None => return Try(CuratedDisambiguation.get(language))
    }
    val url = new URL(language.apiUri + "?" + WikiDisambigReader.getDisambTemplatesQuery(categoryPage))
    val caller = new LazyWikiCaller(url, WikiDisambigReader.followRedirects, file, overwrite)
    WikiDisambigReader.redirectExecute(caller)(readDisambElement) match{
      case Success(s) => Success{s ++ Set(WikiDisambigReader.defaultDisambig)}
      case Failure(f) => Failure(f)
    }
  }

  /**
    * executes a query for a given template, determining whether this template is used as redirect template or for other purposes
    * for this purpose, we have a look at the external lniks of a page, since the rate of external links in disambig pages should be significantly lower
    * @param pageid
    * @return
    */
  private def decideOnDisambTemplate(pageid: String): Boolean ={
    val url = new URL(language.apiUri + "?" + WikiDisambigReader.getDisambTemplateEvalQuery(pageid))
    val disambCaller = new WikiCaller(url, WikiDisambigReader.followRedirects)
    val links = new ListBuffer[URL]()
    var pagecount = 0

    disambCaller.execute(stream => {
      val in = new XMLEventAnalyzer(WikiDisambigReader.factory.createXMLEventReader(stream))
      in.document { _ =>
        in.element("api") { _ =>
          in.ifElement("error") { error => throw new IOException(error attr "info") }
          in.ifElement("continue") { error => }
          in.ifElement("query") { _ =>
            in.element("pages") { _ =>
              in.elements("page") { page =>
                pagecount += 1
                in.ifElement("extlinks") {_ =>
                  in.elements("el"){ _ =>
                    in.text(link => try{links.append(new URL(link.trim))}catch{case e: Exception => })
                  }
                }
              }
            }
          }
        }
      }
    }) match {
      case Failure(f) => throw f
      case Success(s) => s
    }
    val externalLinkCount = links.filterNot(x => WikiDisambigReader.domainLinkExceptions.contains(x.getHost)).size
    pagecount > 0 && externalLinkCount.doubleValue() / pagecount.doubleValue() < WikiDisambigReader.MaxExtLinksToPageCountRatio
  }

  /**
    * reads normal page tag or redirect
    * @param page - page or rd element
    * @return
    */
  private def readDisambElement(page: StartElement): Option[String] ={
    if(page.attr("ns").toInt == Namespace.Template.code) {
      page.getAttr("pageid") match{
        case Some(pageid) => page.attr("title") match{
          case WikiDisambigReader.TemplateNameRegex(templateName) => if(decideOnDisambTemplate(pageid))
            Some(templateName)
          else
            None
          case _ => None
        }
        case None => None
      }
    }
    else
      None
  }
}