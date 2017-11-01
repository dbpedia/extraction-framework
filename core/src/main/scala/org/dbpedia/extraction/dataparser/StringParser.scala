package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser._

import scala.util.matching.Regex.Match

/**
 * Parses a human-readable character string from a node. 
 */
@SoftwareAgentAnnotation(classOf[StringParser], AnnotationType.Parser)
class StringParser extends DataParser[String]
{
    private val smallTagRegex = """<small[^>]*>\(?(.*?)\)?<\/small>""".r
    private val tagRegex = """\<.*?\>""".r

    private[dataparser] override def parse(node : Node) : Option[ParseResult[String]] =
    {
        //Build text from node
        node match {
            case PropertyNode(_, children, line) if children.size == 1 => children.head match {
                case TextNode(t, _, lang) =>
                  return Some(ParseResult(postProcess(t), Option(lang)))
                case _ =>
            }
            case _ =>
        }

        val sb = new StringBuilder()
        nodeToString(node, sb)

        //Clean text
        val text = postProcess(sb.toString)
        
        if(text.isEmpty)
        {
            None
        }
        else
        {
            Some(ParseResult(text))
        }
    }

    //FIXME this cleaning takes a lot of computing power
    // Replace text in <small></small> tags with an "equivalent" string representation
    // Simply extracting the content puts this data at the same level as other text appearing
    // in the node, which might not be the editor's semantics
    private def postProcess(input: String): String ={
        var text: String = input
        text = smallTagRegex.replaceAllIn(text, (m: Match) => if (m.group(1).nonEmpty) "($1)" else "")
        text = tagRegex.replaceAllIn(text, "") //strip tags
        text = WikiUtil.removeWikiEmphasis(text)
        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
        text = text.replaceAll("\\s+", " ")
        text.trim
    }
  
    private def nodeToString(node : Node, sb : StringBuilder)
    {
        node match
        {
            case TextNode(text, _, _) => sb.append(" " + text)
            case _ : TemplateNode | _ : TableNode => //ignore
            case _ => node.children.foreach(child => nodeToString(child, sb))
        }
    }
}


object StringParser extends DataParser[String]{
    private val parser = new StringParser()
    override private[dataparser] def parse(node: Node) = parser.parse(node)
}