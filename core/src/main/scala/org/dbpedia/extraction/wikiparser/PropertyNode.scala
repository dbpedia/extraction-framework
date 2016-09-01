package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.util.StringUtils

/**
 * Represents a template property.
 *
 * @param key The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
case class PropertyNode(key : String, override val children : List[Node], override val line : Int) extends Node(children, line)
{
    def toWikiText =
    {
      // named arguments prefix name and "=", positional arguments use only the value
      val prefix = 
        try { key.toInt; "" }
        catch { case e : NumberFormatException => key+"=" }
        
      prefix+children.map(_.toWikiText).mkString
    }
    
    // properties are skipped for plain text
    def toPlainText = ""

    def propertyNodeValueToPalinText = children.map(_.toPlainText).mkString

    override def sourceUri : String =
    {

      val sb = new StringBuilder

      sb append(super.sourceUri)

      if (this.parent != null && this.parent.isInstanceOf[TemplateNode]) {
        sb append "&template="  append this.parent.asInstanceOf[TemplateNode].title.encoded
      }

      sb append  "&property=" append StringUtils.escape(this.key, Node.fragmentEscapes )

      sb.toString
    }

  override def equals(obj: scala.Any) = obj match {

    case otherPropertyNode : PropertyNode => ( otherPropertyNode.key == key //&&  otherPropertyNode.line == line
      && NodeUtil.filterEmptyTextNodes(otherPropertyNode.children) == NodeUtil.filterEmptyTextNodes(children))
    case _ => false

  }
}