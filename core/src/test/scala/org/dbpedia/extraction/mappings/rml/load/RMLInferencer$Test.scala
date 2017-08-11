package org.dbpedia.extraction.mappings.rml.load

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.util.Resources
import org.dbpedia.extraction.util.Language
import org.scalatest.FunSuite

/**
  * Created by wmaroy on 11.08.17.
  */
class RMLInferencer$Test extends FunSuite {

}

object RMLInferencer$Test {

  val NAME = "Mapping_en:Infobox_person"
  val LOCATION = "/Mapping_en:Infobox_person.ttl"
  val LANGUAGE = "en"

  def getInferencedMappingExampleAsString : String = {
    val dump = Resources.getAsString(LOCATION)
    val name = NAME
    val language = LANGUAGE
    val inferencedMapping : String = RMLInferencer.loadDumpAsString(Language(language), dump, name)
    inferencedMapping
  }

  def getInferencedMappingExampleAsRMLModel : RMLModel = {
    val dump = getInferencedMappingExampleAsString
    RMLModel(LANGUAGE, NAME, dump)
  }

}
