package org.dbpedia.extraction.server.resources.rml

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by wmaroy on 10.08.17.
  */
class MappingsTrackerRepoTest extends FlatSpec with Matchers {

  "getLanguageDirs" should "work correctly" in {

    val map = MappingsTrackerRepo.getLanguageDirs

    // map should not be empty
    assert(map.nonEmpty)

    // map should contain the "en" iso code as key
    assert(map.contains("en"))

    map.keys.foreach(key => println(key + " -> " + map(key).getAbsolutePath))

  }

  "getLanguageRMLModels" should "work correctly" in {

    // everything should get loaded without errors
    val languageRMLModels = MappingsTrackerRepo.getLanguageRMLModels

    // this should not be empty
    assert(languageRMLModels.nonEmpty)

    // this should contain the "en" iso code as key
    assert(languageRMLModels.contains("en"))

    languageRMLModels("en").foreach(entry => println(entry._1 + " : " + entry._2.name))

  }



}
