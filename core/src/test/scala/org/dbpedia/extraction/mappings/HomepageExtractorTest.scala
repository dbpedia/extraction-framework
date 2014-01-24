package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.destinations.{QuadBuilder, Quad}
import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.ontology.io.OntologyReader
import io.Source
import org.dbpedia.extraction.util.Language
import java.io.{FilenameFilter, File}
import scala.collection.mutable.ArrayBuffer
import org.junit.Test
import org.dbpedia.extraction.ontology.{OntologyProperty, Ontology}
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.dbpedia.extraction.dataparser.BooleanParser

/**
 *
 */
@RunWith(classOf[JUnitRunner])
class HomepageExtractorTest extends FlatSpec with ShouldMatchers
{
  // Tests
  "HomepageExtractor" should "return http://example.com from ExternalLink in External links section" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse(
      """
        |==External links==
        |
        |* [http://example.com Official website]
        |
      """.stripMargin, "TestPage", lang) should equal (quad)
  }

  "HomepageExtractor" should """return http://example.com from Template 'Official website' in External links section""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse(
      """
        |==External links==
        |
        |* {{Official website|example.com}}
        |
      """.stripMargin, "TestPage", lang) should equal (quad)
  }

  it should """return http://correct.example.com from Template 'Official website' in External links section""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://correct.example.com", null, null)
    )

    parse(
      """
        |==External links==
        |
        |* {{Official website|2=example.com}}
        |* {{Official website|correct.example.com}}
        |
      """.stripMargin, "TestPage", lang) should equal (quad)
  }

  "HomepageExtractor" should """return http://example.com from Template 'Official' in External links section""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse(
      """
        |==External links==
        |
        |* {{Official|http://example.com}}
        |
      """.stripMargin, "TestPage", lang) should equal (quad)
  }

  "HomepageExtractor" should """return http://example.com from Template property 'website = example.com'""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse("""{{Infobox | website = example.com}}""", "TestPage", lang) should equal (quad)
  }

  "HomepageExtractor" should """return http://example.com from Template property 'website = http://example.com'""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse("""{{Infobox | website = http://example.com}}""", "TestPage", lang) should equal (quad)
  }

  "HomepageExtractor" should """return http://example.com from Template property 'website = http://example.com or http://or.com'""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse("""{{Infobox | website = http://example.com or http://or.com}}""", "TestPage", lang) should equal (quad)
  }

  it should """return Seq.empty from Template property 'website = N/A'""" in {

    val lang = Language.English
    val quad : Seq[Quad] = HomepageExtractorTest.datasets.toList.map(dataset =>
      new Quad(lang, dataset, "TestPage", HomepageExtractorTest.homepageProperty, "http://example.com", null, null)
    )

    parse("""{{Infobox | website = N/A}}""", "TestPage", lang) should equal (Seq.empty)
  }

  // end of tests

  private val parser = WikiParser.getInstance()

  private def parse(input : String, title: String = "TestPage", lang: Language = Language.English) : Seq[Quad] =
  {
    val page = new WikiPage(WikiTitle.parse(title, lang), input)
    val context = new {
      def ontology = HomepageExtractorTest.ontology;
      def language = lang;
      def redirects = new Redirects(Map("Official" -> "Official website"))
    }

    val extractor = new HomepageExtractor(context)
    parser(page) match {
      case Some(pageNode) => extractor.extract(pageNode,"TestPage", new PageContext())
      case None => Seq.empty
    }

  }
}

object HomepageExtractorTest {

  // We need the OntologyProperty for "foaf:homepage"
  private val homepageProperty = new OntologyProperty("Foaf:homepage", Map(Language.English -> "homepage"), Map(), null, null, false, Set())

  /**
   *   val classes : Map[String, OntologyClass],
  val properties : Map[String, OntologyProperty],
  val datatypes : Map[String, Datatype],
  val specializations : Map[(OntologyClass, OntologyProperty), UnitDatatype],
  val equivalentPropertiesMap : Map[OntologyProperty,Set[OntologyProperty]],
  val equivalentClassesMap : Map[OntologyProperty,Set[OntologyProperty]]

  name: String,
  labels: Map[Language, String],
  comments: Map[Language, String],
  val domain: OntologyClass,
  val range: OntologyType,
  val isFunctional: Boolean,
  val equivalentProperties: Set[OntologyProperty]
   */
  private val ontology = new Ontology(
    Map(),
    Map("foaf:homepage" -> homepageProperty),
    Map(),
    Map(),
    Map(),
    Map()
  )

  private val datasets = new HomepageExtractor(new {
    def ontology = HomepageExtractorTest.ontology;
    def language = Language.English;
    def redirects = null
  }).datasets
}