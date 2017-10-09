package org.dbpedia.extraction.util

import org.dbpedia.extraction.config.Config
import org.scalatest.FunSuite

/**
  * Created by chile on 11.04.17.
  */
class ConfigUtils$Test extends FunSuite {

  //check if the right mapping languages are parsed (those with actual mappings)
  test("testParseLanguages") {
    val config = new Config("../scripts/type.consistency.check.properties")
    val languages = config.languages
    languages.length
  }

}
