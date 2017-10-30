package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage}

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls

/**
  * Created by chile on 25.10.17.
  */
@SoftwareAgentAnnotation(classOf[TemplateExtractor], AnnotationType.Extractor)
class TemplateExtractor (
      context : {
        def ontology : Ontology
        def language : Language
      }
    )
    extends WikiPageExtractor
  {
  private val language = context.language

  private val templateName = context.ontology.properties("templateName")
  private val rdfType = context.ontology.getOntologyProperty("rdf:type") match{
    case Some(o) => o.uri
    case None => throw new IllegalArgumentException("Could not find uri for rdf:type")
  }
  private val templateType = context.ontology.getOntologyClass("WikimediaTemplate") match{
    case Some(c) => c.uri
    case None => throw new IllegalArgumentException("Could not find uri for dbo:WikimediaTemplate")
  }

  override val datasets = Set(DBpediaDatasets.TemplateDefinitions)

  override def extract(page : WikiPage, subjectUri : String): Seq[Quad] = {
    val quads = new ListBuffer[Quad] ()

    if(page.title.namespace == Namespace.Template && ! page.isRedirect) {
      val name = if(page.title.decoded.contains(":")) page.title.decoded.substring(page.title.decoded.indexOf(":")+1) else page.title.decoded
      quads += new Quad(language, datasets.head, subjectUri, rdfType, templateType, page.sourceIri, null)
      quads += new Quad(language, datasets.head, subjectUri, templateName, name, page.sourceIri, null)
    }
    if (page.isRedirect && page.redirect.namespace == Namespace.Template) {
      val name = if(page.redirect.decoded.contains(":")) page.redirect.decoded.substring(page.redirect.decoded.indexOf(":")+1) else page.redirect.decoded
      quads += new Quad(language, datasets.head, language.resourceUri.append(page.redirect.decodedWithNamespace), templateName, name, page.sourceIri, null)
    }

    quads.toList
  }
}
