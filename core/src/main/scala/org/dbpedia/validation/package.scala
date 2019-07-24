package org.dbpedia

import org.apache.jena.query.{QueryExecutionFactory, QueryFactory}
import org.apache.jena.rdf.model.{Model, ModelFactory}

import scala.collection.mutable.ListBuffer

package object validation {

  private val prefixVocab: String = "http://dev.vocab.org/"

  private def prefixDefinition: String =
    s"""PREFIX v: <$prefixVocab>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
     """.stripMargin

  def iriTestCaseQuery(): String =
    s"""$prefixDefinition
       |
       |SELECT ?testCase
       |       (GROUP_CONCAT(DISTINCT ?validator; SEPARATOR="\t")
       |       (GROUP_CONCAT(DISTINCT ?validator; SEPARATOR="\t") {
       |  ?testCase
       |}
     """.stripMargin

  def iriTriggerQuery(): String =
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

  def iriValidatorQuery(): String =
    s"""
       |
     """.stripMargin

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


  /*------------------------------------------------------------------------------------------------------------------*/

  case class IRI_Trigger(iri: String, label: String, comment: String, patterns: List[String] /*TODO: or REGEX*/ )
  case class IRI_Validator(iri: String, has_scheme: String, has_query: Boolean, has_fragment: Boolean, not_contains: List[String] /*TODO: or REGEX*/)

  protected def prefix_v = "<http://dev.vocab.org/>"

  def load_iri_list(rdf_data: String): Unit = {

    //    TODO: Form file, URL etc

    val m_data = ModelFactory.createDefaultModel()
    m_data.read(rdf_data)

  }

  def load_test_cases(rdf_tests: String): Unit = {

    //    TODO: Jena could not read https and ttl is not well formed
    //    val testCaseTTL = new URL("https://raw.githubusercontent.com/dbpedia/extraction-framework/master/new_release_based_ci_tests_draft.ttl")
    //    IOUtils.copy(testCaseTTL.openStream(), System.out)

    val m_tests = ModelFactory.createDefaultModel()
    m_tests.read(rdf_tests)

    val l_iri_triggers = load_iri_triggers(m_tests)
    val l_iri_validators = load_iri_validators(m_tests)

  }

  private def load_iri_triggers(m_tests: Model): List[IRI_Trigger] = {

    val q_trigger = QueryFactory.create(
      s"""
         |PREFIX v: $prefix_v
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT ?trigger ?label ?comment (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns) {
         |  ?trigger
         |     a            v:RDF_IRI_Trigger ;
         |     v:pattern    ?pattern ;
         |     rdfs:label   ?label ;
         |     rdfs:comment ?comment .
         |
         |} GROUP BY ?trigger ?label ?comment
      """.stripMargin)

    val query_exec = QueryExecutionFactory.create(q_trigger,m_tests)
    val result_set = query_exec.execSelect()

    val l_iri_trigger = ListBuffer[IRI_Trigger]()

    while ( result_set.hasNext ) {

      val solution = result_set.next()

      l_iri_trigger.append(
        IRI_Trigger(
          solution.getResource("trigger").getURI,
          solution.getLiteral("label").getLexicalForm,
          solution.getLiteral("comment").getLexicalForm,
          solution.getLiteral("patterns").getLexicalForm.split("\t").toList
        )
      )

      print(
        s"""
           |FOUND TRIGGER: ${solution.getResource("trigger").getURI}
           |> LABEL: ${solution.getLiteral("label").getLexicalForm}
           |> COMMENT: ${solution.getLiteral("comment").getLexicalForm}
           |> PATTERN: ${solution.getLiteral("patterns").getLexicalForm.split("\t").toList}
        """.stripMargin
      )
    }

    l_iri_trigger.toList
  }

  private def load_iri_validators(m_tests: Model): List[IRI_Validator] = {

    val q_validator = QueryFactory.create(
      s"""
         |PREFIX v: $prefix_v
         |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
         |
         |SELECT ?validator ?hasScheme ?hasQuery ?hasFragment (GROUP_CONCAT(DISTINCT ?notContain; SEPARATOR="\t") AS ?notContainChars) {
         |  ?validator
         |     a                          v:IRI_Validator ;
         |     v:hasScheme                ?hasScheme ;
         |     v:hasQuery                 ?hasQuery ;
         |     v:hasFragment              ?hasFragment ;
         |     v:doesNotContainCharacters ?notContain .
         |
         |} GROUP BY ?validator ?hasScheme ?hasQuery ?hasFragment
      """.stripMargin)

    val query_exec = QueryExecutionFactory.create(q_validator,m_tests)
    val result_set = query_exec.execSelect()

    val l_iri_validator = ListBuffer[IRI_Validator]()

    while ( result_set.hasNext ) {

      val solution = result_set.next()

      l_iri_validator.append(
        IRI_Validator(
          solution.getResource("validator").getURI,
          solution.getLiteral("hasScheme").getLexicalForm,
          solution.getLiteral("hasQuery").getLexicalForm.toBoolean,
          solution.getLiteral("hasFragment").getLexicalForm.toBoolean,
          solution.getLiteral("notContainChars").getLexicalForm.split("\t").toList
          //          solution.getLiteral("notContainChars").getLexicalForm.split("\t").map(_.charAt(0)).toList)
        )
      )

      print(
        s"""
           |FOUND VALIDATOR: ${solution.getResource("validator").getURI}
           |> SCHEME: ${solution.getLiteral("hasScheme").getLexicalForm}
           |> QUERY: ${solution.getLiteral("hasQuery").getLexicalForm}
           |> FRAGMENT: ${solution.getLiteral("hasFragment").getLexicalForm}
           |> NOT CONTAIN CHARS: ${solution.getLiteral("notContainChars").getLexicalForm.split("\t").toList}
        """.stripMargin
      )
    }

    l_iri_validator.toList
  }
}

