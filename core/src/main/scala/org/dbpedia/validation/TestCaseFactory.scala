package org.dbpedia.validation

case class IriTrigger(id: TriggerReference, label: String, comment: String,
                       patterns: Array[String] /*TODO: or REGEX*/, triggers: Array[ValidatorReference])

case class IriValidator(id: ValidatorReference, hasScheme: String, hasQuery: Boolean,
                           hasFragment: Boolean, notContainsChars: Array[Char] /*TODO: or REGEX*/)

class TestCaseFactory {

  // TODO use InputStream
  def loadTestCases(pathToTestCaseFile: String): Unit = {

  }

  def getIriTriggers: Array[IriTrigger] = {


    null
  }

  def getIriValidators: Array[IriValidator] = {

    null
  }

}
