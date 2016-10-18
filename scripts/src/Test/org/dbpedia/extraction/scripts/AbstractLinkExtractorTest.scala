package org.dbpedia.extraction.scripts

import java.io.File

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.mappings.AbstractLinkExtractor
import org.dbpedia.extraction.util.{RichFile, Language}
import org.scalatest.FunSuite

/**
  * Created by Chile on 10/17/2016.
  */
class AbstractLinkExtractorTest extends FunSuite {

  private val context = new {
    def ontology = throw new IllegalStateException("don't need Ontology for testing!!! don't call extract!")
    def language = Language.map.get("en").get
  }
  private val extractor = new AbstractLinkExtractor(context)

  private val inFile = new RichFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\enwiki\\20160305\\linked-abstracts-en.ttl"))
  private val outFile = new RichFile(new File("C:\\Users\\Chile\\Desktop\\Dbpedia\\core-i18n\\enwiki\\20160305\\linked-abstracts.ttl"))

  test("testExtractNif") {
    QuadMapper.mapQuads("nifTest", inFile, outFile){ quad: Quad =>
      extractor.extractNif("https://wikipedia.org/wiki/" + quad.subject.substring(quad.subject.indexOf("/resource/")+10), quad.subject, quad.value)
    }
  }
}
