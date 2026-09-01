package org.dbpedia.extraction.wikiparser

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.dbpedia.extraction.util.Language

/**
 *
 */
@RunWith(classOf[JUnitRunner])
class WikiTitleTest extends FlatSpec with Matchers {

  "WikiTitle" should "return a wikilink to Commons:Test page in commonswiki" in {
    val language = Language("commons")
    val title = WikiTitle.parse("Commons:Test", language)
    title should equal (new WikiTitle("Test", Namespace.apply(language, "Commons"), language))
    title should not be 'interLanguageLink
  }

  it should "return a wikilink to Commons:Test page in commonswiki (leading colon)" in {
    val language = Language("commons")
    val title = WikiTitle.parse(":Commons:Test", language)
    title should equal (new WikiTitle("Test", Namespace.apply(language, "Commons"), language))
    title should not be 'interLanguageLink
  }

  it should "return an interwikilink to a Test page (Main namespace) in enwiki" in {
    val sourceLang = Language("commons")
    val destLang = Language("en")
    val title = WikiTitle.parse("en:Test", sourceLang)
    title should equal (new WikiTitle("Test", Namespace.Main, destLang, true))
    title should be an 'interLanguageLink
  }

  it should "return an wikilink to a Test page (Main namespace) in enwiki (leading colon)" in {
    val sourceLang = Language("commons")
    val destLang = Language("en")
    val title = WikiTitle.parse(":en:Test", sourceLang)
    title should equal (new WikiTitle("Test", Namespace.Main, destLang, false))
    title should not be 'interLanguageLink
  }

  it should "return a interwikilink to a Test page (Template namespace) in enwiki" in {
    val sourceLang = Language("commons")
    val destLang = Language("en")
    val title = WikiTitle.parse("en:Template:Test", sourceLang)
    title should equal (new WikiTitle("Test", Namespace.Template, destLang, true))
    title should be an 'interLanguageLink
  }

  it should "return a wikilink to a Test page (Template namespace) in enwiki (leading colon)" in {
    val sourceLang = Language("commons")
    val destLang = Language("en")
    val title = WikiTitle.parse(":en:Template:Test", sourceLang)
    title should equal (new WikiTitle("Test", Namespace.Template, destLang, false))
    title should not be 'interLanguageLink
  }

  it should "recognize the localized Amharic Module namespace" in {
    val language = Language("am")
    val title = WikiTitle.parse("ሞጁል:String", language)

    title.namespace should equal (Namespace.Module)
    title.decoded should equal ("String")
  }

  it should "keep the English Module alias for older Amharic dumps" in {
    val language = Language("am")
    val title = WikiTitle.parse("Module:String", language)

    title.namespace should equal (Namespace.Module)
    title.decoded should equal ("String")
  }
}
