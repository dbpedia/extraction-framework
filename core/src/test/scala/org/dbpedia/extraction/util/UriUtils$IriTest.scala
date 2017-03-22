package org.dbpedia.extraction.util

import org.scalatest.FunSuite

/**
  * Created by chile on 22.03.17.
  */
class UriUtils$IriTest extends FunSuite {

  test("testEncodeIriComponent") {
    UriUtils.uriToIri("http:/example.org/äüö/  xtz")
  }

}
