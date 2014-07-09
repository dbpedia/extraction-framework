package org.dbpedia.extraction.mappings

import org.scalatest.{PrivateMethodTester, FlatSpec}
import org.scalatest.matchers.ShouldMatchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.collection.mutable.{Set => MutableSet, HashSet}
import org.dbpedia.extraction.sources.{WikiPage, MemorySource}
import org.dbpedia.extraction.wikiparser.{Namespace, WikiTitle}
import org.dbpedia.extraction.util.Language

/**
 *
 */
@RunWith(classOf[JUnitRunner])
class ImageExtractorTest extends FlatSpec with ShouldMatchers with PrivateMethodTester {

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
