  package org.dbpedia.extraction.wikiparser.impl.sweble

  import java.util

  import org.dbpedia.extraction.wikiparser.impl.WikiParserWrapper
  import org.dbpedia.extraction.sources.WikiPage
  import org.dbpedia.extraction.util.Language
  import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}
  import org.junit.Test
  import java.util.Collection
  import org.junit.runner.RunWith
  import org.junit.runners.Parameterized
  import org.junit.runners.Parameterized.Parameters


  @ RunWith (value = classOf[Parameterized] )
  class SwebleWrapperFailureTest(body: String) {
    @Test def test() = {
       val swebleWikiParser = new WikiParserWrapper("sweble")
       val simpleWikiParser = new WikiParserWrapper("simple")
       var pgTitle = WikiTitle.parse("Title", Language.English)
       var page = new WikiPage(pgTitle, body);

       var simplePageNode: Option[PageNode] = simpleWikiParser.apply(page)
       var sweblePageNode: Option[PageNode] = swebleWikiParser.apply(page)
       assert(sweblePageNode != simplePageNode)
    }
  }

  object SwebleWrapperFailureTest {
    @Parameters def parameters: Collection[Array[String]] =
    {
      val data = new util.ArrayList[Array[String]]
      var allTests = scala.io.Source.fromFile("src/test/scala/org/dbpedia/extraction/wikiparser/impl/sweble/SwebleFailureTestCases.txt").mkString
      allTests.split("----------------------------------------\n").foreach(n => data.add(Array(n)))
      data
    }

  }
