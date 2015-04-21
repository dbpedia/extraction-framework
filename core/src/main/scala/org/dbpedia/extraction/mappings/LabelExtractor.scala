package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.ExtractorUtils
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

/**
 * Extracts labels to articles based on their title.
 */
class LabelExtractor( 
  context : {
    def ontology : Ontology
    def language : Language 
  } 
) 
extends WikiPageExtractor
{
  val labelProperty = context.ontology.properties("rdfs:label")
  
  override val datasets = Set(DBpediaDatasets.Labels)

  override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

    // TODO: use templates like {{lowercase}}, magic words like {{DISPLAYTITLE}}, 
    // remove stuff like "(1999 film)" from title...
    val label = page.title.decoded
    
    if(label.isEmpty) Seq.empty
    else Seq(new Quad(context.language, DBpediaDatasets.Labels, subjectUri, labelProperty, label, page.sourceUri, context.ontology.datatypes("rdf:langString")))
  }
}
