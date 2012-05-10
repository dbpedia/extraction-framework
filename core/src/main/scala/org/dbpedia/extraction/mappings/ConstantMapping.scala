package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad, Graph}
import org.dbpedia.extraction.ontology.{OntologyProperty, OntologyObjectProperty}
import org.dbpedia.extraction.util.{WikiUtil, Language}

/**
 * Used to map information that is only contained in the infobox template name, for example
 *
 * en:Infobox_Australian_Road
 * {{TemplateMapping
 *    | mapToClass = Road
 *    | mappings =
 *	     {{ConstantMapping | ontologyProperty = country | value = Australia }}
 *   ...
 * }}
 */
class ConstantMapping( ontologyProperty : OntologyProperty,
                       private var value : String,
                       unit : Datatype,
                       context : {
                          def language : Language } ) extends PropertyMapping
{
    if(ontologyProperty.isInstanceOf[OntologyObjectProperty])
    {
        require(unit == null, "unit == null if ontologyProperty.isInstanceOf[OntologyObjectProperty]")
        value = context.language.resourceUri.append(value)
    }

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        new Graph(
            new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, unit)
        )
    }


}