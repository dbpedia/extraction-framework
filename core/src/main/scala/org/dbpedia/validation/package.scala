package org.dbpedia

import scala.collection.immutable.HashSet
import scala.collection.mutable

package object validation {

  case class EvalCounter(all: Long, trg: Long, vld: Long) {

    def coverage: Float = if ( all > 0 ) trg.toFloat / all.toFloat else 0

    override def toString: String = s"all: $all trg: $trg vld: $vld"
  }

  case class CoverageResult(subjects: EvalCounter, predicates: EvalCounter, objects: EvalCounter) {

    def coverage: Float = {

      if ( 0 < (subjects.coverage + predicates.coverage + objects.coverage ) ) {
        (subjects.coverage + predicates.coverage + objects.coverage ) / 3f
      } else {
        0
      }
    }

    override def toString: String = {

      s"""
         |C_s ${subjects.coverage} ${subjects.toString}
         |C_p ${predicates.coverage} ${predicates.toString}
         |C_o ${objects.coverage} ${objects.toString}
         |C   $coverage
         """.stripMargin
    }
  }

  case class TestSuite(triggers: Array[IriTrigger],
                       validators: Array[IriValidator], validatorReferencesToIndexMap: Map[ValidatorReference,Int])

  case class IriTrigger(id: TriggerReference, label: String, comment: String,
                        patterns: Array[String] /*TODO: or REGEX*/, validatorReferences: Array[ValidatorReference])

  case class IriValidator(id: ValidatorReference, hasScheme: String, hasQuery: Boolean,
                          hasFragment: Boolean, patterns: Array[String]  /*TODO: or REGEX*/,
                          oneOf: HashSet[String])

  type ValidatorReference = String
  type TriggerReference = String

  private val prefixVocab: String = "http://dev.vocab.org/"

  private def prefixDefinition: String =
    s"""PREFIX v: <$prefixVocab>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
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
       |     v:pattern    ?pattern ;
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

}

