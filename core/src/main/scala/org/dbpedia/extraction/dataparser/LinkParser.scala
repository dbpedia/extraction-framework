package org.dbpedia.extraction.dataparser

import java.net.URI
import org.dbpedia.extraction.wikiparser._

/**
 * Parses external links.
 */
class LinkParser(val strict : Boolean = false) extends DataParser
{
    private val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or |/|;|,"""  //TODO this split regex might not be complete

    override def parse(node : Node) : Option[URI] =
    {
        if (!strict)
        {
            val list = collectExternalLinks(node)
            list.foreach(link => {
                try
                {
                    return Some(link.destination)
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
                case ExternalLinkNode(destination, _, _, _) => return Some(destination)
                case _ =>
                {
                    node.children match
                    {
                        case ExternalLinkNode(destination, _, _, _) :: Nil => return Some(destination)
                        case ExternalLinkNode(destination, _, _, _) :: TextNode(text, _) :: Nil if text.trim.isEmpty => return Some(destination)
                        case TextNode(text, _) :: ExternalLinkNode(destination, _, _, _) :: Nil if text.trim.isEmpty => return Some(destination)
                        case TextNode(text1, _) :: ExternalLinkNode(destination, _, _, _) :: TextNode(text2, _) :: Nil if (text1.trim.isEmpty && text2.trim.isEmpty) => return Some(destination)
                        case _ => return None
                    }
                }
            }
        }
        None
    }

    override def splitPropertyNode(propertyNode : PropertyNode) : List[Node] =
    {
        NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex)
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