package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.wikiparser.{Node, TextNode}
import org.dbpedia.extraction.ontology.datatypes.EnumerationDatatype

/**
 * Parses enumerations.
 */
@SoftwareAgentAnnotation(classOf[EnumerationParser], AnnotationType.Parser)
class EnumerationParser(datatype : EnumerationDatatype) extends DataParser[String]
{
    private[dataparser] override def parse(node : Node) : Option[ParseResult[String]] =
    {
        node match
        {
            case TextNode(text, line, _) => datatype.parse(text)
            case _ => node.children.flatMap(parse).headOption
        }
    }
}
