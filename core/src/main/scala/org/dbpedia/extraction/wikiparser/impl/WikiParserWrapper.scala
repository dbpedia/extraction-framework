package org.dbpedia.extraction.wikiparser.impl

import org.dbpedia.extraction.wikiparser.{PageNode, WikiParser}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

import org.dbpedia.extraction.sources.WikiPageFormat
import json.JsonWikiParser

import WikiParserWrapper._
import org.dbpedia.extraction.wikiparser.impl.sweble.SwebleWrapper

/**
 * Created with IntelliJ IDEA.
 * User: andread
 * Date: 08/04/13
 * Time: 14:11
 * To change this template use File | Settings | File Templates.
 */

object WikiParserWrapper {

  private val simpleWikiParser = new SimpleWikiParser()
  private val jsonParser = new JsonWikiParser()
  private val swebleWikiParser = new SwebleWrapper

}

class WikiParserWrapper(wikiTextParserName: String) extends WikiParser {

  def apply(page : WikiPage) : PageNode =
  {
     page.format match {
       case WikiPageFormat.WikiText =>
         if (wikiTextParserName == null || wikiTextParserName.equals("simple")){
           simpleWikiParser(page)
         } else {
           swebleWikiParser(page)
         }
       case WikiPageFormat.Json => jsonParser(page)
     }
  }
}
