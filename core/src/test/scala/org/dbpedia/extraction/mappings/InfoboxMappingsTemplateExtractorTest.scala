package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiPage, XMLSource}
import org.dbpedia.extraction.util.Language
import org.scalatest.{Matchers, PrivateMethodTester, FlatSpec}

import org.dbpedia.extraction.wikiparser.{WikiTitle, WikiParser}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
/**
  * Created by aditya on 6/21/16.
  */
@RunWith(classOf[JUnitRunner])
class InfoboxMappingsTemplateExtractorTest  extends FlatSpec with Matchers with PrivateMethodTester{

  "InfoboxMappingsExtractor" should """return correct property id's for conditional expressions """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","string1","P1082"), ("Infobox Test1","string2","P1082"), ("Infobox Test1","string4","P1082"))
    val parsed = parse(
      """
        {{Infobox Test1

        | data37    = {{#ifeq: temp_string1 | temp_string2 | temp_string3 | temp_string4 }}
        | data38    = {{#ifeq: string1 | string2 |{{#property:P1082}} | string4 }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for incorrect conditional expressions """ in {

    val lang = Language.English
    val answer = List()
    val parsed = parse(
      """
        {{Infobox Test1

        | data37    = {{#ifeq: temp_string1 | temp_string2 | temp_string3 | temp_string4 }}
        | data38    = {{#ifeq: string1 | string2 |{{#property:P1082}} |  {{#invoke:Wikidata|property|p456}} }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }



  "InfoboxMappingsExtractor" should """return correct property id's for conditional expressions with one nested level """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","string1","p123"), ("Infobox Test1","string2","p123"), ("Infobox Test1","value if non-empty","p123"), ("Infobox Test1","value if empty","p123"), ("Infobox Test1","value if different","p123"))
    val parsed = parse(
      """
        {{Infobox Test1
        |data39   = {{#ifeq: string1 | string2 | {{#if: {{#property:p123}} | value if non-empty | value if empty }} | value if different }}
        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  "InfoboxMappingsExtractor" should """return correct property id's for conditional expressions with multiple nested level """ in {

    val lang = Language.English
    val answer = List(("Infobox Test1","string1","p1243"), ("Infobox Test1","string2","p1243"), ("Infobox Test1","test_string1","p1243"),
      ("Infobox Test1","test_string2","p1243"), ("Infobox Test1","test_string3","p1243"),("Infobox Test1","test_string4","p1243"), ("Infobox Test1","test_string5","p1243"))
    val parsed = parse(
      """
        {{Infobox Test1
        |data40   = {{#ifeq: string1 | string2 | {{#if: test_string1 |  {{#ifexist: {{#property:p1243}} | test_string2 | test_string3 }}| test_string4 }} | test_string5 }}        }}
      """, "TestPage", lang, "all")

    (parsed) should be (answer)
  }

  private val parser = WikiParser.getInstance()

  private def parse(input : String, title: String = "TestPage", lang: Language = Language.English, test : String) : List[(String,String, String)] =
  {
    val page = new WikiPage(WikiTitle.parse(title, lang), input)
    val context = new {
      def ontology = InfoboxMappingsTemplateExtractorTest.context.ontology;
      def language = lang;
      def redirects = new Redirects(Map("Official" -> "Official website"))
    }

    val extractor = new InfoboxMappingsTemplateExtractor(context)
    var to_return : List[(String,String, String)]= List.empty
      to_return =  parser(page) match {
        case Some(pageNode) => extractor.getTuplesFromConditionalExpressions(pageNode, Language.English)
        case None => List.empty
      }
    to_return
  }
}


object InfoboxMappingsTemplateExtractorTest {

  val context = new {
    def ontology = {
      val ontoFilePath = "../ontology.xml"
      val ontoFile = new File(ontoFilePath)
      val ontologySource = XMLSource.fromFile(ontoFile, Language.Mappings)
      new OntologyReader().read(ontologySource)
    }
    def language = "en"
    def redirects = new Redirects(Map())
  }

}