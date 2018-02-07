package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts labels to articles based on their title.
 */
@SoftwareAgentAnnotation(classOf[LabelExtractor], AnnotationType.Extractor)
class LabelExtractor( 
  context : {
    def ontology : Ontology
    def language : Language 
  } 
) 
extends WikiPageExtractor
{
  val labelProperty = context.ontology.properties("rdfs:label")
  
  override val datasets = Set(DBpediaDatasets.Labels)

  private val langString =context.ontology.datatypes("rdf:langString")
  private val qb = QuadBuilder(context.language, DBpediaDatasets.Labels, labelProperty, langString)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(page: WikiPage, subjectUri: String) : Seq[Quad] =
  {
    if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title))
      return Seq.empty

    // TODO: use templates like {{lowercase}}, magic words like {{DISPLAYTITLE}}, 
    // remove stuff like "(1999 film)" from title...
    val label = page.title.decoded
    
    if(label.isEmpty)
      return Seq.empty

    qb.setSourceUri(page.sourceIri)
    qb.setNodeRecord(page.getNodeRecord)

    qb.setSubject(subjectUri)
    qb.setValue(label)
    qb.setDatatype(langString)
    Seq(qb.getQuad)
  }
}
