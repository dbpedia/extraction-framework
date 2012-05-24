package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer

/**
 * Extracts labels for Categories.
 */
class CategoryLabelExtractor( context : {
                                  def ontology : Ontology
                                  def language : Language } ) extends Extractor
{
    val labelProperty = context.ontology.properties("rdfs:label")

    override val datasets = Set(DBpediaDatasets.CategoryLabels)

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Category) Seq.empty
        else Seq(new Quad(context.language, DBpediaDatasets.CategoryLabels, subjectUri, labelProperty, node.title.decoded, node.sourceUri, new Datatype("xsd:string")))
    }
}