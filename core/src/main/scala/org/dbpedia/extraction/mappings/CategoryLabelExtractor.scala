package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor( context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends WikiPageExtractor
{
    private val labelProperty = context.ontology.properties("rdfs:label")
    
    private val quad = QuadBuilder(context.language, DBpediaDatasets.CategoryLabels, labelProperty, new Datatype("rdf:langString")) _

    override val datasets = Set(DBpediaDatasets.CategoryLabels)

    override def extract(node : WikiPage, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Category) Seq.empty
        else Seq(quad(subjectUri, node.title.decoded, node.sourceUri))
    }
}