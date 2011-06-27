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

    private val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or | in |/|;|,"""  //TODO this split regex might not be complete
    // the Template {{·}} would also be nice, but is not that easy as the regex splits

    override def parse(node : Node) : Option[String] =
    {
        if (!strict)
        {
            val pageNode = node.root
            for (child <- node :: node.children) child match
            {
                //ordinary links
                case InternalLinkNode(destination, _, _, _) => return Some(getUri(destination))

                //creating links if the same string is a link on this page
                case TextNode(text, _) => getAdditionalWikiTitle(text.trim.capitalize, pageNode) match
                {
                    case Some(destination) => return Some(getUri(destination))
                    case None =>
                }

                //resolve templates to create links
                case templateNode : TemplateNode if(node.children.length == 1) => resolveTemplate(templateNode) match
                {
                    case Some(destination) => return Some(OntologyNamespaces.getResource(destination.encoded, extractionContext.language.wikiCode))
                    case None =>
                }

                case _ =>
            }
        }
        else
        {
            node match
            {
                case InternalLinkNode(destination, _, _, _) => return Some(getUri(destination))
                case _ =>
                {
                    node.children match
                    {
                        case InternalLinkNode(destination, _, _, _) :: Nil => return Some(getUri(destination))
                        case InternalLinkNode(destination, _, _, _) :: TextNode(text, _) :: Nil if text.trim.isEmpty => return Some(getUri(destination))
                        case TextNode(text, _) :: InternalLinkNode(destination, _, _, _) :: Nil if text.trim.isEmpty => return Some(getUri(destination))
                        case TextNode(text1, _) ::InternalLinkNode(destination, _, _, _) :: TextNode(text2, _) :: Nil if (text1.trim.isEmpty && text2.trim.isEmpty) => return Some(getUri(destination))
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
        flagTemplateParser.getDestination(templateNode).foreach(destination => return Some(destination))
        None
    }

    private def getUri(destination : WikiTitle) : String =
    {
        //OntologyNamespaces.getUri(destination.encoded, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE)
        OntologyNamespaces.getResource(destination.encoded, destination.language.wikiCode)
    }
}
