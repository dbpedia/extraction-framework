package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.config.provenance.Dataset
import org.scalatest.FunSuite

/**
  * Created by Chile on 11/12/2016.
  */
class DatasetTest extends FunSuite {

  //val dataset = new Dataset("Long-Abstracts-temporary-wikidata-normalized", "some test description here", Language.English, null, null, DBpediaDatasets.LongAbstracts, Seq(DBpediaDatasets.PagesArticles))
val dataset = new Dataset("wiktionary.dbpedia.org")
  test("testVersionUri") {
    println(dataset.versionUri)
  }

  test("testEncodedWithLanguage") {
    println(dataset.encoded)
  }

  test("testDefaultUri") {
    println(dataset.languageUri)
  }

  test("testName") {
    println(dataset.name)
  }

  test("testCanonicalUri") {
    println(dataset.canonicalUri)
  }

  test("testVersion") {
    println(dataset.version.getOrElse(null))
  }

  override def convertToLegacyEqualizer[T](left: T): LegacyEqualizer[T] = ???

  override def convertToLegacyCheckingEqualizer[T](left: T): LegacyCheckingEqualizer[T] = ???
}
