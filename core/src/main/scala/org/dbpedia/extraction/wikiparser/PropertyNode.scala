package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.NodeRecord
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI

import scala.util.{Failure, Success, Try}

/**
 * Represents a template property.
 *
 * @param key The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
@WikiNodeAnnotation(classOf[PropertyNode])
case class PropertyNode(key : String, override val children : List[Node], override val line : Int) extends Node
{
    def toWikiText: String =
    {
      // named arguments prefix name and "=", positional arguments use only the value
      val prefix = Try{ key.toInt; "" } match{
        case Success(_) => ""
        case Failure(_) => key+"="
      }
      prefix+children.map(_.toWikiText).mkString
    }
    
    // properties are skipped for plain text
    def toPlainText = ""

  override def getNodeRecord: NodeRecord = NodeRecord(
    IRI.create(this.sourceIri).get,
    this.wikiNodeAnnotation,
    this.root.revision,
    this.root.title.namespace.code,
    this.id,
    this.root.title.language,
    Option(this.line),
    Option(key),
    if(section != null)
      Some(escape(null, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes).toString)
    else
      None
  )

  def propertyNodeValueToPlainText: String = children.map(_.toPlainText).mkString

  override def equals(obj: scala.Any): Boolean = obj match {

    case otherPropertyNode : PropertyNode => ( otherPropertyNode.key == key //&&  otherPropertyNode.line == line
      && NodeUtil.filterEmptyTextNodes(otherPropertyNode.children) == NodeUtil.filterEmptyTextNodes(children))
    case _ => false

  }
}