package org.dbpedia.extraction.dataparser

import _root_.org.dbpedia.extraction.sources.WikiPage
import _root_.org.scalatest.matchers.ShouldMatchers
import org.scalatest.FlatSpec
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import java.net.URI

@RunWith(classOf[JUnitRunner])
class LinkParserTest extends FlatSpec with ShouldMatchers
{
  /**
   *  {{ URL }}	{{URL|example.com|optional display text}}
        {{ URL | }}	{{URL|example.com|optional display text}}
        {{ URL | EXAMPLE.com }}
        {{ URL | example.com }}
        {{ URL | www.example.com }}	www.example.com
        {{ URL | http://www.example.com }}	www.example.com
        {{ URL | https://www.example.com }}	www.example.com
        {{ URL | ftp://www.example.com }}	www.example.com
        {{ URL | ftp://ftp.example.com }}	ftp.example.com
        {{ URL | http://www.example.com/ }}	www.example.com
        {{ URL | http://www.example.com/path }}	www.example.com/path
        {{ URL | irc://irc.example.com/channel }}	irc.example.com/channel
        {{ URL | www.example.com/foo }}	www.example.com/foo
        {{ URL | http://www.example.com/path/ }}	www.example.com/path/
        {{ URL | www.example.com/foo/ }}	www.example.com/foo/
        {{ URL | 1=http://www.example.com/path?section=17 }}	www.example.com/path?section=17
        {{ URL | 1=www.example.com/foo?page=42 }}	www.example.com/foo?page=42
        {{ URL | http://www.example.com/foo | link }}	link (Deprecated)
        {{ URL | www.example.com/foo | link }}	link (Deprecated)
        {{ URL | http://www.example.com/foo/ | link }}	link (Deprecated)
        {{ URL | www.example.com/foo/ | link }}	link (Deprecated)
   */
  "LinkParser" should "http://www.example.com/" in {
    parse("{{URL|http://www.example.com/}}") should equal (Some(build("http://www.example.com/")))
  }

  it should "return http://www.example.com/path" in {
    parse("{{URL|http://www.example.com/path}}") should equal (Some(build("http://www.example.com/path")))
  }

  it should "return irc://irc.example.com/channel" in {
    parse("{{URL|irc://irc.example.com/channel}}") should equal (Some(build("irc://irc.example.com/channel")))
  }

  it should "return http://www.example.com/path/" in {
    parse("{{URL|http://www.example.com/path/}}") should equal (Some(build("http://www.example.com/path/")))
  }

  it should "return http://www.example.com/path?section=17" in {
    parse("{{URL|1=http://www.example.com/path?section=17}}") should equal (Some(build("http://www.example.com/path?section=17")))
  }

  it should "return http://www.example.com/foo?page=42" in {
    parse("{{URL|1=www.example.com/foo?page=42}}") should equal (Some(build("http://www.example.com/foo?page=42")))
  }

  it should "return http://example.com" in {
    parse("{{URL|example.com}}") should equal (Some(build("http://example.com")))
  }

  it should "return http://EXAMPLE.COM" in {
    parse("{{URL|EXAMPLE.com}}") should equal (Some(build("http://EXAMPLE.COM")))
  }

  it should "return http://www.example.com" in {
    parse("{{URL|www.example.com}}") should equal (Some(build("http://www.example.com")))
    parse("{{URL|http://www.example.com}}") should equal (Some(build("http://www.example.com")))
  }

  it should "return https://www.example.com" in {
    parse("{{URL|https://www.example.com}}") should equal (Some(build("https://www.example.com")))
  }

  it should "return ftp://www.example.com" in {
    parse("{{URL|ftp://www.example.com}}") should equal (Some(build("ftp://www.example.com")))
  }

  it should "return ftp://ftp.example.com" in {
    parse("{{URL|ftp://ftp.example.com}}") should equal (Some(build("ftp://ftp.example.com")))
  }

  it should "return http://www.example.com/foo" in {
    parse("{{URL|www.example.com/foo}}") should equal (Some(build("http://www.example.com/foo")))
    parse("{{URL|http://www.example.com/foo|link}}") should equal (Some(build("http://www.example.com/foo")))
    parse("{{URL|www.example.com/foo|link}}") should equal (Some(build("http://www.example.com/foo")))
  }

  it should "return http://www.example.com/foo/" in {
    parse("{{URL|www.example.com/foo/|link}}") should equal (Some(build("http://www.example.com/foo/")))
    parse("{{URL|http://www.example.com/foo/|link}}") should equal (Some(build("http://www.example.com/foo/")))
    parse("{{URL|www.example.com/foo/}}") should equal (Some(build("http://www.example.com/foo/")))
  }

  private val parser = WikiParser.getInstance()
  private val notStrictParser = new LinkParser(strict = false)

  private def build(uri: String) : URI = {
    URI.create(uri)
  }

  private def parse(input : String) : Option[URI] =
  {
    val page = new WikiPage(WikiTitle.parse("TestPage", Language.English), input)

    // Not strict parsing
    parser(page) match {
      case Some(n) => notStrictParser.parse(n)
      case None => None
    }
  }
}