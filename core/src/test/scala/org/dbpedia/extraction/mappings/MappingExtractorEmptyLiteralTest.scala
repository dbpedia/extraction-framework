package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{TemplateNode, WikiPage, WikiParser, WikiTitle}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class MappingExtractorEmptyLiteralTest extends FlatSpec with Matchers {

  "MappingExtractor" should "omit empty literals while preserving non-empty mapping output" in {
    val mapping = new Extractor[TemplateNode] {
      override val datasets = Set(DBpediaDatasets.OntologyPropertiesLiterals)

      override def extract(input: TemplateNode, subjectUri: String): Seq[Quad] = Seq(
        new Quad("am", DBpediaDatasets.OntologyPropertiesLiterals.encoded, subjectUri,
          "http://dbpedia.org/ontology/name", "", input.sourceIri,
          "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString"),
        new Quad("am", DBpediaDatasets.OntologyPropertiesLiterals.encoded, subjectUri,
          "http://dbpedia.org/ontology/name", "የሙከራ ስም", input.sourceIri,
          "http://www.w3.org/1999/02/22-rdf-syntax-ns#langString")
      )
    }
    val context = new {
      def mappings = new Mappings(Map("Test mapping" -> mapping), Nil)
      def redirects = new Redirects(Map.empty)
    }
    val language = Language("am")
    val page = new WikiPage(
      WikiTitle.parse("የሙከራ ገጽ", language),
      "{{Test mapping}}"
    )
    val pageNode = WikiParser.getInstance()(page).get

    val quads = new MappingExtractor(context).extract(pageNode, page.title.resourceIri)

    quads.map(_.value) should equal(Seq("የሙከራ ስም"))
  }
}
