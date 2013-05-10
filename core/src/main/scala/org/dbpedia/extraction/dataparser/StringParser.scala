package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser._

/**
 * Parses a human-readable character string from a node. 
 */
object StringParser extends DataParser
{
    private val smallTagRegex = """<small[^>]*>(.*?)<\/small>""".r
    private val tagRegex = """\<.*?\>""".r
    
    override val splitPropertyNodeRegex = """<br\s*\/?>"""
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
        //Build text from node
        val sb = new StringBuilder()
        nodeToString(node, sb)

        //Clean text
        var text = sb.toString()
        text = smallTagRegex.replaceAllIn(text, "$1")
        text = tagRegex.replaceAllIn(text, "") //strip tags
        text = WikiUtil.removeWikiEmphasis(text)
        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
        text = text.trim
        
        if(text.isEmpty)
        {
            None
        }
        else
        {
            Some(text)
        }
    }
    
    def stringParse(text : String) : String = {
        //Build text from node
        val sb = new StringBuilder()

        //Clean text
        var text = sb.toString()
        text = smallTagRegex.replaceAllIn(text, "$1")
        text = tagRegex.replaceAllIn(text, "") //strip tags
        text = WikiUtil.removeWikiEmphasis(text)
        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
        text = text.trim
        
        if(text.isEmpty)
        {
            ""
        }
        else
        {
            text
        }
    }
  
    private def nodeToString(node : Node, sb : StringBuilder)
    {
        node match
        {
            case TextNode(text, _) => sb.append(text)
            case _ : TemplateNode | _ : TableNode => //ignore
            case _ => node.children.foreach(child => nodeToString(child, sb))
        }
    }
}
