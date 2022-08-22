package org.dbpedia.extraction.server.resources.rml.dbf

/**
  * Contains the functions for the dbf namespace
  */
object DbfFunction {

  private val _unitFunction = new {
    def name = "unitFunction"
    def unitParameter = "unitParameter"
    def valueParameter = "valueParameter"
  }

  private val _startDateFunction = new {
    def name = "startDateFunction"
    def startDateParameter = "startDateParameter"
  }

  private val _endDateFunction = new {
    def name = "endDateFunction"
    def endDateParameter = "endDateParameter"
  }

  private val _latFunction = new {
    def name = "latFunction"
    def latParameter = "latParameter"
    def latDegreesParameter = "latDegreesParameter"
    def latMinutesParameter = "latMinutesParameter"
    def latDirectionParameter = "latDirectionParameter"
  }

  private val _lonFunction = new {
    def name = "lonFunction"
    def lonParameter = "lonParameter"
    def lonDegreesParameter = "lonDegreesParameter"
    def lonMinutesParameter = "lonMinutesParameter"
    def lonDirectionParameter = "lonDirectionParameter"
  }

  private val _operatorFunction = new {
    def valueParameter = "valueParameter"
    def propertyParameter = "propertyParameter"
  }

  private val _simplePropertyFunction = new {
    def name = "simplePropertyFunction"
    def transformParameter = "transformParameter"
    def factorParameter = "factorParameter"
    def selectParameter = "selectParameter"
    def prefixParameter = "prefixParameter"
    def suffixParameter = "suffixParameter"
    def unitParameter = "unitParameter"
  }

  def unitFunction = _unitFunction

  def startDateFunction = _startDateFunction

  def endDateFunction = _endDateFunction

  def latFunction = _latFunction

  def lonFunction = _lonFunction

  def operatorFunction = _operatorFunction

  def simplePropertyFunction = _simplePropertyFunction


}
