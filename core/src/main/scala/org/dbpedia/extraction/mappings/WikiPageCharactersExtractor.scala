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
class WikiPageCharactersExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends WikiPageExtractor
{
  val wikiPageCharacterSizeProperty = context.ontology.properties("wikiPageCharacterSize")
  val nonNegativeInteger = context.ontology.datatypes("xsd:nonNegativeInteger")

  override val datasets = Set(DBpediaDatasets.CharacterSize)

  override def extract(page : WikiPage, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty
    
    Seq(new Quad(context.language, DBpediaDatasets.CharacterSize, subjectUri, wikiPageCharacterSizeProperty, page.source.length.toString, page.sourceUri, nonNegativeInteger) )
  }
}