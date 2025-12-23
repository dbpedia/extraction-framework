package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{Node, NodeUtil, PropertyNode}
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.ontology.datatypes.{Datatype, UnitDatatype}
import org.dbpedia.extraction.util.Language

/**
 * Extracts data from a node in the abstract syntax tree.
 * The type of the data which is extracted depends on the specific parser e.g. The IntegerParser extracts integers.
 */
abstract class DataParser extends java.io.Serializable
{

    def parse( node : Node ) : Option[ParseResult[_]]

    /**
     * Parser dependent splitting of nodes. Default is overridden by some parsers.
     */
    val splitPropertyNodeRegex: String = DataParserConfig.splitPropertyNodeRegex("en")

    /**
     * (Split node and) return parse result.
     */
    def parsePropertyNode( propertyNode : PropertyNode, split : Boolean, transformCmd : String = null , transformFunc : String => String = identity ) : List[ParseResult[_]] =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex, transformCmd = transformCmd, transformFunc = transformFunc).flatMap( node => parse(node).toList )
        }
        else
        {
            parse(propertyNode).toList
        }
    }

}

case class ParseResult[T](value: T, lang: Option[Language] = None, unit: Option[Datatype] = None)