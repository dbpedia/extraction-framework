package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset, Quad}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.util.{Language, WikiUtil}
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, PropertyNode, TextNode}
import scala.language.reflectiveCalls

/**
  * Created by Lukas Faber, Sebastian Serth, Stephan Haarmann
  */
class DBpediaResourceExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor {

  private val propertyUri : OntologyProperty = context.ontology.properties("owl:sameAs")
  private val objectBaseUri : String = "http://%sdbpedia.org/resource/%s"

  /**
    * @param page       The source node
    * @param subjectUri The subject URI of the generated triples
    * @param pageContext    The page context which holds the state of the extraction.
    * @return A graph holding the extracted data
    */
  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] = {

    if(page.title.namespace != Namespace.Main) return Seq.empty

    for {
      template <- InfoboxExtractor.collectTemplates(page)
      if (template.title.decoded == "VN")
    } {
      return template.children
        .filter((node : PropertyNode) => Seq("de", "en", "fr").contains(node.key))
        .map((node : PropertyNode) => new Quad(context.language, DBpediaDatasets.PageLinks, subjectUri, propertyUri, String.format(objectBaseUri, if (node.key == "en") "" else node.key + ".", WikiUtil.wikiEncode(node.children.head.asInstanceOf[TextNode].text.split(", ").head)), null, null))

    }
    Seq.empty
  }

  override val datasets: Set[Dataset] = Set(DBpediaDatasets.PageLinks)
}
