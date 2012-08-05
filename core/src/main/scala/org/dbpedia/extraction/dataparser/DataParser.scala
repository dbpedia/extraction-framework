package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{NodeUtil, PropertyNode, Node}

/**
 * Extracts data from a node in the abstract syntax tree.
 * The type of the data which is extracted depends on the specific parser e.g. The IntegerParser extracts integers.
 */
abstract class DataParser
{

    def parse( node : Node ) : Option[Any]

    /**
     * Parser dependent splitting of nodes. Default is overridden by some parsers.
     */
    val splitPropertyNodeRegex = """<br\s*\/?>|\n"""

    /**
     * (Split node and) return parse result.
     */
    def parsePropertyNode( propertyNode : PropertyNode, split : Boolean ) : List[Any] =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex).flatMap( node => parse(node).toList )
        }
        else
        {
            parse(propertyNode).toList
        }
    }

}
