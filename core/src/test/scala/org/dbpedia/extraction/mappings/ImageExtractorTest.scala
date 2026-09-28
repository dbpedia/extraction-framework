package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.ConfigUtils
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.sources.MemorySource
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

import scala.collection.mutable.{Set => MutableSet}

/**
 *
 */
@RunWith(classOf[JUnitRunner])
class ImageExtractorTest extends FlatSpec with Matchers with PrivateMethodTester {

  "ImageExtractor" must "load 1 free image" in {

    val loadImages = PrivateMethod[Unit]('loadImages)
    val source = new MemorySource(new WikiPage(new WikiTitle("Test.png", Namespace.File, Language("en")), "{{Free screenshot|template=BSD}}"))
    // def loadImages(source: Source, freeImages: MutableSet[String], nonFreeImages: MutableSet[String], wikiCode: String)
    val res = ConfigUtils.loadImages(source, "en")

    res._1 should have size (1)
    res._2 should not be ('empty)
  }

  // A wiki-markup image link broken across a line (e.g., [[File:\nBerlin_Map.png]]) used to have its
  // leading newline swept into the matched filename by ImageRegex, producing an invalid
  // foaf:depiction/thumbnail IRI with a literal newline in it (#774, #748).
  "ImageExtractorConfig.ImageRegex" must "not include a newline before the filename" in {

    val text = "File:\nBerlin_Map.png"
    val matched = ImageExtractorConfig.ImageRegex.findFirstIn(text)

    matched should be (Some("Berlin_Map.png"))
  }

  it must "still match an ordinary single-line filename" in {

    val text = "File:Berlin_Map.png"
    val matched = ImageExtractorConfig.ImageRegex.findFirstIn(text)

    matched should be (Some("Berlin_Map.png"))
  }
}
