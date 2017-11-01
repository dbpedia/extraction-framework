package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.iri.IRI

/**
 * Parses external links.
 */
@SoftwareAgentAnnotation(classOf[LinkParser], AnnotationType.Parser)
class LinkParser(val strict : Boolean = false) extends DataParser[IRI]
{
    override val splitPropertyNodeRegex: String = DataParserConfig.splitPropertyNodeRegexLink("en")

    private[dataparser] override def parse(node : Node) : Option[ParseResult[IRI]] =
    {
        if (!strict)
        {
            val list = collectExternalLinks(node)
            list.foreach(link => {
                try
                {
                    return Some(ParseResult(link.destination))
                }
                catch
                {
                    case e : Exception =>
                }
            })
        }
        else
        {
            node match
            {
                case ExternalLinkNode(destination, _, _, _) => return Some(ParseResult(destination))
                case _ =>
                {
                    node.children match
                    {
                        case ExternalLinkNode(destination, _, _, _) :: Nil => return Some(ParseResult(destination))
                        case ExternalLinkNode(destination, _, _, _) :: TextNode(text, _, _) :: Nil if text.trim.isEmpty => return Some(ParseResult(destination))
                        case TextNode(text, _, _) :: ExternalLinkNode(destination, _, _, _) :: Nil if text.trim.isEmpty => return Some(ParseResult(destination))
                        case TextNode(text1, _, _) :: ExternalLinkNode(destination, _, _, _) :: TextNode(text2, _, _) :: Nil if text1.trim.isEmpty && text2.trim.isEmpty => return Some(ParseResult(destination))
                        case _ => return None
                    }
                }
            }
        }
        None
    }

    private def collectExternalLinks(node : Node) : List[ExternalLinkNode] =
    {
        node match
        {
            case linkNode : ExternalLinkNode => List(linkNode)
            case _ => node.children.flatMap(collectExternalLinks)
        }
    }
}