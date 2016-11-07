package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts the number of characters in a wikipedia page
 */
class WikiPageLengthExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends WikiPageExtractor
{
  val wikiPageLengthProperty = context.ontology.properties("wikiPageLength")
  val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  override val datasets = Set(DBpediaDatasets.PageLength)

  override def extract(input : PageNode, subjectUri : String) : Seq[Quad] =
  {
    val page = input.asInstanceOf[WikiPage]
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) 
        return Seq.empty
    
    Seq(new Quad(context.language, DBpediaDatasets.PageLength, subjectUri, wikiPageLengthProperty, page.source.length.toString, page.sourceIri, nonNegativeInteger) )
  }
}
