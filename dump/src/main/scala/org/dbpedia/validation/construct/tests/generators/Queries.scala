package org.dbpedia.validation.construct.tests.generators

/**
 * TODO refactor, merge
 */
object Queries {

  private val prefixVocab: String = "http://dev.vocab.org/"

  private lazy val prefixDefinition: String =
    s"""PREFIX v: <$prefixVocab>
       |PREFIX trigger: <http://dev.vocab.org/trigger/>
       |PREFIX validator: <http://dev.vocab.org/validator/>
       |PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
       |PREFIX dataid-mt: <http://dataid.dbpedia.org/ns/mt#>
     """.stripMargin

  def iriTriggerQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT ?trigger ?label ?comment
       |  (GROUP_CONCAT(DISTINCT ?pattern; SEPARATOR="\t") AS ?patterns)
       |{
       |  ?trigger
       |     a            v:RDF_IRI_Trigger ;
       |     trigger:pattern    ?pattern ;
       |     Optional{ ?trigger rdfs:label ?label . }
       |     Optional{ ?trigger rdfs:comment ?comment . }
       |
       |
       |} GROUP BY ?trigger ?label ?comment
     """.stripMargin

  def literalTriggerQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT DISTINCT ?trigger ?datatype ?label ?comment
       |{
       |  ?trigger
       |     a            v:RDF_Literal_Trigger ;
       |     trigger:datatype    ?datatype ;
       |     Optional{ ?trigger rdfs:label ?label . }
       |     Optional{ ?trigger rdfs:comment ?comment . }
       |
       |}
     """.stripMargin

  def iriValidatorQueryStr(): String =
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

  def literalValidatorQueryStr(): String =
    s"""$prefixDefinition
       |
       |SELECT Distinct ?validator ?comment ?pattern ?validatorGroup ?doesNotContains
       |{
       |  ?validator
       |     a v:Datatype_Literal_Validator .
       |     Optional{ ?validator v:validatorGroup ?validatorGroup }
       |     Optional{ ?validator rdfs:comment ?comment }
       |     Optional{ ?validator v:doesNotContain ?doesNotContains . }
       |     Optional{ ?validator v:pattern ?pattern }
       |}
     """.stripMargin

  def triggeredValidatorsQueryStr(triggerIri: String, isBlank: Boolean): String = {

    s"""$prefixDefinition
       |
       |SELECT ?validator {
       |
       |  ?s a v:TestGenerator ;
       |     v:trigger ${if (isBlank) s"_:$triggerIri" else s"<$triggerIri>" } ;
       |     v:validator ?validator .
       |}
     """.stripMargin
  }

  def testGeneratorQueryStr: String =
    s"""$prefixDefinition
       |
       |SELECT ?generator ?trigger ?validator
       |{
       |  ?generator
       |  a     v:TestGenerator ;
       |  v:trigger ?trigger ;
       |  v:validator ?validator .
       |
       |}
     """.stripMargin

  def oneOfVocabQueryStr: String =
    """PREFIX owl: <http://www.w3.org/2002/07/owl#>
      |
      |SELECT DISTINCT ?property {
      |  #VALUES ?type {
      |  #   <http://www.w3.org/1999/02/22-rdf-syntax-ns#Property>
      |  #   owl:DatatypeProperty
      |  #   owl:ObjectProperty
      |  #}
      |  ?property a ?type .
      |  #FILTER ( ?type IN ( owl:DatatypeProperty, owl:ObjectProperty ) )
      |}"""
      .stripMargin
}
