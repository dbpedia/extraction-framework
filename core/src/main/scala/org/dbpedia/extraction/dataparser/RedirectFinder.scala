package org.dbpedia.extraction.dataparser

import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Redirect

import scala.util.matching.Regex

/**
  * Created by chile on 25.10.17.
  * Extracted from org.dbpedia.extraction.mappings.Redirects.scala and made public
  */

class RedirectFinder private(lang : Language) extends (PageNode => Option[(WikiTitle, WikiTitle)])
{
  val regex: Regex = buildRegex

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

  override def apply(page : PageNode) : Option[(WikiTitle, WikiTitle)]=
  {
    val destinationTitle : WikiTitle =
      page.source match {
        case regex(destination) => {
          try {

            WikiTitle.parse(destination, page.title.language)
          }
          catch {
            case ex : WikiParserException => {
              Logger.getLogger(Redirects.getClass.getName).log(Level.WARNING, "Couldn't parse redirect destination", ex)
              null
            }
          }
        }
        case _ => null
      }

    if(destinationTitle != null && page.title.namespace == Namespace.Template && destinationTitle.namespace == Namespace.Template)
    {
      Some((page.title, destinationTitle))
    }
    else
    {
      None
    }
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
