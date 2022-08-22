package org.dbpedia.extraction.dump

import org.dbpedia.extraction.dump.TestConfig.classLoader
import org.dbpedia.extraction.dump.tags.PostProcessingTestTag
import org.scalatest.{DoNotDiscover, FunSuite}
import org.dbpedia.extraction.scripts.{MapObjectUris, ResolveTransitiveLinks, TypeConsistencyCheck}

@DoNotDiscover
class PostProcessingTest extends FunSuite {

  test("resolve transitive links", PostProcessingTestTag) {
    ResolveTransitiveLinks.main(Array(
      "./target/minidumptest/base",
      "redirects",
      "redirects_transitive",
      ".ttl.bz2",
      "en,fr"
      ))
  }

  test("map object uris", PostProcessingTestTag) {

    MapObjectUris.main(Array(
      "./target/minidumptest/base",
      "redirects_transitive",
      ".ttl.bz2",
      "mappingbased-objects-uncleaned,disambiguations,infobox-properties,page-links,persondata,topical-concepts",
      "_redirected",
      ".ttl.bz2",
      "en,fr"
    ))
  }

  test("type consistency check", PostProcessingTestTag) {
    val propertyPath = classLoader.getResource("post-processing/type.consistency.check.properties").getFile
    TypeConsistencyCheck.main(Array(propertyPath))
  }
}
