package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}

import scala.language.reflectiveCalls
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{PageNode, TextNode, WikiTitle}

import scala.collection.mutable.ArrayBuffer

/**
 * Links non-commons DBpedia resources to their DBpedia Commons counterpart using owl:sameAs.
 * This requires the the Wikipedia page to contain a {{Commons}} template.
 *
 * Example http://en.wikipedia.org/wiki/Eurasian_blue_tit:
 *   Page contains node:
 *     {{Commons|Cyanistes caeruleus}}
 *
 *   Produces triple:
 *     <dbr:Eurasian_blue_tit> <owl:sameAs> <dbpedia-commons:Cyanistes_caeruleus>.
 *
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 * date 28.05.2016.
 */
@SoftwareAgentAnnotation(classOf[CommonsResourceExtractor], AnnotationType.Extractor)
class CommonsResourceExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
) extends PageNodeExtractor{

  private val propertyUri = context.ontology.properties("owl:sameAs")
  private val commonsLanguage = Language.apply("commons")

  override val datasets = Set(DBpediaDatasets.CommonsLink)

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] ={

    val qb = QuadBuilder.staticSubject(context.language, DBpediaDatasets.CommonsLink, subjectUri, propertyUri, node.getNodeRecord, ExtractorRecord(
      this.softwareAgentAnnotation
    ))
    qb.setSourceUri(node.sourceIri)

    for { template <- ExtendedInfoboxExtractor.collectTemplates(node)
      if template.title.decoded.equalsIgnoreCase("Commons")
    }
    {
      if (template.children.isEmpty){
        qb.setValue(WikiTitle.parse(node.title.encoded.asInstanceOf[String], commonsLanguage).resourceIri)
        return Seq(qb.getQuad)
      } else{
        qb.setValue(WikiTitle.parse(template.children.head.children.head.asInstanceOf[TextNode].text, commonsLanguage).resourceIri)
        return Seq(qb.getQuad)
      }
    }
    Seq.empty
  }
}
