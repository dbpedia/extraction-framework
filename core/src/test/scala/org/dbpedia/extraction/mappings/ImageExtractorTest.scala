package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.ConfigUtils
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
    val res = ConfigUtils.loadImages(source, Language.English)

    res._1 should have size (1)
    res._2 should not be ('empty)
  }
}
