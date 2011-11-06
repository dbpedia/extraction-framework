package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad, Graph}
import org.dbpedia.extraction.ontology.{OntologyNamespaces, OntologyProperty, OntologyObjectProperty}
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
    private val encodedUriRegex = "^.*%[0-9a-fA-F][0-9a-fA-F].*$"

    if(ontologyProperty.isInstanceOf[OntologyObjectProperty])
    {
        require(unit == null, "unit == null if ontologyProperty.isInstanceOf[OntologyObjectProperty]")

        require(!value.matches(encodedUriRegex), "URI value must be decoded (must not contain any %XX)")

        val encodedUri = WikiUtil.wikiEncode(value, context.language)
        value = OntologyNamespaces.getResource(encodedUri, context.language)
    }

    override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        new Graph(
            new Quad(context.language, DBpediaDatasets.OntologyProperties, subjectUri, ontologyProperty, value, node.sourceUri, unit)
        )
    }


}