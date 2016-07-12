package org.dbpedia.extraction.mappings

import java.net.URI

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{OntologyProperty, OntologyObjectProperty}
import org.dbpedia.extraction.util.{UriUtils, Language}
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
  val ontologyProperty: OntologyProperty,
  var value : String,
  val datatype : Datatype,
  context : {
    def language : Language
  } 
)
extends PropertyMapping
{
  val isObjectProperty = ontologyProperty.isInstanceOf[OntologyObjectProperty]

  //split to literal / object dataset
  val dataset = if (isObjectProperty) DBpediaDatasets.OntologyPropertiesObjects else DBpediaDatasets.OntologyPropertiesLiterals

  if (isObjectProperty)
  {
    require(datatype == null, "expected no datatype for object property '"+ontologyProperty+"', but found datatype '"+datatype+"'")
    value = try {
      // if it is a URI return it directly
      val uri = new URI(value)
      // if the URI is absolute, we can use it directly. otherwise we make a DBpedia resource URI
      if (!uri.isAbsolute) context.language.resourceUri.append(value)
      else uri.toString
    } catch {
      // otherwise create a DBpedia resource URI
      case _ : Exception => context.language.resourceUri.append(value)
    }
  }

  override val datasets = Set(DBpediaDatasets.OntologyPropertiesObjects, DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    Seq(new Quad(context.language, dataset, subjectUri, ontologyProperty, value, node.sourceUri, datatype))
  }


}