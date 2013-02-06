package org.dbpedia.extraction.wikiparser

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

    /**
     * Utility function which splits a property node based on a regex
     * If trimResults == true, the regex is extended to eat up whitespace at beginning and end when splitting.
     */
    def splitPropertyNode(inputNode : PropertyNode, regex : String, trimResults : Boolean = false) : List[PropertyNode] =
    {
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
                        if(parts(i).size > 0) currentNodes = new TextNode(parts(i), line) :: currentNodes
                        currentNodes = currentNodes.reverse
                        propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
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
        if(currentNodes.nonEmpty)
        {
            propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
        }

        propertyNodes = propertyNodes.reverse

        //Create a synthetic template node for each property node
        val inputTemplateNode = inputNode.parent.asInstanceOf[TemplateNode]
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
}