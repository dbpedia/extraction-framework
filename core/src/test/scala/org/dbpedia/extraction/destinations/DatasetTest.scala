package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.util.Language
import org.scalatest.FunSuite

/**
  * Created by Chile on 11/12/2016.
  */
class DatasetTest extends FunSuite {

val dataset = DBpediaDatasets.TestDataset.getLanguageVersion(Language("de"), "2016-10").get
  test("testVersionUri") {
    println(dataset.versionUri)
    println(dataset.canonicalVersion.versionUri)
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
