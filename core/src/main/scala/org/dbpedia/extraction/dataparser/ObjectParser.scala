package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.util.Language

/**
 * Parses links to other instances.
 */

class ObjectParser( extractionContext : { def language : Language }, val strict : Boolean = false) extends DataParser
{
    private val flagTemplateParser = new FlagTemplateParser(extractionContext)

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or | in |/|;|,"""
    // the Template {{·}} would also be nice, but is not that easy as the regex splits

    override def parsePropertyNode( propertyNode : PropertyNode, split : Boolean ) =
    {
        if(split)
        {
            NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex, trimResults = true).flatMap( node => parse(node).toList )
        }
        else
        {
            parse(propertyNode).toList
        }
    }

    override def parse(node : Node) : Option[String] =
    {
        val pageNode = node.root

        if (!strict)
        {
            for (child <- node :: node.children) child match
            {
                //ordinary links
                case InternalLinkNode(destination, _, _, _) if destination.namespace == WikiTitle.Namespace.Main =>
                {
                    return Some(getUri(destination, pageNode))
                }

                //creating links if the same string is a link on this page
                case TextNode(text, _) => getAdditionalWikiTitle(text, pageNode) match
                {
                    case Some(destination) if destination.namespace == WikiTitle.Namespace.Main =>
                    {
                        return Some(getUri(destination, pageNode))
                    }
                    case _ =>
                }

                //resolve templates to create links
                case templateNode : TemplateNode if(node.children.length == 1) => resolveTemplate(templateNode) match
                {
                    case Some(destination) =>  // should always be Main namespace
                    {
                        return Some(OntologyNamespaces.getResource(destination.encodedWithNamespace, extractionContext.language))
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
                case InternalLinkNode(destination, _, _, _) => return Some(getUri(destination, pageNode))
                case _ =>
                {
                    node.children match
                    {
                        case InternalLinkNode(destination, _, _, _) :: Nil => return Some(getUri(destination, pageNode))
                        case InternalLinkNode(destination, _, _, _) :: TextNode(text, _) :: Nil if text.trim.isEmpty => return Some(getUri(destination, pageNode))
                        case TextNode(text, _) :: InternalLinkNode(destination, _, _, _) :: Nil if text.trim.isEmpty => return Some(getUri(destination, pageNode))
                        case TextNode(text1, _) ::InternalLinkNode(destination, _, _, _) :: TextNode(text2, _) :: Nil if (text1.trim.isEmpty && text2.trim.isEmpty) => return Some(getUri(destination, pageNode))
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
        surfaceForm.trim.capitalize match
        {
            case "" => None
            case sf : String => getTitleForSurfaceForm(sf, pageNode)
        }
    }

    private def getTitleForSurfaceForm(surfaceForm : String, node : Node) : Option[WikiTitle] =
    {
        node match
        {
            case linkNode : InternalLinkNode =>
            {
                val linkText = linkNode.children.collect{case TextNode(text, _) => text}.mkString("")
                if(linkText.capitalize == surfaceForm)
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

    private def resolveTemplate(templateNode : TemplateNode) : Option[WikiTitle] =
    {
        flagTemplateParser.parse(templateNode).foreach(destination => return Some(destination))
        None
    }

    private def getUri(destination : WikiTitle, pageNode : PageNode) : String =
    {
        // prepend page title to the URI if the destination links to a subsection on this page, e.g. starring = #Cast
         val uriSuffix = if(destination.decoded startsWith "#") (pageNode.title.encodedWithNamespace + destination.encoded)
                         else destination.encodedWithNamespace
        // TODO this is not the final solution:
        // parsing the list of items in the subsection would be better, but the signature of parse would have to change to return a List

        OntologyNamespaces.getResource(uriSuffix, destination.language)
    }
}
