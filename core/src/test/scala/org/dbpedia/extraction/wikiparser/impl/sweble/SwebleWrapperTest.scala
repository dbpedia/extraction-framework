package org.dbpedia.extraction.wikiparser.impl.sweble

import org.dbpedia.extraction.wikiparser.impl.json.JsonWikiParser
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.scalatest.{FlatSpec, PrivateMethodTester}
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.sweble.wikitext.engine.PageTitle


/**
  *
  */
@RunWith(classOf[JUnitRunner])
class SwebleWrapperTest extends FlatSpec with Matchers with PrivateMethodTester {


  "SwebleWrapper" should """return similar AST for formatted text""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
                 ''italic''
                 '''bold'''
                 '''''bold & italic'''''
                 <strike> strike text </strike>
               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    var h =0
    (sweblePageNode) should be (simplePageNode)

  }

  "SwebleWrapper" should """return similar AST for Links""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
                 Internal Link - [[Main Page]]
                 Piped Link - [[Main Page|different text]]
                 [[{{TALKPAGENAME}}|Discussion]]
                 [[Special:MyTalk]]

               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)

    (sweblePageNode) should be (simplePageNode)

  }

  "SwebleWrapper" should """return similar AST for Lists """ in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
       * Lists are easy to do:
       ** start every line
       * with a star
       ** more stars mean
       *** deeper levels

       # Numbered lists are good
       ## very organized
       ## easy to follow

       ; Mixed definition lists
       ; item 1 : definition
       :; sub-item 1 plus term
       :: two colons plus definition
       :; sub-item 2 : colon plus definition
       ; item 2
       : back to the main list
               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)

    (sweblePageNode) should be (simplePageNode)

  }

  "SwebleWrapper" should """return similar AST for Image Links""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
        [[File:example.jpg|frameless|border|caption]]
               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    (sweblePageNode) should be (simplePageNode)

  }

  "SwebleWrapper" should """return similar AST for Tables""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
                 {|
                 |Orange
                 |Apple
                 |-
                 |Bread
                 |Pie
                 |-
                 |Butter
                 |Ice cream
                 |}             """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    (sweblePageNode) should be (simplePageNode)

  }

  "SwebleWrapper" should """return similar AST for TemplateNodes""" in {
    val swebleWikiParser = WikiParserWrapper.swebleWikiParser
    val simpleWikiParser = WikiParserWrapper.simpleWikiParser
    var body = """
        {{Infobox Test1

                 | data37     = {{Example | temp_string1 | temp_string2  }}

        }}
               """
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    (sweblePageNode) should  be (simplePageNode)

  }
}
