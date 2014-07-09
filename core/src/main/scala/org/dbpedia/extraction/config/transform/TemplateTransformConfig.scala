package org.dbpedia.extraction.config.transform

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.TextNode
import java.net.URI
import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable

/**
 * Template transformations.
 *
 * Could be useful to analyse
 *
 * http://en.wikipedia.org/wiki/Wikipedia:Template_messages
 *
 * and collect transformations for the commonly used templates
 */
object TemplateTransformConfig {

  private def extractTextFromPropertyNode(node: Option[PropertyNode], prefix : String = "", suffix : String = "") : String = {
    node match {
      case Some(p) =>
        val propertyText = p.children.collect { case TextNode(t, _) => t }.mkString.trim
        if (propertyText.nonEmpty) prefix + propertyText + suffix else ""
      case None => ""
    }
  }

  // General functions
  private def textNode(text: String)(node: TemplateNode) : List[TextNode] = List(TextNode(text, node.line))

  /**
   * Extracts all the children of the PropertyNode's in the given TemplateNode
   */
  private def extractChildren(filter: PropertyNode => Boolean)(node: TemplateNode) : List[Node] = {
    // We have to reverse because flatMap prepends to the final list
    // while we want to keep the original order
    node.children.filter(filter).flatMap(_.children).reverse
  }

  private def identity(node: TemplateNode) : List[Node] = List(node)

  private def externalLinkNode(node: TemplateNode) : List[Node] = {

    try {

      def defaultLinkTitle(node: Node) : PropertyNode = {
        PropertyNode("link-title", List(TextNode("", node.line)), node.line)
      }

      var uri = new URI(extractTextFromPropertyNode(node.property("1")))

      // Check if this uri has a scheme. If it does not, add a default http:// scheme
      // From https://en.wikipedia.org/wiki/Template:URL:
      // The first parameter is parsed to see if it takes the form of a complete URL.
      // If it doesn't start with a URI scheme (such as "http:", "https:", or "ftp:"),
      // an "http://" prefix will be prepended to the specified generated target URL of the link.
      if (uri.getScheme == null) {
        uri = new URI("http://" + uri.toString)
      }

      List(
        ExternalLinkNode(
          uri,
          node.property("2").getOrElse(defaultLinkTitle(node)).children,
          node.line
        )
      )
    } catch {
      // In case there are problems with the URL/URI just bail and return the original node
      case _ : Throwable => List(node)
    }
  }

  private val transformMap : Map[String, Map[String, (TemplateNode) => List[Node]]] = Map(

    "en" -> Map(
      "-" -> textNode("<br />") _ ,
      "Clr" -> textNode("<br />") _ ,
      "Flatlist" -> extractChildren { p : PropertyNode => !(Set("class", "style", "indent").contains(p.key)) }  _,
      "Plainlist" -> extractChildren { p : PropertyNode => !(Set("class", "style", "indent").contains(p.key)) } _ ,
      "Hlist" ->  extractChildren { p : PropertyNode => !(Set("class", "style", "ul_style", "li_style", "indent").contains(p.key)) } _ ,
      "Unbulleted list" -> extractChildren { p : PropertyNode => !(Set("class", "style", "ul_style", "li_style", "indent").contains(p.key)) } _ ,

      "URL" -> externalLinkNode _ ,

      // http://en.wikipedia.org/wiki/Template:ICD10
      // See https://github.com/dbpedia/extraction-framework/issues/40
      "ICD10" ->
        ((node: TemplateNode) =>
          List(
            new TextNode(extractTextFromPropertyNode(node.property("1")) +
                         extractTextFromPropertyNode(node.property("2")) +
                         extractTextFromPropertyNode(node.property("3"), "."), node.line)
          )
        )
      ,
      // http://en.wikipedia.org/wiki/Template:ICD9
      // See https://github.com/dbpedia/extraction-framework/issues/40
      "ICD9" -> extractChildren { p : PropertyNode => p.key == "1" } _
    )
  )

  def apply(node: TemplateNode, lang: Language) : List[Node] = {

     val mapKey = if (transformMap.contains(lang.wikiCode)) lang.wikiCode else "en"

     transformMap(mapKey).get(node.title.decoded).getOrElse(identity _)(node)
  }
}
