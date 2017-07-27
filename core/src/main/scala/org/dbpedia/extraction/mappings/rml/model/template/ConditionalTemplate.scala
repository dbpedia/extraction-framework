package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyClass

/**
  * Created by wmaroy on 24.07.17.
  */
case class ConditionalTemplate(condition : Condition, templates : Seq[Template], ontologyClass: OntologyClass, fallback : ConditionalTemplate) extends Template(ConditionalTemplate.NAME)

class Condition(operator: String)

case class IsSetCondition(property: String) extends Condition(Condition.ISSET)

case class ContainsCondition(property: String, value: String) extends Condition(Condition.CONTAINS)

case class EqualsCondition(property: String, value: String) extends Condition(Condition.EQUALS)

case class OtherwiseCondition() extends Condition(Condition.OTHERWISE)

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