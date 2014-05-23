package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
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
  /**
   * Don't access context directly in methods. Cache context.language and context.ontology for use inside
   * methods so that Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language
  private val ontology = context.ontology

  val labelProperty = context.ontology.properties("rdfs:label")
  
  override val datasets = Set(DBpediaDatasets.Labels)

  override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main) return Seq.empty

    // TODO: use templates like {{lowercase}}, magic words like {{DISPLAYTITLE}}, 
    // remove stuff like "(1999 film)" from title...
    val label = page.title.decoded
    
    if(label.isEmpty) Seq.empty
    else Seq(new Quad(language, DBpediaDatasets.Labels, subjectUri, labelProperty, label, page.sourceUri, ontology.datatypes("rdf:langString")))
  }
}
