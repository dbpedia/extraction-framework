package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.JsonNode
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Created by ali on 10/26/14.
 * Wikidata statement's references extracted on the form of
 * wd:Q76_P140_V39759 dbo:reference "http://www.christianitytoday.com/ct/2008/januaryweb-only/104-32.0.html?start=2"^^ xsd:string.
 *
 */
@SoftwareAgentAnnotation(classOf[WikidataReferenceExtractor], AnnotationType.Extractor)
class WikidataReferenceExtractor(
                                  context: {
                                    def ontology: Ontology
                                    def language: Language
                                  }
                                  )
  extends JsonNodeExtractor {

  val referenceProperty = context.ontology.properties("reference")

  override val datasets = Set(DBpediaDatasets.WikidataReference)

  override def extract(page: JsonNode, subjectUri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()

    for (statementGroup <- page.wikiDataDocument.getStatementGroups) {
      statementGroup.getStatements.foreach {
        statement => {
          val references = statement.getReferences
          val property = statement.getClaim().getMainSnak().getPropertyId().getIri
          if (!references.isEmpty) {
            for (i <- references.indices) {
              for (reference <- references.get(i).getAllSnaks) {
                reference match {
                  case snak: ValueSnak => {
                    val value = snak.getValue
                    val statementUri = WikidataUtil.getStatementUri(subjectUri, property, value)
                    val datatype = if (WikidataUtil.getDatatype(value) != null) context.ontology.datatypes(WikidataUtil.getDatatype(value)) else null
                    quads += new Quad(context.language, DBpediaDatasets.WikidataReference, statementUri, referenceProperty, WikidataUtil.getValue(value), page.wikiPage.sourceIri, datatype)
                  }
                  case _ =>
                }
              }
            }
          }
        }

      }
    }

    quads
  }
}
