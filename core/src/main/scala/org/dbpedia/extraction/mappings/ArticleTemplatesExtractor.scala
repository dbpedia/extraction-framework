package org.dbpedia.extraction.mappings

import collection.mutable.HashSet
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{Dataset, DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * This extractor extracts all templates that exist in an article.
 * This data can be used for Wikipedia administrative tasks.
 */
class ArticleTemplatesExtractor(
    context: {
     def ontology: Ontology
     def language: Language
     def redirects: Redirects
    }
  ) extends PageNodeExtractor {

  // FIXME: this uses the http://xx.dbpedia.org/property/ namespace, but the
  // http://dbpedia.org/ontology/ namespace would probably make more sense.
  private val usesTemplateProperty = context.language.propertyUri.append("wikiPageUsesTemplate")

  override val datasets = Set(DBpediaDatasets.ArticleTemplates, DBpediaDatasets.ArticleTemplatesNested)

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    var quads = new ArrayBuffer[Quad]()

    val seenTemplates = new HashSet[String]()

    val topLevelTemplates = collectTemplatesTopLevel(node)
    val nestedTemplates = collectTemplatesTransitive(node).filter( !topLevelTemplates.contains(_))

    val topLevelQuads = templatesToQuads(topLevelTemplates, subjectUri, DBpediaDatasets.ArticleTemplates)
    val nestedQuads = templatesToQuads(nestedTemplates, subjectUri, DBpediaDatasets.ArticleTemplatesNested)

    topLevelQuads ++ nestedQuads
  }

  private def templatesToQuads(templates: List[TemplateNode], subjectUri: String, dataset: Dataset) : Seq[Quad] = {
    templates.map( t => {
      val templateUri = context.language.resourceUri.append(t.title.decodedWithNamespace)
      new Quad(context.language, dataset, subjectUri, usesTemplateProperty,
        templateUri, t.sourceUri, null)
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
