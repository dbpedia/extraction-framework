package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser._

/**
 * Parses a human-readable character string from a node. 
 */
object StringParser extends DataParser
{
    override def parse(node : Node) : Option[String] =
    {
        //Build text from node
        val sb = new StringBuilder()
        nodeToString(node, sb)

        //Clean text
        var text = sb.toString
        text = text.replaceAll("""<small[^>]*>(.*?)<\/small>""", "")
        text = text.replaceAll("\\<.*?\\>", "") //strip tags
        text = WikiUtil.removeWikiEmphasis(text)
        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
        text = text.trim
        
        if(text.isEmpty) return None

        return Some(text)
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
