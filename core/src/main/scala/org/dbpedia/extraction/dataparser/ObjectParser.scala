package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language

/**
 * Parses links to other instances.
 */

class ObjectParser( context : { def language : Language }, val strict : Boolean = false) extends DataParser
{
    private val flagTemplateParser = new FlagTemplateParser(context)

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or | in |;|,|/"""
    // the Template {{·}} would also be nice, but is not that easy as the regex splits

    override def parsePropertyNode( propertyNode : PropertyNode, split : Boolean ) =
    {
        if(split)
        {
	    println("avant : " + propertyNode)
	    println()
	    val cleanPropertyNodes = textNodeToInternalLinkNode(propertyNode)
	    println("après : " + cleanPropertyNodes)
	    println()
            NodeUtil.splitPropertyNode(cleanPropertyNodes, splitPropertyNodeRegex, trimResults = true).flatMap(node => parse(node).toList)
        }
        else
        {
            parse(propertyNode).toList
        }
    }

    override def parse(node : Node) : Option[String] =
    {
        val pageNode = node.root
        
        if (! strict)
        {
            for (child <- node :: node.children) child match
            {
                //ordinary links
                case InternalLinkNode(destination, _, _, _) if destination.namespace == Namespace.Main =>
                {
                    return Some(getUri(destination, pageNode))
                }

                //creating links if the same string is a link on this page
                case TextNode(text, _) => getAdditionalWikiTitle(text, pageNode) match
                {
                    case Some(destination) if destination.namespace == Namespace.Main =>
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
                        return Some(context.language.resourceUri.append(destination.decodedWithNamespace))
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
                val linkText = linkNode.children.collect{case TextNode(text, _) => text}.mkString("")
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

    private def resolveTemplate(templateNode : TemplateNode) : Option[WikiTitle] =
    {
        flagTemplateParser.parse(templateNode).foreach(destination => return Some(destination))
        None
    }

    private def getUri(destination : WikiTitle, pageNode : PageNode) : String =
    {
        // prepend page title to the URI if the destination links to a subsection on this page, e.g. starring = #Cast
        // FIXME: how do we want to treat fragment URIs? Currently, "#" is percent-encoded as "%23"
        val uriSuffix = if(destination.decoded startsWith "#") (pageNode.title.decodedWithNamespace + destination.decoded)
                         else destination.decodedWithNamespace
        // TODO this is not the final solution:
        // parsing the list of items in the subsection would be better, but the signature of parse would have to change to return a List

        destination.language.resourceUri.append(uriSuffix)
    }
    
    private def textNodeToInternalLinkNode (propertyNode: PropertyNode) : PropertyNode = {
        var currentNodes = List[Node]()
        var cleanedPropertyNode = new PropertyNode("", List[Node](), 0)
        
        for(property <- propertyNode.children) property match {
            case TextNode(text, line) => {
                if (!(text matches """<br\s*\/?>""") && text != "") {
		    var newString = StringParser.stringParse(text)
		    if (newString != "") {
                        currentNodes = currentNodes ::: List[Node](new InternalLinkNode(new WikiTitle(newString, Namespace.Main, context.language), List[Node](), line, List[Node](new TextNode(newString, line))))
		    }
                }
		else {
		    currentNodes = currentNodes ::: List[Node](property)
		}
            }
            case _ => currentNodes = currentNodes ::: List[Node](property)
        }
        
        cleanedPropertyNode = new PropertyNode(propertyNode.key, currentNodes, propertyNode.line)
        cleanedPropertyNode.parent = propertyNode.parent
        
        return cleanedPropertyNode
    }
}
