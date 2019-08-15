package org.dbpedia

import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.mutable

package object validation {

  case class IriValidatorDev( id: ValidatorReference,
                              comment: ValidatorComment,
                              patterns: Array[String],
                              oneOfVocabs: Array[HashSet[String]],
                              doesNotContains: Array[String],
                              sizePatterns: Int, sizeOneOfVocabs: Int, sizeDoesNotContains: Int
                            )

  /*-----------------------------*/

  case class EvalCounter(all: Long, trg: Long, vld: Long) {

    def testCoverage: Float = if ( all > 0 ) trg.toFloat / all.toFloat else 0

    def proof: Float = if ( trg > 0 ) vld.toFloat / trg.toFloat else 0

    override def toString: String = s"all: $all trg: $trg vld: $vld"
  }

  case class CoverageResult(subjects: EvalCounter, predicates: EvalCounter, objects: EvalCounter) {

    def proof(): Float = {
      if ( 0 < (subjects.proof + predicates.proof + objects.proof ) ) {
        (subjects.proof + predicates.proof + objects.proof ) / 3f
      } else {
        0
      }
    }

    def testCoverage: Float = {

      if ( 0 < (subjects.testCoverage + predicates.testCoverage + objects.testCoverage ) ) {
        (subjects.testCoverage + predicates.testCoverage + objects.testCoverage ) / 3f
      } else {
        0
      }
    }

    override def toString: String = {

      s"""
         |Cov_s: ${subjects.testCoverage} ( ${subjects.trg} triggered of ${subjects.all} total ), Success_rate_s: ${subjects.proof} ( ${subjects.vld} )
         |Cov_p: ${predicates.testCoverage} ( ${predicates.trg} triggered of ${predicates.all} total ), Success_rate_p: ${predicates.proof} ( ${predicates.vld} )
         |Cov_o: ${objects.testCoverage} ( ${objects.trg} triggered of ${objects.all} total ), Success_rate_o: ${objects.proof} ( ${objects.vld} )
         |Cov:   $testCoverage
         """.stripMargin
    }
  }

  case class TestSuite(triggers: Array[IriTrigger],
                       validators: Array[IriValidatorDev], validatorReferencesToIndexMap: Map[ValidatorReference,Int])

  case class IriTrigger(id: TriggerReference, label: String, comment: String,
                        patterns: Array[String] /*TODO: or REGEX*/, validatorReferences: Array[ValidatorReference])

  case class IriValidator(id: ValidatorReference, hasScheme: String, hasQuery: Boolean,
                          hasFragment: Boolean, patterns: Array[String]  /*TODO: or REGEX*/,
                          oneOf: HashSet[String])

  type ValidatorReference = String
  type ValidatorComment = String
  type TriggerReference = String

  private val prefixVocab: String = "http://dev.vocab.org/"

  private def prefixDefinition: String =
    s"""PREFIX v: <$prefixVocab>
       |PREFIX trigger: <http://dev.vocab.org/trigger/>
       |PREFIX validator: <http://dev.vocab.org/validator/>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |PREFIX dataid-mt: <http://dataid.dbpedia.org/ns/mt#>
     """.stripMargin

  def iriTestCaseQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT ?testCase
       |       (GROUP_CONCAT(DISTINCT ?validator; SEPARATOR="\t")
       |       (GROUP_CONCAT(DISTINCT ?validator; SEPARATOR="\t") {
       |  ?testCase
       |}
     """.stripMargin

  def iriTriggerQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT ?trigger ?label ?comment (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns) {
       |  ?trigger
       |     a            v:RDF_IRI_Trigger ;
       |     trigger:pattern    ?pattern ;
       |     rdfs:label   ?label ;
       |     rdfs:comment ?comment .
       |
       |} GROUP BY ?trigger ?label ?comment
     """.stripMargin

  def iriValidatorQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT ?validator ?hasScheme ?hasQuery ?hasFragment ?patternRegex ?oneOfVocab
       |  (GROUP_CONCAT(DISTINCT ?doesNotContainCharacter; SEPARATOR="\t") AS ?doesNotContainCharacters)
       |{
       |  ?validator
       |     a                          v:IRI_Validator ;
       |     v:hasScheme                ?hasScheme ;
       |     v:hasQuery                 ?hasQuery ;
       |     v:hasFragment              ?hasFragment .
       |     Optional{ ?validator v:doesNotContainCharacters ?doesNotContainCharacter . }
       |     Optional{ ?validator v:patternRegex ?patternRegex . }
       |     Optional{ ?validator v:oneOfVocab ?oneOfVocab . }
       |
       |} GROUP BY ?validator ?hasScheme ?hasQuery ?hasFragment ?patternRegex ?oneOfVocab
     """.stripMargin

  def iriValidatorDevQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT Distinct ?validator ?comment
       |  (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns)
       |  (GROUP_CONCAT(DISTINCT ?oneOfVocab; SEPARATOR="\t") AS ?oneOfVocabs)
       |  (GROUP_CONCAT(DISTINCT ?doesNotContain; SEPARATOR="\t") AS ?doesNotContains)
       |{
       |  ?validator
       |     a v:IRI_Validator .
       |     Optional{ ?validator rdfs:comment ?comment }
       |     Optional{ ?validator v:doesNotContain ?doesNotContain . }
       |     Optional{ ?validator v:pattern ?pattern . }
       |     Optional{ ?validator v:oneOfVocab ?oneOfVocab . }
       |} GROUP BY ?validator ?comment
     """.stripMargin

  def triggeredValidatorsQueryStr(triggerIri: String): String =
    s"""$prefixDefinition
       |
       |SELECT ?validator {
       |
       |	?s v:trigger <$triggerIri> ;
       |     v:validator ?validator
       |
       |}
     """.stripMargin

  def oneOfVocabQueryStr: String =
    """PREFIX owl: <http://www.w3.org/2002/07/owl#>
      |
      |SELECT DISTINCT ?property {
      |
      |  ?property a  <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property> .
      |  #FILTER ( ?type IN ( owl:DatatypeProperty, owl:ObjectProperty ) )
      |}"""
     .stripMargin

  /*------------------------------------------------------------------------------------------------------- TODO clean*/

  trait RdfTrigger {

    object RdfTriggerType extends Enumeration {
      def RdfTriggerType: Value = Value
      val RdfIriTrigger, RdfLiteralTrigger, RdfBlankNodeTrigger = Value
    }

    def Type : RdfTriggerType.Value
  }

  case class RdfIriTrigger(iri: String, label: String, comment: String,
                           patterns: List[String] /*TODO: or REGEX*/) extends RdfTrigger {
    override def Type: RdfTriggerType.Value = RdfTriggerType.RdfIriTrigger
  }

  case class RdfIriValidator(iri: String, hasScheme: String, hasQuery: Boolean,
                             hasFragment: Boolean, notContainsChars: List[Char] /*TODO: or REGEX*/)

  object Tabulator {

    def format(table: Seq[Seq[Any]]): String = table match {
      case Seq() => ""
      case _ =>
        val sizes = for (row <- table) yield for (cell <- row) yield if (cell == null) 0 else cell.toString.length
        val colSizes = for (col <- sizes.transpose) yield col.max
        val rows = for (row <- table) yield formatRow(row, colSizes)
        formatRows(rowSeparator(colSizes), rows)
    }

    def formatRows(rowSeparator: String, rows: Seq[String]): String = (
      rowSeparator ::
        rows.head ::
        rowSeparator ::
        rows.tail.toList :::
        rowSeparator ::
        List()).mkString("\n")

    def formatRow(row: Seq[Any], colSizes: Seq[Int]): String = {
      val cells = for ((item, size) <- row.zip(colSizes)) yield if (size == 0) "" else ("%" + size + "s").format(item)
      cells.mkString("|", "|", "|")
    }

    def rowSeparator(colSizes: Seq[Int]): String = colSizes map { "-" * _ } mkString("+", "+", "+")
  }

}

