package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}

import scala.language.reflectiveCalls
import scala.collection.mutable.ListBuffer

/**
 * Extracts the link texts used to refer to a page by means of internal links. This data provides one part of the input
 * for the surface forms dataset.
 *
 * @author Michael Moore
 */
@SoftwareAgentAnnotation(classOf[AnchorTextExtractor], AnnotationType.Extractor)
class AnchorTextExtractor(
                  context: {
                    def ontology: Ontology
                    def language: Language
                  }
                  )
  extends PageNodeExtractor {
  val wikiPageWikiLinkProperty = context.ontology.properties("wikiPageWikiLinkText")

  override val datasets = Set(DBpediaDatasets.AnchorText)

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] = {
    if (node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) {
      return Seq.empty
    }

    val baseBuilder = QuadBuilder(
      context.language,
      DBpediaDatasets.AnchorText,
      wikiPageWikiLinkProperty,
      context.ontology.datatypes("rdf:langString")
    )
    baseBuilder.setNodeRecord(node.getNodeRecord)

    val list = AnchorTextExtractor.collectInternalLinks(node)
    var buffer = new ListBuffer[Quad]()
    for (node <- list) {
      baseBuilder.setExtractor(ExtractorRecord(
        this.softwareAgentAnnotation,
        Seq(),
        None,
        None,
        None,
        node.containedTemplateNames()
      ))

      if (node.destination.namespace == Namespace.Main) {
        val qb = baseBuilder.clone
        qb.setSubject(getUri(node.destination))
        qb.setValue(node.children.map(_.toPlainText).mkString(""))
        qb.setSourceUri(node.sourceIri)
        buffer += qb.getQuad
      }
      for (child <- node.children) {
        child match {
          case intlink: InternalLinkNode => if (node.destination.namespace == Namespace.Main) {
            val qb = baseBuilder.clone
            qb.setSubject(getUri(intlink.destination))
            qb.setValue(intlink.children.head.toPlainText)
            qb.setSourceUri(node.sourceIri)
            buffer += qb.getQuad
          }
          case _ =>
        }
      }
    }
    val list2 = buffer.toList
    list2
  }

  private def getUri(destination: WikiTitle): String = {
    context.language.resourceUri.append(destination.decodedWithNamespace)
  }
}

object AnchorTextExtractor {

  def collectInternalLinks(node: Node): List[InternalLinkNode] = {
    node match {
      case linkNode: InternalLinkNode => List(linkNode)
      case _ => node.children.flatMap(collectInternalLinks)
    }
  }
}