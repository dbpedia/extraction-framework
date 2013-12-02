package org.dbpedia.extraction.mappings

import collection.mutable.HashSet
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer

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
  ) extends Extractor {

  // FIXME: this uses the http://xx.dbpedia.org/property/ namespace, but the
  // http://dbpedia.org/ontology/ namespace would probably make more sense.
  private val usesTemplateProperty = context.language.propertyUri.append("wikiPageUsesTemplate")

  override val datasets = Set(DBpediaDatasets.ArticleTemplates)

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    var quads = new ArrayBuffer[Quad]()

    val seenTemplates = new HashSet[String]()

    for (template <- collectTemplates(node)) {
      val templateUri = context.language.resourceUri.append(template.title.decodedWithNamespace)
      if (!seenTemplates.contains(templateUri)) {
        quads += new Quad(context.language, DBpediaDatasets.ArticleTemplates, subjectUri, usesTemplateProperty,
          templateUri, template.sourceUri, null)
        seenTemplates.add(templateUri)
      }
    }
    quads
  }

  private def collectTemplates(node: Node): List[TemplateNode] = {
    node match {
      case templateNode: TemplateNode => List(templateNode)
      case _ => node.children.flatMap(collectTemplates)
    }
  }
}
