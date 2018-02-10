package org.dbpedia.extraction.config.transform

import com.fasterxml.jackson.databind.node.ArrayNode
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.{JsonConfig, Language}
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

  val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("templatetransform.json"))
  private var transformerMap = (for (lang <- mappingsFile.keys() if lang != "comment") yield {
    val language = Language(lang)
    language -> (for(trans <- mappingsFile.getMap(lang)) yield{
      val templateNames = if(trans._1.contains("$(lang)"))
        Language.map.keys.map(x => trans._1.replaceAll("\\$\\(lang\\)", x))
      else
        List(trans._1)

      for(template <- templateNames) yield {
        val keys = if (trans._2.get("keys") != null) trans._2.get("keys").asInstanceOf[ArrayNode].iterator().asScala.toList.map(_.asText()) else List()
        val contains = if (trans._2.get("whileList") != null) trans._2.get("whileList").asBoolean() else false
        val replace = if (trans._2.get("replace") != null) trans._2.get("replace").asText() else null
        val langParam = Option(if (trans._2.get("langParameter") != null) trans._2.get("langParameter").asText() else null)
        val templatesOnly = if (trans._2.get("templatesOnly") != null) trans._2.get("templatesOnly").asBoolean() else false
        template.trim.toLowerCase(language.locale) -> (trans._2.get("transformer").asText() match {
          case "externalLinkNode" => TemplateTransformerCatalog.externalLinkNode _
          case "unwrapTemplates" => TemplateTransformerCatalog.unwrapTemplates { p => if (contains) keys.contains(p.key) else !keys.contains(p.key) } _
          case "extractProperties" => TemplateTransformerCatalog.extractProperties { p => if (contains) keys.contains(p.key) else !keys.contains(p.key) } _
          case "extractChildren" => TemplateTransformerCatalog.extractAndReplace(p => if (contains) keys.contains(p.key) else !keys.contains(p.key), templatesOnly, replace) _
          case "internalLinkNode" => TemplateTransformerCatalog.internalLinkNode(p => if (contains) keys.contains(p.key) else !keys.contains(p.key)) _
          case "ushr" => TemplateTransformerCatalog.ushr()_
          case "getLangText" => {
            val langcode = langParam match{
              case Some(l) => (node: TemplateNode) => node.property(l).get.children.headOption.map(x => x.toPlainText).getOrElse("")
              case None => (node: TemplateNode) => template.substring(5)
            }
            TemplateTransformerCatalog.getLangText(p => if (contains) keys.contains(p.key) else !keys.contains(p.key), langcode) _
          }
          case "textNode" => TemplateTransformerCatalog.textNode {
            Option(replace) match {
              case Some(s) => s
              case None => ""
            }
          } _
        })
      }
    }).flatten.toMap
  }).toMap

  private def identity(node: TemplateNode, lang:Language) : List[Node] = List(node)

  def apply(node: TemplateNode, lang: Language) : List[Node] = {

     val mapKey = if (transformerMap.contains(lang)) lang else Language.English

     val transformation = transformerMap(mapKey).get(node.title.decoded.toLowerCase(lang.locale)) match{
       case Some(trans) => trans
       case None =>
         //TODO record un-transformed template to have statistics about which template to cover!
         identity _
     }
    transformation(node, lang)
  }
}
