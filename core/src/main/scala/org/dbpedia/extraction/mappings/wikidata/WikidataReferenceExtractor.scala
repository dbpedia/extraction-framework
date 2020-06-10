package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{Language, WikidataUtil}
import org.dbpedia.extraction.wikiparser.{JsonNode, Namespace}
import org.wikidata.wdtk.datamodel.interfaces.{StatementGroup, ValueSnak}

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Created by ali on 10/26/14.
 * Wikidata statement's references extracted on the form of
 * wd:Q76_P140_V39759 dbo:reference "http://www.christianitytoday.com/ct/2008/januaryweb-only/104-32.0.html?start=2"^^ xsd:string.
 *
 */
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
    //extracting references from item's statements
    if (page.wikiPage.title.namespace == Namespace.Main) {
      val document = page.wikiDataDocument.deserializeItemDocument(page.wikiPage.source)
      quads ++= extractStatements(document.getStatementGroups.toList,subjectUri, page.wikiPage.sourceIri)
    }
    //extracting references from properties' statements
    else if (page.wikiPage.title.namespace == Namespace.WikidataProperty){
      val document = page.wikiDataDocument.deserializePropertyDocument(page.wikiPage.source)
      quads ++= extractStatements(document.getStatementGroups.toList, subjectUri, page.wikiPage.sourceIri)
    }

    quads
  }
  private def extractStatements(statementGroups: List[StatementGroup], subjectUri: String, sourceIri: String): Seq[Quad] = {
    val quads = new ArrayBuffer[Quad]()
    for (statementGroup <- statementGroups) {
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
                    quads += new Quad(context.language, DBpediaDatasets.WikidataReference, statementUri, referenceProperty, WikidataUtil.getValue(value), sourceIri, datatype)
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
