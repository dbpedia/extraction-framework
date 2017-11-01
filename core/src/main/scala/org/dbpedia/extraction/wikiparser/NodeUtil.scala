package org.dbpedia.extraction.wikiparser

import java.util.logging.Logger

import org.dbpedia.extraction.config.transform.TemplateTransformConfig
import org.dbpedia.extraction.util.Language
import org.dbpedia.iri.{IRISyntaxException, UriUtils}

import scala.util.{Failure, Success}

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
            case TextNode(text, line, _) =>
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
            case _  if parenthesesCount <= 0 => nodes ::= child
            case _ =>
        }

        val propertyNode = PropertyNode(node.key, nodes.reverse, node.line)

        //Set link to the original AST
        propertyNode.parent = node.parent

        propertyNode
    }

    private def buildPropertyNode(
                                   text : String,
                                   line : Int,
                                   articleLanguage : Language,
                                   transformCmd : String,
                                   transformFunc : String => String,
                                   literalLang: Language = null
                                 ) : Node = {

        val textNode = TextNode(transformFunc(text), line, literalLang)

        transformCmd match
        {
            case "internal" => InternalLinkNode(WikiTitle.parse(textNode.text, articleLanguage), List(textNode), line)
            case "external" => try {
                if (UriUtils.hasKnownScheme(textNode.text))
                    ExternalLinkNode(UriUtils.createURI(textNode.text).get, List(textNode), line)
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
      var addThisNode = true

      def traverseChildNodes(children: List[Node]): Unit = {
        for (child <- children ) child match {
          case TextNode(text, line, lang) => {

            //if exclusive close tag -> set addThisNode to true
            if (text.matches("\\s*<\\/br\\s*>\\s*")) {
              addThisNode = false
              propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
              currentNodes = List[Node]()
            }

            if (addThisNode) {
              val parts = text.replaceAll("""(<br\s*exclusive=[^>]+>)([^<]*)(<\/br\s*>)""", "$2").split(fullRegex, -1)

              for (i <- 0 until parts.size) {
                if (parts.size > 1 && i < parts.size - 1) {
                  if (parts(i).trim.nonEmpty) {

                    val currentNode = buildPropertyNode(parts(i), line, inputNode.root.title.language, transformCmd, transformFunc, lang)

                    currentNodes = currentNode :: currentNodes
                  }
                  currentNodes = currentNodes.reverse
                  propertyNodes = PropertyNode(inputNode.key, currentNodes, inputNode.line) :: propertyNodes
                  currentNodes = List[Node]()
                }
                else {
                  if (parts(i).trim.nonEmpty) {

                    val currentNode = buildPropertyNode(parts(i), line, inputNode.root.title.language, transformCmd, transformFunc, lang)

                    currentNodes = currentNode :: currentNodes
                  }
                }
              }
            }
            //if exclusive start tag -> set addThisNode to true
            if (text.matches("\\s*<br\\s*exclusive=[^>]+>\\s*")) {
              //if addThisNode is true, this is the first encounter of this tag -> we reset the propertylist
              if (addThisNode) {
                propertyNodes = List[PropertyNode]()
                currentNodes = List[Node]()
              }
              addThisNode = true
            }
          }
          case ExternalLinkNode(destinationURI, children, line, destinationNodes) =>
            // In case of an external link node, transform the URI using the
            // transform function and attempt to use the result as a URI.
            UriUtils.createURI(transformFunc(destinationURI.toString)) match {
              case Success(u) => currentNodes = ExternalLinkNode(u, children, line, destinationNodes) :: currentNodes
              case Failure(f) => f match {
                // If the new URI doesn't make syntactical sense, produce
                // a warning and don't modify the original node.
                case e: IRISyntaxException => {
                  Logger.getLogger(NodeUtil.getClass.getName).warning(
                    "(while processing template '" + inputTemplateNode.title.decodedWithNamespace +
                      "', property '" + inputNode.key + "')" +
                      f" Adding prefix or suffix to '$child%s' caused an error, skipping: " + e.getMessage
                  )
                  currentNodes = child :: currentNodes
                }
                case _ => throw f
              }
            }
          case _ => currentNodes = child :: currentNodes
        }
      }

      traverseChildNodes(inputNode.children)

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
            case TextNode(text, line, _) =>
            {
                val parts = text.split(fullRegex, -1)

                for(i <- 0 until parts.size)
                {
                    if(parts.size > 1 && i < parts.size - 1)
                    {
                        if(parts(i).length > 0) currentNodes = TextNode(parts(i), line) :: currentNodes
                        currentNodes = currentNodes.reverse
                        splitNodes = currentNodes :: splitNodes
                        currentNodes = List[Node]()
                    }
                    else
                    {
                        if(parts(i).length > 0) currentNodes = TextNode(parts(i), line) :: currentNodes
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
        list.filter(x => isEmptyTextNode(x))
    }

    def isEmptyTextNode(node : Node) : Boolean = {

        if(!node.isInstanceOf[TextNode]){
            return true
        }

        ! node.asInstanceOf[TextNode].text.trim.isEmpty

    }

}
