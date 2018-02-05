package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls

/**
 * This extractor extracts all templates that exist in an article.
 * This data can be used for Wikipedia administrative tasks.
 */
@SoftwareAgentAnnotation(classOf[ArticleTemplatesExtractor], AnnotationType.Extractor)
class ArticleTemplatesExtractor(
    context: {
     def ontology: Ontology
     def language: Language
     def redirects: Redirects
    }
  ) extends PageNodeExtractor {

  private val usesTemplateProperty = context.ontology.getOntologyProperty("wikiPageUsesTemplate")
  override val datasets = Set(DBpediaDatasets.ArticleTemplates, DBpediaDatasets.ArticleTemplatesNested)

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] = {

    val topLevelTemplates = collectTemplatesTopLevel(node)
    val nestedTemplates = collectTemplatesTransitive(node).filter( !topLevelTemplates.contains(_))

    val qb = new QuadBuilder(Some(subjectUri), usesTemplateProperty, None, None, context.language, None, None, None)
    qb.setNodeRecord(node.getNodeRecord)
    qb.setExtractor(this.softwareAgentAnnotation)
    qb.setDataset(DBpediaDatasets.ArticleTemplates)
    val topLevelQuads = templatesToQuads(topLevelTemplates, qb)

    qb.setDataset(DBpediaDatasets.ArticleTemplatesNested)
    val nestedQuads = templatesToQuads(nestedTemplates, qb)


    topLevelQuads ++ nestedQuads
  }

  private def templatesToQuads(templates: List[TemplateNode], qb: QuadBuilder) : Seq[Quad] = {
    templates.map(t => {
      qb.setValue(context.language.resourceUri.append(t.title.decodedWithNamespace))
      qb.setSourceUri(t.sourceIri)
      qb.getQuad
    })
  }

  private def collectTemplatesTopLevel(node: Node): List[TemplateNode] = {
    node match {
      case templateNode: TemplateNode => List(templateNode)
      case _ => node.children.flatMap(collectTemplatesTopLevel)
    }
  }

  private def collectTemplatesTransitive(node: Node): List[TemplateNode] = {
    node match {
      case templateNode: TemplateNode => List(templateNode) ++ node.children.flatMap(collectTemplatesTransitive)
      case _ => node.children.flatMap(collectTemplatesTransitive)
    }
  }
}
