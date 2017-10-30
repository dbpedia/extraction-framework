package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Links DBpedia Commons resources to their counterparts in other DBpedia languages (only en, de and fr) using owl:sameAs.
 * This requires the the Wikimedia page to contain a {{VN}} template.
 *
 * Example http://commons.wikimedia.org/wiki/Cyanistes_caeruleus:
 *   Page contains node:
 *   {{VN
 *     |de=Blaumeise
 *     |en=Blue Tit
 *     |fr=Mésange bleue
 *   }}
 *
 *   Produces triple:
 *     <dbpedia-commons:Cyanistes_caeruleus> <owl:sameAs> <dbr:Eurasian_blue_tit>.
 *     <dbpedia-commons:Cyanistes_caeruleus> <owl:sameAs> <dbpedia-de:Blaumeise>.
 *     <dbpedia-commons:Cyanistes_caeruleus> <owl:sameAs> <dbpedia-fr:Mésange_bleue>
 *
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 * date 28.05.2016.
 */
@SoftwareAgentAnnotation(classOf[DBpediaResourceExtractor], AnnotationType.Extractor)
class DBpediaResourceExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor {

  private val propertyUri : OntologyProperty = context.ontology.properties("owl:sameAs")

  /**
    * @param page       The source node
    * @param subjectUri The subject URI of the generated triples
    * @return A graph holding the extracted data
    */
  override def extract(page: PageNode, subjectUri: String): Seq[Quad] = {

    if(page.title.namespace != Namespace.Main) return Seq.empty

    for {
      template <- InfoboxExtractor.collectTemplates(page)
      if template.title.decoded == "VN"
    } {
      return template.children
        .filter((node : PropertyNode) => Seq("de", "en", "fr").contains(node.key))
        .map((node : PropertyNode) =>
          new Quad(
            context.language,
            DBpediaDatasets.CommonsLink,
            subjectUri,
            propertyUri,
            WikiTitle.parse(node.children.head.asInstanceOf[TextNode].text.split(", ").head, Language.apply(node.key)).resourceIri,
            page.sourceIri,
            null
          )
        )

    }
    Seq.empty
  }

  override val datasets: Set[Dataset] = Set(DBpediaDatasets.CommonsLink)
}
