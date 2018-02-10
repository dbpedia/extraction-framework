package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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

    private val qb = QuadBuilder.dynamicPredicate(context.language, DBpediaDatasets.TemplateDefinitions)
    qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(page : WikiPage, subjectUri : String): Seq[Quad] = {
    val quads = new ListBuffer[Quad] ()

    qb.setNodeRecord(page.getNodeRecord)
    qb.setSourceUri(page.sourceIri)
    qb.setSubject(subjectUri)

    if(page.title.namespace == Namespace.Template && ! page.isRedirect) {
      val name = if(page.title.decoded.contains(":")) page.title.decoded.substring(page.title.decoded.indexOf(":")+1) else page.title.decoded
      qb.setPredicate(rdfType)
      qb.setValue(templateType)
      quads += qb.getQuad
      qb.setPredicate(templateName)
      qb.setValue(name)
      quads += qb.getQuad
    }
    if (page.isRedirect && page.redirect.namespace == Namespace.Template) {
      val name = if(page.redirect.decoded.contains(":")) page.redirect.decoded.substring(page.redirect.decoded.indexOf(":")+1) else page.redirect.decoded
      qb.setSubject(language.resourceUri.append(page.redirect.decodedWithNamespace))
      qb.setPredicate(templateName)
      qb.setValue(name)
      quads += qb.getQuad
    }

    quads.toList
  }
}
