package org.dbpedia.extraction.wikiparser


import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.datatypes.UnitDatatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.MemorySource
import org.dbpedia.extraction.util.Language
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.matchers.{BeMatcher, MatchResult}

import scala.math._
import org.dbpedia.extraction.ontology.{Ontology, OntologyDatatypes}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TemplateTransformParserTest extends FlatSpec with Matchers
{


  it should "unwrap {{Unbulleted list | [[Arthur D. Levinson]] ([[Chairman]]) }}" in
    {
      parse("en", "{{Unbulleted list | [[Arthur D. Levinson]] ([[Chairman]])}}") should be (Some("<br />[[Arthur D. Levinson|Arthur D. Levinson]] ([[Chairman|Chairman]])<br />"))
    }

  it should "unwrap {{FlattList...}}" in
    {
      parse("en",
        """{{Plainlist|
          |*[[item1]]
          |**[[Item 11]]
          |*Item 1 string
          |*[[item1]]
          |*Item 3 string
          |}}
          |""".stripMargin.trim) should be (Some(
        """
          |<br /><br />[[Item1|item1]]
          |<br /><br />[[Item 11|Item 11]]
          |<br />Item 1 string
          |<br />[[Item1|item1]]
          |<br />Item 3 string
          |<br />
          |""".stripMargin.trim))

    }


  it should "unwrap {{URL|https://www.dji.com DJI.com}}" in
    {
      parse("en", "{{url|https://www.dji.com DJI.com}}") should be (Some("[https://www.dji.com]"))
    }


  private val wikiParser = WikiParser.getInstance()

  private def parse(language : String, input : String, inconvertible : Boolean = false) : Option[String] =
  {
    val lang = Language(language)


    val page = new WikiPage(WikiTitle.parse("TestPage", lang), input)
    wikiParser(page).map(_.toWikiText)
  }
}