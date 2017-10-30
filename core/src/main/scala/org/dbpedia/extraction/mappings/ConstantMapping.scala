package org.dbpedia.extraction.mappings

import java.net.URI

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{OntologyObjectProperty, OntologyProperty}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.iri.UriUtils

import scala.language.reflectiveCalls
import scala.util.{Failure, Success}

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
@SoftwareAgentAnnotation(classOf[ConstantMapping], AnnotationType.Extractor)
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
    value = UriUtils.createURI(value) match{
      case Success(u) => if(u.isAbsolute)
          context.language.resourceUri.append(value)
        else u.toString
      case Failure(f) => context.language.resourceUri.append(value)
    }
  }

  override val datasets = Set(DBpediaDatasets.OntologyPropertiesObjects, DBpediaDatasets.OntologyPropertiesLiterals)

  override def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    Seq(new Quad(context.language, dataset, subjectUri, ontologyProperty, value, node.sourceIri, datatype))
  }


}