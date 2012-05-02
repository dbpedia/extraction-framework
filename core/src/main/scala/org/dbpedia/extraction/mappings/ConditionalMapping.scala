package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.dataparser.StringParser

class ConditionalMapping( val cases : List[ConditionMapping], // must be public val for statistics
                          val defaultMappings : List[PropertyMapping], // must be public val for statistics
                          context : { } ) extends ClassMapping     //TODO remove unused argument
{
    override def extract(node : Node, subjectUri : String, pageContext : PageContext) : Graph = node match
    {
        case templateNode : TemplateNode =>
        {
            for(condition <- cases)
            {
                val graph = condition.extract(templateNode, subjectUri, pageContext)
                templateNode.annotation(TemplateMapping.INSTANCE_URI_ANNOTATION) match
                {
                    case Some(instanceURI : String) =>
                    {
                        return defaultMappings.map(mapping => mapping.extract(templateNode, instanceURI, pageContext))
                                              .foldLeft(graph)(_ merge _)
                    }
                    case _ =>
                }
            }
            new Graph()
        }
        case _ => new Graph()
    }
}

class ConditionMapping( templateProperty : String,
                        operator : String,
                        value : String,
                        val mapping : TemplateMapping, // must be public val for statistics
                        context : { } )    //TODO remove unused argument
{
    /** Check if templateProperty is defined */
    require(operator == "otherwise" || templateProperty != null, "templateProperty must be defined")
    /** Check if given operator is supported */
    require(List("isSet", "equals", "contains", "otherwise").contains(operator), "Invalid operator: " + operator +". Supported operators: isSet, equals, contains, otherwise")
    /** Check if value is defined */
    require(operator == "otherwise" || operator == "isSet" || value != null, "Value must be defined")

    def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(operator == "otherwise" || evaluate(node))
        {
            return mapping.extract(node, subjectUri, pageContext)
        }

        new Graph()
    }

    private def evaluate(node : TemplateNode) : Boolean =
    {
        val property = node.property(templateProperty).getOrElse(return false)
        val propertyText = StringParser.parse(property).getOrElse("").toLowerCase.trim

        operator match
        {
            case "isSet" => !propertyText.isEmpty
            case "equals" => propertyText == value.toLowerCase
            case "contains" => propertyText.contains(value.toLowerCase)
            case _ => false
        }
    }
}
