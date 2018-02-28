package org.dbpedia.extraction.dataparser

import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.{CategoryRedirect, Redirect}

import scala.util.matching.Regex

/**
  * Created by chile on 25.10.17.
  * Extracted from org.dbpedia.extraction.mappings.Redirects.scala and made public
  */

class RedirectFinder private(lang : Language) extends (PageNode => Option[(WikiTitle, WikiTitle)])
{
  val regex: Regex = buildRegex
  private val logger = ExtractionLogger.getLogger(getClass, lang)

  private def buildRegex = {
    val redirects = Redirect(lang).mkString("|")
    // (?ius) enables CASE_INSENSITIVE UNICODE_CASE DOTALL
    // case insensitive and unicode are important - that's what mediawiki does.
    // Note: Although we do not specify a Locale, UNICODE_CASE does mostly the right thing.
    // DOTALL means that '.' also matches line terminators.
    // Reminder: (?:...) are non-capturing groups, '*?' is a reluctant qualifier.
    // (?:#[^\n]*?)? is an optional (the last '?') non-capturing group meaning: there may
    // be a '#' after which everything but line breaks is allowed ('[]{}|<>' are not allowed
    // before the '#'). The match is reluctant ('*?'), which means that we recognize ']]'
    // as early as possible.
    // (?:\|[^\n]*?)? is another optional non-capturing group that reluctantly consumes
    // a '|' character and everything but line breaks after it.
    ("""(?ius)^\s*(?:"""+redirects+""")\s*:?\s*\[\[([^\[\]{}|<>\n]+(?:#[^\n]*?)?)(?:\|[^\n]*?)?\]\].*""").r
  }

  override def apply(page : PageNode) : Option[(WikiTitle, WikiTitle)]= {
    //first we try to redirect category redirects (these are "soft redirects" and we need to look for the CategoryRedirect templates - see CategoryRedirect.scala)
    val destination =  if (page.isCategory) {
      val template = CategoryRedirect.get(page.title.language) match {
        case Some(crd) => page.children.map(c => c.containedTemplateNodes(crd)).filter(t => t.collectFirst { case y => y.children.size > 1 }.isDefined).flatten
        case None => Set()
      }
      if (template.nonEmpty) {
        val newCat = WikiTitle.parse(template.head.children.head.propertyNodeValueToPlainText, page.title.language)
        new WikiTitle(newCat.decoded, Namespace.Category, newCat.language, newCat.isInterLanguageLink, newCat.fragment, newCat.capitalizeLink, newCat.id)
      }
      else
        null.asInstanceOf[WikiTitle] //legacy
    }
    else {
      page.source match {
        case regex(dest) => {
          try {

            WikiTitle.parse(dest, page.title.language)
          }
          catch {
            case ex: WikiParserException => {
              logger.log(Level.WARN, "Couldn't parse redirect destination", ex)
              null
            }
          }
        }
        case _ => null
      }
    }

    Option(destination).map(d => (page.title, d))
  }
}

object RedirectFinder{

  private var rfs :Map[Language, RedirectFinder] = Map[Language, RedirectFinder]()

  def getRedirectFinder(lang:Language): RedirectFinder = {
    rfs.get(lang) match{
      case Some(r) => r
      case None =>
        val rf = new RedirectFinder(lang)
        rfs += lang -> rf
        rf
    }
  }
}
