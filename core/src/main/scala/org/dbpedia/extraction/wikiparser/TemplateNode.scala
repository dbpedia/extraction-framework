package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.{NodeRecord, QuadProvenanceRecord}
import org.dbpedia.extraction.config.transform.TemplateTransformConfig
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

/**
 * Represents a template.
 *
 * @param title The title of the page, where this template is defined
 * @param children The properties of this template
 * @param line The source line number of this property
 */
@WikiNodeAnnotation(classOf[TemplateNode])
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


    /**
      * Creates a NodeRecord metadata object of this node
      *
      * @return
      */
    override def getNodeRecord = NodeRecord(
        IRI.create(this.sourceIri).get,
        this.wikiNodeAnnotation,
        this.root.revision,
        this.root.title.namespace.code,
        this.id,
        this.root.title.language,
        Option(this.line),
        Option(title.decoded),
        if(section != null)
            Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
        else
            None
    )

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
