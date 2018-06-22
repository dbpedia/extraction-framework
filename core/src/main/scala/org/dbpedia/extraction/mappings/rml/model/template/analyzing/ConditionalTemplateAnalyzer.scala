package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.mappings.rml.model.template._
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, RdfNamespace}

/**
  * Created by wmaroy on 12.08.17.
  */
class ConditionalTemplateAnalyzer(ontology: Ontology, ignore: Boolean = false) extends AbstractTemplateAnalyzer(ontology) {

  @throws(classOf[IllegalArgumentException])
  def apply(pom: RMLPredicateObjectMap): Template = {

    if (!pom.isInstanceOf[RMLConditionalPredicateObjectMap]) {
      throw new IllegalArgumentException("Predicate Object Map is not conditional.")
    }

    val condPom = pom.asInstanceOf[RMLConditionalPredicateObjectMap]
    val conditionFTM = condPom.equalCondition
    val function = conditionFTM.getFunction
    val fallbacks = condPom.fallbacks

    val condition = getCondition(function)
    val ontologyClass = getOntologyClass(pom)

    val ignoreURIPart = RMLUri.CONDITIONALMAPPING
    val analyzer = new StdTemplatesAnalyzer(ontology, ignoreURIPart)

    // if this is a classmapping do not search for templates
    val templates = if (ontologyClass != null) Seq() else Seq(analyzer.analyze(condPom))

    val fallbackTemplate = retrieveFallbackTemplateFromFallbackPoms(fallbacks)

    ConditionalTemplate(condition, templates, ontologyClass, fallbackTemplate)
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Private methods
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  def getCondition(function: Function) = {

    val conditionName = function.name
    val conditionValueParam = function.constants.getOrElse(conditionName + "/valueParameter", null)
    val conditionPropertyParam = function.references.getOrElse(conditionName + "/propertyParameter", null)

    val prefix = RdfNamespace.DBF.namespace

    val condition = conditionName.replaceFirst(prefix, "") match {
      case Condition.ISSET => IsSetCondition(conditionPropertyParam)
      case Condition.CONTAINS => ContainsCondition(conditionPropertyParam, conditionValueParam)
      case Condition.EQUALS => ContainsCondition(conditionPropertyParam, conditionValueParam)
      case Condition.OTHERWISE => OtherwiseCondition()
    }


    condition
  }

  def getOntologyClass(pom: RMLPredicateObjectMap): OntologyClass = {
    pom.rrPredicate match {
      case Property.TYPE => RMLOntologyUtil.loadOntologyClassFromIRI(pom.rrObject, ContextCreator.createOntologyContext(ontology))
      case _ => null
    }
  }

  /**
    *
    * @param fallbacks
    * @return A conditional template, returns null if the fallbacks parameter is an empty list
    */
  def retrieveFallbackTemplateFromFallbackPoms(fallbacks: List[RMLPredicateObjectMap]): ConditionalTemplate = {

    if (fallbacks.isEmpty) {
      // if not null is returned a ConditionalTemplate object with empty fields will be returned,
      // which is not wanted
      return null
    }

    val ignoreURIPart = RMLUri.CONDITIONALMAPPING
    val analyzer = new StdTemplatesAnalyzer(ontology, ignoreURIPart)

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

    val condition = if (condPom != null) {
      val function = condPom.equalCondition.getFunction
      getCondition(function)
    } else null

    val fallbackPom = if (condPom != null && condPom.fallbacks.nonEmpty) {
      retrieveFallbackTemplateFromFallbackPoms(condPom.fallbacks)
    } else null

    ConditionalTemplate(condition, templates, ontologyClass, fallbackPom)
  }

}
