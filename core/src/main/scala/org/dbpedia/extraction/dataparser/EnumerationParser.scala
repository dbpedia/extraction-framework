package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{Node, TextNode}
import org.dbpedia.extraction.ontology.datatypes.{EnumerationDatatype}

/**
 * Parses enumerations.
 */
class EnumerationParser(datatype : EnumerationDatatype) extends DataParser
{
    override def parse(node : Node) : Option[String] =
    {
        node match
        {
            case TextNode(text, line) => datatype.parse(text)
            case _ => node.children.flatMap(parse).headOption
        }
    }
}
