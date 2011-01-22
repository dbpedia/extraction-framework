package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{NodeUtil, PropertyNode, Node}

/**
 * Extracts data from a node in the abstract syntax tree.
 * The type of the data which is extracted depends on the specific parser e.g. The IntegerParser extracts integers.
 */
trait DataParser
{
    /**
     * (Split node and) return parse result.
     */
    def parsePropertyNode( propertyNode : PropertyNode, split : Boolean ) : List[Any] =
    {
        if(split)
        {
            splitPropertyNode(propertyNode).flatMap( node => parse(node).toList )
        }
        else
        {
            parse(propertyNode).toList
        }
    }

    /**
     * Parser dependent splitting of nodes. Default implementation is overwritten by some parsers.
     */
    def splitPropertyNode( propertyNode : PropertyNode ) : List[Node] =
    {
        NodeUtil.splitPropertyNode(propertyNode, """<br\s*\/?>|\n""")
    }

    def parse( node : Node ) : Option[Any]

}
