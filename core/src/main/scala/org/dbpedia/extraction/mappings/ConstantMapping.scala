package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{OntologyProperty, OntologyObjectProperty}
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.language.reflectiveCalls

/**
 * Used to map information that is only contained in the infobox template name, for example
 *
 * en:Infobox_Australian_Road
 * {{TemplateMapping
 *    | mapToClass = Road
 *    | mappings =
 *         {{ConstantMapping | ontologyProperty = country | value = Australia }}
 *   ...
 * }}
 */
class ConstantMapping (
  ontologyProperty: OntologyProperty,
  private var value : String,
  datatype : Datatype,
  context : {
    def language : Language
  } 
)
extends PropertyMapping
{
  /**
   * Don't access context directly in methods. Cache context.language for use inside methods so that
   * Spark (distributed-extraction-framework) does not have to serialize the whole context object
   */
  private val language = context.language

  if (ontologyProperty.isInstanceOf[OntologyObjectProperty])
  {
    require(datatype == null, "expected no datatype for object property '"+ontologyProperty+"', but found datatype '"+datatype+"'")
    value = context.language.resourceUri.append(value)
  }

  override val datasets = Set(DBpediaDatasets.OntologyProperties)

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    Seq(new Quad(language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, datatype))
  }


}