package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.SoftwareAgentAnnotation
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.config.provenance.ParserRecord
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Node, NodeUtil, PropertyNode}

/**
 * Extracts data from a node in the abstract syntax tree.
 * The type of the data which is extracted depends on the specific parser e.g. The IntegerParser extracts integers.
 */
abstract class DataParser[T]
{

    private[dataparser] def parse( node : Node ) : Option[ParseResult[T]]

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
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex, transformCmd = transformCmd, transformFunc = transformFunc)
              .flatMap( node => parseWithProvenance(node).toList )
        }
        else
        {
            parseWithProvenance(propertyNode).toList
        }
    }

  /**
    * Executes the parse function and appends a ParseRecord for provenance
    * @param node - the node to be parsed
    * @return - parse result
    */
    def parseWithProvenance( node : Node ) : Option[ParseResult[T]] = {
      this.parse(node) match{
        case Some(pr) => pr.provenance match{
          case Some(_) => Some(pr)
          case None =>
            val annotation = SoftwareAgentAnnotation.getAnnotationIri(this.getClass)
            val rec = ParserRecord(
              uri = annotation.toString,
              wikiText = node.root.getOriginWikiText(node.line),
              transformed = node.toWikiText,
              resultValue = pr.value.toString
            )
            Some(ParseResult(pr.value, pr.lang, pr.unit, Some(rec)))
        }
        case None => None
      }
    }
}

/**
  * Warpper object for parse results
  * @param value - the parse result
  * @param lang - optional language parameter for language tags
  * @param unit - carries a unit if property was attributed with it
  * @param provenance - an provenance record covering only information pertaining to the Parser (needs to be extended with info from Extractor)
  * @tparam T - Type of the value
  */
case class ParseResult[T](value: T, lang: Option[Language] = None, unit: Option[Datatype] = None, provenance: Option[ParserRecord] = None)