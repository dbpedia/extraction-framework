package org.dbpedia.validation.construct

package object model {

  type TestCaseID = Int

  type TriggerID = Int
  type TriggerIRI = String

  object TriggerType extends Enumeration {

    val IRI, LITERAL, BLANK_NODE: Value = Value
  }

  type ValidatorID = Int
  type ValidatorIRI = String

  object ValidatorType extends Enumeration {

    val PATTERN_BASED, VOCAB_BASED, PART_BASED, GENERIC, TYPED_LITERAL: Value = Value
  }

  object ValidatorGroup extends Enumeration {

    val RIGHT,LEFT, DEFAULT: Value = Value
  }

  object TestCaseType extends Enumeration {

    val GENERIC, CUSTOM: Value = Value
  }

}
