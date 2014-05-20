package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{QuadBuilder, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

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

  override def extract(page : WikiPage, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    // Ignore files that are not in Main, *unless* they're
    // File:s on the Commons.
    if(page.title.namespace != Namespace.Main && 
        !(page.title.namespace == Namespace.File && 
        context.language.wikiCode == "commons")
    ) return Seq.empty

    Seq(new Quad(context.language, DBpediaDatasets.PageLength, subjectUri, wikiPageLengthProperty, page.source.length.toString, page.sourceUri, nonNegativeInteger) )
  }
}
