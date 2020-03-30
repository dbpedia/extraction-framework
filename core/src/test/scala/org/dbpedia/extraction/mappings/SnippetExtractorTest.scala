package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiParser, WikiTitle}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

import scala.language.reflectiveCalls

/**
  *
  */
@RunWith(classOf[JUnitRunner])
class SnippetExtractorTest extends FlatSpec with Matchers with PrivateMethodTester {

  "InfoboxMappingsExtractor" should """return correct property id's for YM infobox""" in {

    val lang = Language.English

    val quads = extractPageNode(
      """
        |== External links ==
        | * [http://www.homepage.com Official website]
        |""".stripMargin,
      homepageExtractor)


      val answer: Seq[Quad] = Seq(
       Quad.unapply("<http://dbpedia.org/resource/TestPage> <http://xmlns.com/foaf/0.1/homepage> <http://www.homepage.com> ." )
      ).map(_.get)

    (quads) should be (answer)
  }

  private val parser = WikiParser.getInstance()

  private val context = new {
    def ontology = InfoboxMappingsExtractorTest.context.ontology;
    def language = Language.English;
    def redirects = new Redirects(Map())
  }
  private val homepageExtractor: PageNodeExtractor = new HomepageExtractor(context)


  private def extractPageNode(input : String, extractor: PageNodeExtractor) : Seq[Quad] =
  {
    val title = "TestPage"
    val subjectUri = "http://dbpedia.org/resource/" + title
    val page = new WikiPage(WikiTitle.parse("TestPage", Language.English), input)

    extractor.extract(page.pageNode.get, subjectUri)
        .map(quad => quad.copy(dataset =  null, context = null, language = if (quad.datatype != null && quad.datatype.endsWith("langString")) quad.datatype else null)) // remove dataset

  }
}

