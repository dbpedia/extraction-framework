package org.dbpedia.extraction.dataparser

import junit.framework.TestCase
import junit.framework.Assert._
import org.dbpedia.extraction.util.Language

class ParserUtilsTest extends TestCase
{
  def testConvertLargeNumbers() : Unit =
  {
    testConvertLargeNumbers("en", "100.5 million", "100500000")
    testConvertLargeNumbers("de", "100,5 million", "100500000")

    // testConvertLargeNumbers("de", "1.234,5 mrd", "1234500000000")
    // FIXME: this should fail, mrd is not English
    // testConvertLargeNumbers("en", "1,234.5 mrd", "1234500000000")

    // testConvertLargeNumbers("en", "1,234.5 billion", "1234500000000")
    testConvertLargeNumbers("de", "100,5 billion", "100500000000000")

    // testConvertLargeNumbers("en", "1,234.5 trillion", "1234500000000000")
    // FIXME: this should work, trillion is 10^18 in German
    // testConvertLargeNumbers("de", "1.234,5 trillion", "1234500000000000000000")
    testConvertLargeNumbers("nl", "123 milja", "123 milja")
    testConvertLargeNumbers("nl", "123 milj.", "123000000000")
  }

  def testParse(): Unit = {
    testParse("en", ".1", 0.1)
  }

  private def testConvertLargeNumbers( lang : String, value : String, expect : String ) : Unit =
  {
    val parser = new ParserUtils(new { def language = Language(lang) })
    assertEquals(expect, parser.convertLargeNumbers(value))
  }

  private def testParse( lang : String, value : String, expect : Number ) : Unit =
  {
    val parser = new ParserUtils(new { def language = Language(lang) })
    assertEquals(expect, parser.parse(value))
  }
}
