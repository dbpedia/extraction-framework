package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class InfoboxExtractorConfigTest extends FlatSpec with Matchers {

  "InfoboxExtractorConfig" should "combine the baseline and language-specific ignored properties" in {
    InfoboxExtractorConfig.ignoreProperties.foreach { case (wikiCode, languageProperties) =>
      val ignored = InfoboxExtractorConfig.ignoredProperties(wikiCode)
      InfoboxExtractorConfig.ignoreProperties("en").subsetOf(ignored) should equal(true)
      languageProperties.subsetOf(ignored) should equal(true)
    }

    val amharicIgnored = InfoboxExtractorConfig.ignoredProperties("am")
    amharicIgnored should contain ("image")
    amharicIgnored should contain ("ሥዕል")
  }
}
