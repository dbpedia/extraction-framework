package org.dbpedia.extraction.wikiparser

import java.util.logging.Logger
import java.net.{URI, URISyntaxException}

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.UriUtils

import scala.collection.mutable.ListBuffer

/**
 * Utility functions for working with nodes.
 */
object NodeUtil
{
    /**
     * Removes the contents of parentheses in a property node.
     */
    def removeParentheses(node : PropertyNode, openPar : Char = '(', closePar : Char = ')') : PropertyNode =
    {
        var parenthesesCount = 0
        var nodes = List[Node]()

        for(child <- node.children) child match
        {
            case TextNode(text, line) =>
            {
                val sb = new StringBuilder()

                for(c <- text)
                {
                    if(parenthesesCount <= 0)
                    {
                        if(c == openPar)
                        {
                            parenthesesCount += 1
                        }
                        else
                        {
                            sb.append(c)
                        }
                    }
                    else
                    {
                        if(c == openPar)
                        {
                            parenthesesCount += 1
                        }
                        else if(c == closePar)
                        {
                            parenthesesCount -= 1
                        }
                    }
                }

                nodes ::= TextNode(sb.toString(), line)
            }
            case _  if (parenthesesCount <= 0) => nodes ::= child
            case _ =>
        }

        val propertyNode = PropertyNode(node.key, nodes.reverse, node.line)

        //Set link to the original AST
        propertyNode.parent = node.parent

        propertyNode
    }

    private def buildPropertyNode(text : String, line : Int, language : Language, transformCmd : String, transformFunc : String => String) : Node = {

        val textNode = new TextNode(transformFunc(text), line)

        transformCmd match
        {
            case "internal" => new InternalLinkNode(WikiTitle.parse(textNode.text, language), List(textNode), line)
            case "external" => try {
                if (UriUtils.hasKnownScheme(textNode.text))
                    new ExternalLinkNode(new URI(textNode.text), List(textNode), line)
                else
                    textNode
            } catch {
                // If the provided text is not a valid URI
                case e : Exception => textNode
            }
            case _ => textNode
        }
    }

    /**
     * Utility function which splits a property node based on a regex
     * If trimResults == true, the regex is extended to eat up whitespace at beginning and end when splitting.
     */
    def splitPropertyNode(inputNode : PropertyNode, regex : String, trimResults : Boolean = false, transformCmd : String = null, transformFunc : String => String = identity) : List[PropertyNode] =
    {
        // Store a reference to the TemplateNode this PropertyNode belongs to.
        val inputTemplateNode = inputNode.parent.asInstanceOf[TemplateNode]

        var propertyNodes = List[PropertyNode]()
        var currentNodes = List[Node]()

        val fullRegex = if(trimResults) "\\s*(" + regex + ")\\s*" else regex

        for(child <- inputNode.children) child match
        {
            case TextNode(text, line) =>
            {
                val parts = text.split(fullRegex, -1)

                for(i <- 0 until parts.size)
                {
                    if(parts.size > 1 && i < parts.size - 1)
                    {
                        if(parts(i).size > 0) {

                          val currentNode = buildPropertyNode(parts(i), line, inputNode.root.title.language, transformCmd, transformFunc)

                          currentNodes = currentNode :: currentNodes
                        }
                        currentNodes = currentNodes.reverse
                        propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
                        currentNodes = List[Node]()
                    }
                    else
                    {
                        if(parts(i).size > 0) {

                          val currentNode = buildPropertyNode(parts(i), line, inputNode.root.title.language, transformCmd, transformFunc)

                          currentNodes = currentNode :: currentNodes
                        }
                    }
                }
            }
            case ExternalLinkNode(destinationURI, children, line, destinationNodes) =>
                // In case of an external link node, transform the URI using the
                // transform function and attempt to use the result as a URI.
                try {
                    currentNodes = new ExternalLinkNode(
                        new URI(transformFunc(destinationURI.toString)),
                        children, line, destinationNodes) :: currentNodes
                } catch {
                    // If the new URI doesn't make syntactical sense, produce
                    // a warning and don't modify the original node.
                    case e: URISyntaxException => {
                        Logger.getLogger(NodeUtil.getClass.getName).warning(
                            "(while processing template '" + inputTemplateNode.title.decodedWithNamespace + 
                            "', property '" + inputNode.key + "')" +
                            f" Adding prefix or suffix to '$child%s' caused an error, skipping: " + e.getMessage
                        )
                        currentNodes = child :: currentNodes
                    }
                }
            case _ => currentNodes = child :: currentNodes
        }

        //Add last property node
        currentNodes = currentNodes.reverse
        if(currentNodes.nonEmpty)
        {
            propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
        }

        propertyNodes = propertyNodes.reverse

        //Create a synthetic template node for each property node
        val templateNodes = for(propertyNode <- propertyNodes) yield TemplateNode(inputTemplateNode.title, propertyNode :: Nil, inputTemplateNode.line)

        //Set link to the original AST
        templateNodes.foreach(tnode => tnode.parent = inputTemplateNode.parent)

        propertyNodes
    }

    /**
     * Utility function which splits a text nodes based on a regex  .
     * If trimResults == true, the regex is extended to eat up whitespace at beginning and end when splitting.
     */
    def splitNodes(inputNodes : List[Node], regex : String, trimResults : Boolean = false) : List[List[Node]] =
    {
        var splitNodes = List[List[Node]]()
        var currentNodes = List[Node]()

        val fullRegex = if(trimResults) "\\s+(" + regex + ")\\s+" else regex

        for(child <- inputNodes) child match
        {
            case TextNode(text, line) =>
            {
                val parts = text.split(fullRegex, -1)

                for(i <- 0 until parts.size)
                {
                    if(parts.size > 1 && i < parts.size - 1)
                    {
                        if(parts(i).size > 0) currentNodes = new TextNode(parts(i), line) :: currentNodes
                        currentNodes = currentNodes.reverse
                        splitNodes = currentNodes :: splitNodes
                        currentNodes = List[Node]()
                    }
                    else
                    {
                        if(parts(i).size > 0) currentNodes = new TextNode(parts(i), line) :: currentNodes
                    }
                }
            }
            case _ => currentNodes = child :: currentNodes
        }

        //Add last property node
        currentNodes = currentNodes.reverse
        splitNodes = currentNodes :: splitNodes

        splitNodes.reverse
    }

    def filterEmptyTextNodes(list : List[Node]) : List[Node] = {
        return list.filter(x => isEmptyTextNode(x))
    }

    def isEmptyTextNode(node : Node) : Boolean = {

        if(!node.isInstanceOf[TextNode]){
            return true;
        }

        return ! node.asInstanceOf[TextNode].text.trim.isEmpty

    }

}
