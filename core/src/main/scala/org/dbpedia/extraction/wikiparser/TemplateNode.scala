package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.config.provenance.{NodeRecord, ProvenanceRecord}
import org.dbpedia.extraction.config.transform.TemplateTransformConfig

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
  extends Node
{
    private val propertyMap : Map[String, PropertyNode] = Map.empty ++
          (for(property <- children) yield (property.key, property))
    
    /**
     * Retrieves a property by its key.
     * As in Wikipedia the search is case insensitive in the first character and case sensitive in the rest.
     *
     * @param key The key of the wanted property.
     * @return PropertyNode The propertyNode or None if no property with a given key has been found.
     */
    def property(key : String) : Option[PropertyNode] =
    {
        propertyMap.get(key)
    }

    def keySet :  scala.collection.Set[String] = propertyMap.keySet

    def toWikiText: String = "{{" + title.decoded + (if(children.isEmpty) "" else "|") + children.map(_.toWikiText).mkString("|") + "}}"
    
    // templates are skipped for plain text
    def toPlainText = ""

    override def getNodeRecord: NodeRecord = this.root.getNodeRecord.copy(Some(this.line))

    override def equals(obj: scala.Any): Boolean = obj match {

        case otherTemplateNode : TemplateNode =>
            otherTemplateNode.title == title &&
          otherTemplateNode.line == line &&
          NodeUtil.filterEmptyTextNodes(otherTemplateNode.children) == NodeUtil.filterEmptyTextNodes(children)
        case _ => false
    }
}

/**
 *
 */
object TemplateNode {

    def transform(node: TemplateNode) : List[Node] = {

      TemplateTransformConfig(node, node.title.language)
    }

    def transformAll(node: Node) : Unit = {

    }
}
