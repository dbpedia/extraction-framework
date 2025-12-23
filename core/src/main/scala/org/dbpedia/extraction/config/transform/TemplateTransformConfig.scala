package org.dbpedia.extraction.config.transform

import java.util.regex.Pattern

import com.fasterxml.jackson.databind.node.ArrayNode
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.{JsonConfig, Language, WikiUtil}
import org.dbpedia.extraction.wikiparser.TextNode
import org.dbpedia.iri.{IRI, UriUtils}

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success}
import scala.collection.convert.decorateAsScala._
import scala.language.postfixOps


/**
 * Template transformations.
 *
 * Could be useful to analyse
 *
 * http://en.wikipedia.org/wiki/Wikipedia:Template_messages
 *
 * and collect transformations for the commonly used templates
  *
  * TODO!!! We have to update the template name -> transform mappings!!!
 */
object TemplateTransformConfig {

  private val textNodeParamsRegex = "\\$\\(([0-9]+)\\|([^\\|]*)\\|([^\\)]*)\\)"r
  private var transformerMap: Map[Language, Map[String, (TemplateNode, Language) => List[Node]]] = _

  val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("templatetransform.json"))
  transformerMap = (for (lang <- mappingsFile.keys() if lang != "comment") yield {
    Language(lang) -> (for(trans <- mappingsFile.getMap(lang)) yield{
      val templateNames = if(trans._1.contains("$(lang)"))
        Language.map.keys.map(x => trans._1.replaceAll("\\$\\(lang\\)", x))
      else
        List(trans._1)

      for(template <- templateNames) yield {
        val keys = if (trans._2.get("keys") != null) trans._2.get("keys").asInstanceOf[ArrayNode].iterator().asScala.toList.map(_.asText()) else null
        val contains = if (trans._2.get("whileList") != null) trans._2.get("whileList").asBoolean() else false
        val replace = if (trans._2.get("replace") != null) trans._2.get("replace").asText() else null
        val additionalSplitString = if (trans._2.get("additionalSplitString") != null) trans._2.get("additionalSplitString").asText() else null
        val templateSynonyms = template.split("\\|").map(WikiUtil.cleanSpace).map(_.capitalize).distinct
        templateSynonyms.map(templateSynonym => templateSynonym -> (trans._2.get("transformer").asText() match {
          case "externalLinkNode" => externalLinkNode _
          case "unwrapTemplates" => unwrapTemplates { p => if (contains) keys.contains(p.key) else !keys.contains(p.key) } _
          case "extractChildren" => extractAndReplace(p => if (contains) keys.contains(p.key) else !keys.contains(p.key), replace, additionalSplitString) _
          case "getLangText" => getLangText(p => if (contains) keys.contains(p.key) else !keys.contains(p.key), templateSynonym.substring(5)) _
          case "textNode" => textNode {
            Option(replace) match {
              case Some(s) => s
              case None => ""
            }
          } _
        }))
      }
    }).flatten.flatten.toMap
  }).toMap

  private def extractTextFromPropertyNode(node: Option[PropertyNode], prefix : String = "", suffix : String = "") : String = {
    node match {
      case Some(p) =>
        val propertyText = p.children.collect {
          case TextNode(t, _, _) => t
          case ExternalLinkNode(iri, children, line, destinationNodes) => iri
          case InternalLinkNode(destination, children , line, destinationNodes) => destination.decoded
        }.mkString.trim
        if (propertyText.nonEmpty) prefix + propertyText + suffix else ""
      case None => ""
    }
  }

  private def extractFirstExternalLinkNode(node: Option[PropertyNode]) : Option[ExternalLinkNode] = {
    node
      .flatMap(_.children
        .filter(c => c.isInstanceOf[ExternalLinkNode])
        .map(_.asInstanceOf[ExternalLinkNode])
        .headOption
      )
  }

  // General functions
  private def textNode(text: String)(node: TemplateNode, lang:Language) : List[TextNode] = {
    val resolved = textNodeParamsRegex.replaceAllIn(text, repl =>
      extractTextFromPropertyNode(node.property(repl.group(1))))
    List(TextNode(resolved, node.line))
  }

  // General functions
  private def getLangText(filter: PropertyNode => Boolean, stringLang: String)(node: TemplateNode, lang:Language) : List[Node] = {
    val children = extractChildren(filter, split = false)(node, lang).flatten
    val text = children.headOption match{
      case Some(t) => t.toPlainText
      case None => ""
    }

    textNode("<br />")(node, lang) :::
    List(TextNode(text, node.line, Language(stringLang))) :::
    textNode("<br />")(node, lang)
  }

  /**
   * Extracts all the children of the PropertyNode's in the given TemplateNode
   */
  private def extractChildren(filter: PropertyNode => Boolean, split : Boolean = true, additionalSplitString: String = null)(node: TemplateNode, lang:Language) : List[List[Node]] = {
    // We have to reverse because flatMap prepends to the final list
    // while we want to keep the original order
    val children : List[Node] = node.children.filter(filter)

    val splitChildren = new ArrayBuffer[Node]()
    val splitTxt = if (split) "<br />" else " "
    for ( c <- children) {
      if(split)
        splitChildren += TextNode(splitTxt, c.line)
      splitChildren += c
    }
    if (split && splitChildren.nonEmpty) {
      splitChildren += TextNode(splitTxt, 0)
    }
    val finalList = splitChildren.map(x => {
      if (x.children.nonEmpty)
        x.children.reverse
      else
        List(x)
    }).toList

    if (split && additionalSplitString != null) {
     finalList.map(list => {
      list.map(item => {
        item match {
          case textNode: TextNode => TextNode(textNode.text.replaceAll(Pattern.quote(additionalSplitString), splitTxt), textNode.line)
          case _: Node => item
        }
      })
     }).toList
    } else {
      finalList
    }
  }

  private def extractAndReplace(filter: PropertyNode => Boolean, replace: String = null, additionalSplitString: String = null)(node: TemplateNode, lang:Language) : List[Node] = {
    val children = extractChildren(filter, replace == null, additionalSplitString)(node, lang)
    if(replace != null) {
      //in this case we replace the position variables of a given replace-string with the children of the same number
      //also we frame the results in line breaks to create multiple triples
      textNode("<br />")(node, lang) :::
        textNode(textNodeParamsRegex.replaceAllIn(replace, x => {
          val ind = x.group(1).toInt - 1
          if (children.size > ind)
          // prefix  +  main replacement         +  postfix
            x.group(2) + children(ind).map(_.toPlainText).map(_.trim).mkString(" ") + x.group(3)
          else
            ""
        }))(node, lang) :::
        textNode("<br />")(node, lang)
    }
    else
      children.flatten
  }

  private def identity(node: TemplateNode, lang:Language) : List[Node] = List(node)

  private def externalLinkNode(node: TemplateNode, lang:Language) : List[Node] = {

      def defaultLinkTitle(node: Node) : PropertyNode = {
        PropertyNode("link-title", List(TextNode("", node.line)), node.line)
      }

      // Check if this uri has a scheme. If it does not, add a default http:// scheme
      // From https://en.wikipedia.org/wiki/Template:URL:
      // The first parameter is parsed to see if it takes the form of a complete URL.
      // If it doesn't start with a URI scheme (such as "http:", "https:", or "ftp:"),
      // an "http://" prefix will be prepended to the specified generated target URL of the link.
    val uri: Option[IRI] = extractFirstExternalLinkNode(node.property("1"))
        .map(_.destination)

    uri match{
        case Some(u) => {
          val iri = if (u.getScheme == null)
            UriUtils.createURI("http://" + u.toString).get
          else u

          List(
            ExternalLinkNode(
              iri,
              node.property("2").getOrElse(defaultLinkTitle(node)).children,
              node.line
            )
          )
        }
        // In case there are problems with the URL/URI just bail and return the original node
        case None => List(node)
      }
  }

  /*
   * Unwraps templates that contain other templates as arguments. Please ensure
   * that the filter removes ALL children that may not be templates.
   *
   * Since we unwrap the template, the original template node is kept at the head
   * of the resulting list.
   *
   * Examples of such templates include:
   *    - https://commons.wikimedia.org/wiki/Template:Self
   *    - https://commons.wikimedia.org/wiki/Template:PD-art
   */
  private def unwrapTemplates(filter: PropertyNode => Boolean)(node: TemplateNode, lang:Language):List[Node] =
      node :: toTemplateNodes(extractChildren(filter)(node, lang).flatten, lang)

  /**
   * Stores the Template namespace to avoid querying Namespace.template in a loop.
   */
  private val templateNamespace = Namespace.Template
  
  /**
   * Converts TextNodes in a List[Node] to TemplateNodes. Used by unwrapTemplates.
   * Note that there is no way to test whether this template exists at this
   * stage: EVERY TextNode will be converted into a TemplateNode, which may or
   * may not point to an actual template.
   *
   * TODO: Support entire templates embedded in others, such as this example
   * from https://commons.wikimedia.org/wiki/Template:Licensed-FOP
   *    - {{Licensed-FOP|Spain| {{self|cc-by-sa-3.0|GFDL}} }} 
   *
   * @param nodes The nodes to convert
   * @param lang The language in which the TemplateNode should be created.
   * @return A List of every node in nodes, with TextNodes changed to TemplateNodes.
   */
  private def toTemplateNodes(nodes: List[Node], lang: Language): List[Node] =
      nodes.flatMap({
        case TextNode(text, line, _) => List(TemplateNode(
                new WikiTitle(text.capitalize, templateNamespace, lang), 
                List.empty, line
            ))
        case node:Node => List(node)
      })

  def apply(node: TemplateNode, lang: Language) : List[Node] = {

     val mapKey = if (transformerMap.contains(lang)) lang else Language.English

    val stringToFunction = transformerMap(mapKey)
    val maybeFunction = stringToFunction.get(node.title.decoded)
    val transformation = maybeFunction match{
       case Some(trans) => trans
       case None =>
         //TODO record un-transformed template to have statistics about which template to cover!
         identity _
     }
    transformation(node, lang)
  }
}
