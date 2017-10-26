package org.dbpedia.extraction.wikiparser.impl

import org.dbpedia.extraction.wikiparser.{PageNode, WikiPage, WikiParser, WikiParserException}
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import json.JsonWikiParser
import WikiParserWrapper._
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.wikiparser.impl.sweble.SwebleWrapper

/**
 * Created with IntelliJ IDEA.
 * User: andread
 * Date: 08/04/13
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */

object WikiParserWrapper {

  private val jsonParser = new JsonWikiParser()
  private val swebleWikiParser = new SwebleWrapper()

}

class WikiParserWrapper(wikiTextParserName: String) extends  WikiParser{

  /**
    * Parses WikiText source and returns its Abstract Syntax Tree.
    *
    * @param page              The page
    * @param templateRedirects - if available, the template redirects (usually taken from an extraction context)
    * @return The PageNode which represents the root of the AST
    * @throws WikiParserException if an error occured during parsing
    */
  override def apply(page: WikiPage, templateRedirects: Redirects = new Redirects(Map())) = {
    page.format match {
      case _ =>
        if (wikiTextParserName == null || wikiTextParserName.equals("simple")){
          SimpleWikiParser(page, templateRedirects)
        } else {
          swebleWikiParser(page, templateRedirects)
        }

    }
  }
}