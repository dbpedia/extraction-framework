package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyClass

/**
  * Created by wmaroy on 24.07.17.
  *
  * @param condition
  * @param templates
  * @param ontologyClass
  * @param fallback
  */
case class ConditionalTemplate(condition: Condition, templates: Seq[Template], ontologyClass: OntologyClass, fallback: ConditionalTemplate) extends Template(ConditionalTemplate.NAME) {

  def hasFallback: Boolean = {
    fallback != null
  }

  def hasCondition: Boolean = {
    condition != null
  }

  def hasClass: Boolean = {
    ontologyClass != null
  }

}

class Condition(val operator: String, val property: String, val value: String)

case class IsSetCondition(override val property: String) extends Condition(Condition.ISSET, property, null)

case class ContainsCondition(override val property: String, override val value: String) extends Condition(Condition.CONTAINS, property, value)

case class EqualsCondition(override val property: String, override val value: String) extends Condition(Condition.EQUALS, property, value)

case class OtherwiseCondition() extends Condition(Condition.OTHERWISE, property = null, value = null)

object Condition {

  val CONTAINS = "Contains"
  val EQUALS = "Equals"
  val OTHERWISE = "Otherwise"
  val ISSET = "IsSet"
  val ILLEGALARGUMENTMSG = "Condition operator cannot be found. Please select one of the following: " +
    Condition.OTHERWISE + ", " +
    Condition.CONTAINS + ", " +
    Condition.EQUALS + ", " +
    Condition.ISSET
}

object ConditionalTemplate {

  val NAME = "ConditionalTemplate"

}