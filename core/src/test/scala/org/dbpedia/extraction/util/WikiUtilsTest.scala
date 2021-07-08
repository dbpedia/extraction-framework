package org.dbpedia.extraction.util
import org.scalatest.FunSuite

class WikiUtilsTest extends FunSuite {
  test("test removing broken brackets function") {
    val text = "Berlin (; German: [bɛʁˈliːn] ()) is the capital and largest city of Germany by both area and population."
    val expectedText = "Berlin is the capital and largest city of Germany by both area and population."
    val resultText = WikiUtil.removeBrokenBracketsInAbstracts(text)
    assert(resultText == expectedText)
  }
}
