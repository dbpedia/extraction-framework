package org.dbpedia.extraction.mappings.rml

import org.dbpedia.extraction.mappings.rml.load.RMLInferencer
import org.dbpedia.extraction.util.Language
import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers

/**
  * Created by wmaroy on 10.07.17.
  */
class RMLInferencerTest extends FlatSpec with ShouldMatchers
{

  "Execution " should " work" in {

    val inferencer = new RMLInferencer
    inferencer.load(Language("en"), "/home/wmaroy/github/leipzig/inferencing_test/")

  }

}
