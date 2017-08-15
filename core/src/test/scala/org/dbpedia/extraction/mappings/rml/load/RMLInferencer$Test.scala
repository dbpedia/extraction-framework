package org.dbpedia.extraction.mappings.rml.load

import java.nio.file.{Files, Paths}

import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.util.Resources
import org.dbpedia.extraction.util.Language
import org.scalatest.FunSuite
import org.slf4j
import org.slf4j.LoggerFactory

/**
  * Created by wmaroy on 11.08.17.
  */
class RMLInferencer$Test extends FunSuite {

  test("loadingOfAllMappings") {

    // Sets the package level to INFO
    Logger.getRootLogger.setLevel(Level.OFF)
    val map =Language.map
    val languages = map.values.toList.map(language => language.isoCode).distinct

    languages.map(lang => {

      Logger.getLogger(this.getClass).info("Loading language dir: " + lang)
      println("////////////////////////////////////////////////////////////////////////////////////////////////////////////////////\n" +
        "//" + " Loading language dir: " + lang + "\n" +
      "////////////////////////////////////////////////////////////////////////////////////////////////////////////////////\n")

      try {
        (lang, RMLInferencer.loadDir(Language(lang), "../mappings-tracker/mappings/"))
      } catch {
        case e : Exception => e.printStackTrace(); (lang, null)
      }
    }).toMap

  }

  test("Execution for dirs that contain mappings") {

    RMLInferencer.loadDir(Language("en"), "/home/wmaroy/github/leipzig/inferencing_test/")

  }

  test("Execution work for dumps") {

    val path = this.getClass.getClassLoader.getResource("Mapping_en:Infobox_person.ttl").getPath
    val mappingDump = new String(Files.readAllBytes(Paths.get(path)), "UTF-8")

    val result = RMLInferencer.loadDump(Language("en"), mappingDump, "Mapping_en:Infobox_person")
    result

  }


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
