package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor( context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends Extractor
{
    private val labelProperty = context.ontology.properties("rdfs:label")
    
    private val quad = QuadBuilder(context.language, DBpediaDatasets.CategoryLabels, labelProperty, new Datatype("xsd:string")) _

    override val datasets = Set(DBpediaDatasets.CategoryLabels)

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Category) Seq.empty
        else Seq(quad(subjectUri, node.title.decoded, node.sourceUri))
    }
}