package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.transform.TemplateTransformConfig
import org.dbpedia.extraction.mappings.template.PropertyValueBuilder

/**
 * Represents a template.
 *
 * @param title The title of the page, where this template is defined
 * @param children The properties of this template
 * @param line The source line number of this property
 */
case class TemplateNode (
    title : WikiTitle,
    override val children : List[PropertyNode],
    override val line : Int,
    titleParsed : List[Node] = List())
  extends Node(children, line)
{
    private val propertyMap : Map[String, PropertyNode] = Map.empty ++ (for(property <- children) yield (property.key, property))
    
    /**
     * Retrieves a property by its key.
     * As in Wikipedia the search is case insensitive in the first character and case sensitive in the rest.
     *
     * @param key The key of the wanted property.
     * @return PropertyNode The propertyNode or None if no property with a given key has been found.
     */
    def property(key : String) : Option[PropertyNode] =
    {
        return propertyMap.get(key);
    }

    def property(builder: PropertyValueBuilder) : Option[PropertyNode] = builder(this)

    def keySet :  scala.collection.Set[String] = propertyMap.keySet

    def toWikiText = "{{" + title.decoded + (if(children.isEmpty) "" else "|") + children.map(_.toWikiText).mkString("|") + "}}"
    
    // templates are skipped for plain text
    def toPlainText = ""
}

/**
 *
 */
object TemplateNode {

    def transform(node: TemplateNode) : List[Node] = {

      TemplateTransformConfig(node, node.title.language)
    }
}
