package org.dbpedia.extraction.mappings

import java.net.URI

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
  if (ontologyProperty.isInstanceOf[OntologyObjectProperty])
  {
    require(datatype == null, "expected no datatype for object property '"+ontologyProperty+"', but found datatype '"+datatype+"'")
    try {
      // if it is a URI return it directly
      val uri = new URI(value)
      if (uri.getScheme == null) "http://" + uri.toString
      else uri.toString
    } catch {
      // otherwise create a DBpedia resource URI
      case _ : Exception => context.language.resourceUri.append(value)
    }
  }

  override val datasets = Set(DBpediaDatasets.OntologyProperties)

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    Seq(new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, datatype))
  }


}