package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls



class HistoryStatsExtractor(
                            context : {
                              def ontology: Ontology
                              def language: Language
                            }
                          )
  extends WikiPageWithRevisionsExtractor {
  // PROP FROM ONTOLOGY
  private val nbUniqueContribProperty = context.ontology.properties("nbUniqueContrib")
  private val avgRevSizePerMonthProperty = context.ontology.properties("avgRevSizePerMonth")
  private val avgRevSizePerYearProperty = context.ontology.properties("avgRevSizePerYear")
  private val nbRevPerYearProperty = context.ontology.properties("nbRevPerYear")
  private val NbRevPerMonthProperty = context.ontology.properties("nbRevPerMonth")

  // PROPERTIES NOT IN THE ONTOLOGY
  private val propDate = "http://purl.org/dc/elements/1.1/date"
  private val quadPropDate = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryStats, propDate, context.ontology.datatypes("xsd:date")) _
  private val propValue = "http://www.w3.org/1999/02/22-rdf-syntax-ns#value"
  private val quadPropValue = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.HistoryStats, propValue, context.ontology.datatypes("xsd:integer")) _



  override val datasets = Set(DBpediaDatasets.HistoryStats) //# ToADD


  override def extract(page: WikiPageWithRevisions , subjectUri: String): Seq[Quad] = {


    val quads = new ArrayBuffer[Quad]()
    quads += new Quad(context.language, DBpediaDatasets.HistoryStats, page.title.pageIri, nbUniqueContribProperty, page.getUniqueContributors.toString, page.sourceIri, context.ontology.datatypes("xsd:integer"))

    page.getRevPerYear foreach {
      case (key, value) =>
        val bn_nbRevPerYear = subjectUri + "__nbRevPerYear__"+key
        quads += new Quad(context.language, DBpediaDatasets.HistoryStats, page.title.pageIri, nbRevPerYearProperty, bn_nbRevPerYear, page.sourceIri, context.ontology.datatypes("xsd:integer"))
        quads += quadPropDate(bn_nbRevPerYear,  key, page.sourceIri)
        quads += quadPropValue(bn_nbRevPerYear,  value.toString, page.sourceIri)

    }
    page.getRevPerYearMonth foreach {
      case (key, value) =>
        val bn_nbRevPerMonth = subjectUri + "__nbRevPerMonth__"+key
        quads += new Quad(context.language, DBpediaDatasets.HistoryStats, page.title.pageIri, NbRevPerMonthProperty, bn_nbRevPerMonth, page.sourceIri, context.ontology.datatypes("xsd:integer"))
        quads += quadPropDate(bn_nbRevPerMonth, key, page.sourceIri)
        quads += quadPropValue(bn_nbRevPerMonth, value.toString, page.sourceIri)

    }
    page.getRevPerYearAvgSize foreach {
      case (key, value) =>
        val bn_revPerYearAvgSize = subjectUri + "__revPerYearAvgSize__"+key
        quads += new Quad(context.language, DBpediaDatasets.HistoryStats, page.title.pageIri, avgRevSizePerYearProperty, bn_revPerYearAvgSize, page.sourceIri, context.ontology.datatypes("xsd:integer"))
        quads += quadPropDate(bn_revPerYearAvgSize, key, page.sourceIri)
        quads += quadPropValue(bn_revPerYearAvgSize, value.toString, page.sourceIri)
    }
    page.getRevPerYearMonthAvgSize foreach {
      case (key, value) =>

        val bn_revPerMonthAvgSize = subjectUri + "__revPerMonthAvgSize__"+key
        quads += new Quad(context.language, DBpediaDatasets.HistoryStats, page.title.pageIri, avgRevSizePerMonthProperty, bn_revPerMonthAvgSize, page.sourceIri, context.ontology.datatypes("xsd:integer"))
        quads += quadPropDate(bn_revPerMonthAvgSize, key, page.sourceIri)
        quads += quadPropValue(bn_revPerMonthAvgSize, value.toString, page.sourceIri)

    }



    quads
  }


}
