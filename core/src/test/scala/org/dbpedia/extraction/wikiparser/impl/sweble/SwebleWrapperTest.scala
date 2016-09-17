package org.dbpedia.extraction.wikiparser.impl.sweble


import java.util
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
import org.junit.Test
import java.util.Collection
import org.dbpedia.extraction.wikiparser.impl.WikiParserWrapper
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.junit.Assert._


@RunWith(value = classOf[Parameterized])
class SwebleWrapperTest (body : String) {
  // Test if the formula equals its parsed and pretty-printed self
  @Test def test() = {
    val swebleWikiParser = new WikiParserWrapper("sweble")
    val simpleWikiParser = new WikiParserWrapper("simple")
    var pgTitle = WikiTitle.parse("Title", Language.English)
    var page = new WikiPage(pgTitle, body);
    var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
    var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
    assertEquals(sweblePageNode, simplePageNode);
  }
}


object SwebleWrapperTest {
  @Parameters def parameters: Collection[Array[String]] = {
    val data = new util.ArrayList[Array[String]]
    var allTests = scala.io.Source.fromFile("src/test/scala/org/dbpedia/extraction/wikiparser/impl/sweble/SwebleTestCases.txt").mkString
    allTests.split("----------------------------------------\n").foreach(n => data.add(Array(n)))
    data
  }
}
