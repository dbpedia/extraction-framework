package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language

import scala.Predef._
import org.dbpedia.extraction.wikiparser.PropertyNode
import org.dbpedia.extraction.wikiparser.TextNode
import org.dbpedia.extraction.wikiparser.TemplateNode
import org.dbpedia.extraction.wikiparser.InternalLinkNode

import scala.Some
import org.dbpedia.extraction.config.dataparser.DataParserConfig

import scala.language.reflectiveCalls

/**
 * Parses links to other instances.
 */

@SoftwareAgentAnnotation(classOf[ObjectParser], AnnotationType.Parser)
class ObjectParser( context : { def language : Language }, val strict : Boolean = false) extends DataParser[String]
{
    private val flagTemplateParser = new FlagTemplateParser(context)

    private val language = context.language.wikiCode

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexObject.contains(language)) DataParserConfig.splitPropertyNodeRegexObject(language)
                                          else DataParserConfig.splitPropertyNodeRegexObject("en")
    // the Template {{·}} would also be nice, but is not that easy as the regex splits

    override def parsePropertyNode( propertyNode : PropertyNode, split : Boolean, transformCmd : String = null, transformFunc : String => String = identity ): List[ParseResult[_]] =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex, trimResults = true, transformCmd = transformCmd, transformFunc = transformFunc).flatMap( node => super.parseWithProvenance(node).toList )
        }
        else
        {
            super.parseWithProvenance(propertyNode).toList
        }
    }

    private[dataparser] override def parse(node : Node) : Option[ParseResult[String]] =
    {
        val pageNode = node.root

        if (! strict)
        {
            for (child <- node :: node.children) child match
            {
                //ordinary links
                case InternalLinkNode(destination, _, _, _) if destination.namespace == Namespace.Main =>
                {
                    return Some(ParseResult(getUri(destination, pageNode)))
                }

                case ExternalLinkNode(destination, _, _, _) =>
                {
                    return Some(ParseResult(destination.toString))
                }

                //creating links if the same string is a link on this page
                case TextNode(text, _, _) => getAdditionalWikiTitle(text, pageNode) match
                {
                    case Some(destination) if destination.namespace == Namespace.Main =>
                    {
                        return Some(ParseResult(getUri(destination, pageNode)))
                    }
                    case _ =>
                }

                //resolve templates to create links
                case templateNode : TemplateNode if node.children.length == 1 => resolveTemplate(templateNode) match
                {
                    case Some(destination) =>  // should always be Main namespace
                    {
                        return Some(ParseResult(context.language.resourceUri.append(destination.value.decodedWithNamespace)))
                    }
                    case None =>
                }

                case _ =>
            }
        }
        else
        {
            node match
            {
                case InternalLinkNode(destination, _, _, _) => return Some(ParseResult(getUri(destination, pageNode)))
                case _ =>
                {
                    node.children match
                    {
                        case InternalLinkNode(destination, _, _, _) :: Nil => return Some(ParseResult(getUri(destination, pageNode)))
                        case InternalLinkNode(destination, _, _, _) :: TextNode(text, _, _) :: Nil if text.trim.isEmpty => return Some(ParseResult(getUri(destination, pageNode)))
                        case TextNode(text, _, _) :: InternalLinkNode(destination, _, _, _) :: Nil if text.trim.isEmpty => return Some(ParseResult(getUri(destination, pageNode)))
                        case TextNode(text1, _, _) ::InternalLinkNode(destination, _, _, _) :: TextNode(text2, _, _) :: Nil if text1.trim.isEmpty && text2.trim.isEmpty => return Some(ParseResult(getUri(destination, pageNode)))
                        case _ => return None
                    }
                }
            }
        }
        None
    }

    /**
     * Searches on the wiki page for a link with the same name as surfaceForm and returns the destination if one is found.
     */
    private def getAdditionalWikiTitle(surfaceForm : String, pageNode : PageNode) : Option[WikiTitle] =
    {
        surfaceForm.trim.toLowerCase(context.language.locale) match
        {
            case "" => None
            case sf: String => getTitleForSurfaceForm(sf, pageNode)
        }
    }

    private def getTitleForSurfaceForm(surfaceForm : String, node : Node) : Option[WikiTitle] =
    {
        node match
        {
            case linkNode : InternalLinkNode =>
            {
                // TODO: Here we match the link label. Should we also match the link target?
                val linkText = linkNode.children.collect{case TextNode(text, _, _) => text}.mkString("")
                if(linkText.toLowerCase(context.language.locale) == surfaceForm)
                {
                    return Some(linkNode.destination)
                }
            }
            case _ =>
        }

        for(child <- node.children)
        {
            getTitleForSurfaceForm(surfaceForm, child) match
            {
                case Some(destination) => return Some(destination)
                case None =>
            }
        }

        None
    }

    private def resolveTemplate(templateNode : TemplateNode) : Option[ParseResult[WikiTitle]] =
    {
        flagTemplateParser.parse(templateNode).foreach(destination => return Some(destination))
        None
    }

    private def getUri(destination : WikiTitle, pageNode : PageNode) : String =
    {
        // prepend page title to the URI if the destination links to a subsection on this page, e.g. starring = #Cast
        // FIXME: how do we want to treat fragment URIs? Currently, "#" is percent-encoded as "%23"
        val uriSuffix = if(destination.decoded startsWith "#") pageNode.title.decodedWithNamespace + destination.decoded
                         else destination.decodedWithNamespace
        // TODO this is not the final solution:
        // parsing the list of items in the subsection would be better, but the signature of parse would have to change to return a List

        destination.language.resourceUri.append(uriSuffix)
    }
}
