package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.resource.{Function, RMLConditionalPredicateObjectMap, RMLFunctionTermMap, RMLPredicateObjectMap}
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, RdfNamespace}

/**
  * Created by wmaroy on 12.08.17.
  */
class ConditionalTemplateAnalyzer(ontology: Ontology) extends AbstractTemplateAnalyzer(ontology) {

  @throws(classOf[IllegalArgumentException])
  def apply(pom: RMLPredicateObjectMap): Template ={

    if(!pom.isInstanceOf[RMLConditionalPredicateObjectMap]) {
      throw new IllegalArgumentException("Predicate Object Map is not conditional.")
    }

    val condPom = pom.asInstanceOf[RMLConditionalPredicateObjectMap]
    val conditionFTM = condPom.equalCondition
    val function = conditionFTM.getFunction
    val fallbacks = condPom.fallbacks

    val condition = getCondition(function)
    val ontologyClass = getOntologyClass(pom)

    val analyzer = new StdTemplatesAnalyzer(ontology)
    val templates = Seq(analyzer.analyze(condPom))

    val fallbackTemplate = retrieveFallbackTemplateFromFallbackPoms(fallbacks)

    ConditionalTemplate(condition, templates, ontologyClass, fallbackTemplate)
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  def getCondition(function : Function) = {

    val conditionName = function.name
    val conditionValueParam = function.references(RdfNamespace.DBF.namespace + conditionName + "/valueParameter")
    val conditionPropertyParam = function.references(RdfNamespace.DBF.namespace + conditionName + "/propertyParameter")

    val condition = conditionName match {
      case Condition.ISSET => IsSetCondition(conditionPropertyParam)
      case Condition.CONTAINS => ContainsCondition(conditionPropertyParam, conditionValueParam)
      case Condition.EQUALS => ContainsCondition(conditionPropertyParam, conditionValueParam)
      case Condition.OTHERWISE => OtherwiseCondition()
    }

    condition
  }

  def getOntologyClass(pom : RMLPredicateObjectMap): OntologyClass = {
    pom.rrPredicate match {
      case Property.TYPE => RMLOntologyUtil.loadOntologyClassFromIRI(pom.rrObject, ContextCreator.createOntologyContext(ontology))
      case _ => null
    }
  }

  def retrieveFallbackTemplateFromFallbackPoms(fallbacks : List[RMLPredicateObjectMap]) : ConditionalTemplate = {

    val analyzer = new StdTemplatesAnalyzer(ontology)

    val ontologyClass = fallbacks.find(fallback => {
      getOntologyClass(fallback) != null
    }).map(fallback => getOntologyClass(fallback)).orNull

    val templates = fallbacks.filter(fallback => {
      getOntologyClass(fallback) == null
    }).map(fallback => {
      analyzer.analyze(fallback)
    })

    val condPom = fallbacks.find(fallback => {
      fallback.isInstanceOf[RMLConditionalPredicateObjectMap]
    }).orNull.asInstanceOf[RMLConditionalPredicateObjectMap]

    val condition = if(condPom != null) {
      val function = condPom.equalCondition.getFunction
      getCondition(function)
    } else null

    val fallbackPom = if(condPom != null && condPom.fallbacks.nonEmpty) {
        retrieveFallbackTemplateFromFallbackPoms(condPom.fallbacks)
      } else null

    ConditionalTemplate(condition, templates, ontologyClass, fallbackPom)
  }

}
