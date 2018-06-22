package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.sources.{MemorySource, WikiPage}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiTitle}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers, PrivateMethodTester}

import scala.collection.mutable.{HashSet, Set => MutableSet}

/**
  *
  */
@RunWith(classOf[JUnitRunner])
class ImageExtractorTest extends FlatSpec with Matchers with PrivateMethodTester {

  "ImageExtractor" must "load 1 free image" in {
    val freeImages = new HashSet[String]()
    val nonFreeImages = new HashSet[String]()

    val loadImages = PrivateMethod[Unit]('loadImages)
    val source = new MemorySource(new WikiPage(new WikiTitle("Test.png", Namespace.File, Language("en")), "{{Free screenshot|template=BSD}}"))
    // def loadImages(source: Source, freeImages: MutableSet[String], nonFreeImages: MutableSet[String], wikiCode: String)
    ImageExtractor invokePrivate loadImages(source, freeImages, nonFreeImages, "en")

    freeImages should not be ('empty)
    freeImages should have size (1)
  }
}
