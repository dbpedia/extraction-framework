package org.dbpedia.extraction.dataparser

import junit.framework.TestCase
import junit.framework.Assert._
import org.dbpedia.extraction.util.Language

class ParserUtilsTest extends TestCase
{
  def testConvertLargeNumbers() : Unit =
  {
    testConvert("en", "100.5 million", "100500000")
    testConvert("de", "100,5 million", "100500000")

    // testConvert("de", "1.234,5 mrd", "1234500000000")
    // FIXME: this should fail, mrd is not English
    // testConvert("en", "1,234.5 mrd", "1234500000000")

    // testConvert("en", "1,234.5 billion", "1234500000000")
    testConvert("de", "100,5 billion", "100500000000000")

    // testConvert("en", "1,234.5 trillion", "1234500000000000")
    // FIXME: this should work, trillion is 10^18 in German
    // testConvert("de", "1.234,5 trillion", "1234500000000000000000")
    testConvert("nl", "123 milja", "123 milja")
    testConvert("nl", "123 milj.", "123000000000")
  }

  private def testConvert( lang : String, value : String, expect : String ) : Unit =
  {
    val parser = new ParserUtils(new { def language = Language(lang) })
    assertEquals(expect, parser.convertLargeNumbers(value))
  }
}
