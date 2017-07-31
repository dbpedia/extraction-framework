package org.dbpedia.extraction.mappings.rml

import java.nio.file.{Files, Paths}

import org.dbpedia.extraction.mappings.rml.load.RMLInferencer
import org.dbpedia.extraction.util.Language
import org.scalatest.{FlatSpec, Matchers}
import org.scalatest.matchers.ShouldMatchers

/**
  * Created by wmaroy on 10.07.17.
  */
class RMLInferencerTest extends FlatSpec with Matchers
{

  "Execution " should " work for dirs that contain mappings" in {

    RMLInferencer.loadDir(Language("en"), "/home/wmaroy/github/leipzig/inferencing_test/")

  }

  "Execution " should " work for dumps" in {

    val path = this.getClass.getClassLoader.getResource("Mapping_en:Infobox_person.ttl").getPath
    val mappingDump = new String(Files.readAllBytes(Paths.get(path)), "UTF-8")

    val result = RMLInferencer.loadDump(Language("en"), mappingDump, "Mapping_en:Infobox_person")
    result

  }

}
