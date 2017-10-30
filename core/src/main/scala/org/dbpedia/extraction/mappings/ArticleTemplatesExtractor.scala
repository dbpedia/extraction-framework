package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.transform.Quad

import collection.mutable.HashSet
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.collection.mutable.ArrayBuffer
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

  private val usesTemplateProperty = context.ontology.getOntologyProperty("wikiPageUsesTemplate") match{
    case Some(o) => o.uri
    case None => throw new IllegalArgumentException("Could not find uri for dbo:wikiPageUsesTemplate")
  }
  override val datasets = Set(DBpediaDatasets.ArticleTemplates, DBpediaDatasets.ArticleTemplatesNested)

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] = {

    val topLevelTemplates = collectTemplatesTopLevel(node)
    val nestedTemplates = collectTemplatesTransitive(node).filter( !topLevelTemplates.contains(_))

    val topLevelQuads = templatesToQuads(topLevelTemplates, subjectUri, DBpediaDatasets.ArticleTemplates)
    val nestedQuads = templatesToQuads(nestedTemplates, subjectUri, DBpediaDatasets.ArticleTemplatesNested)

    topLevelQuads ++ nestedQuads
  }

  private def templatesToQuads(templates: List[TemplateNode], subjectUri: String, dataset: Dataset) : Seq[Quad] = {
    templates.map(t => {
      val templateUri = context.language.resourceUri.append(t.title.decodedWithNamespace)
      new Quad(context.language, dataset, subjectUri, usesTemplateProperty, templateUri, t.sourceIri, null)
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
