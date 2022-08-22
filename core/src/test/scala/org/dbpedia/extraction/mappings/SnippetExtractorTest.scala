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
  private val parser = WikiParser.getInstance()

  private val context = new {
    def ontology = InfoboxMappingsExtractorTest.context.ontology;
    def language = Language.English;
    def redirects = new Redirects(Map())
  }


  //TODO LANGUAGE IS NOT USED CORRECTLY
  private def extractPageNode(input: String, extractor: PageNodeExtractor, lang:Language): Seq[Quad] = {
    val title = "TestPage"
    val subjectUri = "http://dbpedia.org/resource/" + title
    val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)

    extractor.extract(page.pageNode.get, subjectUri)
      .map(quad => quad.copy(dataset = null, context = null, language = if (quad.datatype != null && quad.datatype.endsWith("langString")) quad.datatype else null)) // remove dataset

  }
/* TESTS */

  private val homepageExtractor: PageNodeExtractor = new HomepageExtractor(context)
  private val infoboxExtractor: PageNodeExtractor = new InfoboxExtractor(context)

  "HomepageExtractor" should """return correct homepage for external links""" in {

    val quads = extractPageNode(
      """
        |== External links ==
        | * [http://www.homepage.com Official website]
        |""".stripMargin,
      homepageExtractor,
      Language.English)



    val answer: Seq[Quad] = Seq(
      Quad.unapply("<http://dbpedia.org/resource/TestPage> <http://xmlns.com/foaf/0.1/homepage> <http://www.homepage.com> .")
    ).map(_.get)

    (quads) should be(answer)
  }

  "InfoboxExtractor" should """recent bug""" in {

    //https://en.wikipedia.org/wiki/Abbas_Gharabaghi
    val wiki =
      """
        |{{Infobox Officeholder
        || birth_place         = [[Tabriz]], Iran
        || birth_place2         = [[Tabriz]]
        |}}
        |""".stripMargin
    print(wiki)

    val quads = extractPageNode(
      wiki,
      infoboxExtractor,
      Language.English)

    //TODO how do you print the triples as ntriples (for posting into answer)
    quads.foreach(println)

    val answer: Seq[Quad] = Seq(
      Quad.unapply("<http://dbpedia.org/resource/TestPage> <http://xmlns.com/foaf/0.1/homepage> <http://www.homepage.com> .")
    ).map(_.get)

    //(quads) should be(answer)
  }


}

