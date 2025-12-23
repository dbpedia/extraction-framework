package org.dbpedia.extraction.util
import org.dbpedia.extraction.util.abstracts.AbstractUtils
import org.scalatest.FunSuite

class AbstractUtilsTest extends FunSuite {
  test("test removing broken brackets function") {
    val text = "Berlin (; German: [bɛʁˈliːn] ()) is the capital and largest city of Germany by both area and population."
    val expectedText = "Berlin is the capital and largest city of Germany by both area and population."
    val resultText = AbstractUtils.removeBrokenBracketsInAbstracts(text)
    assert(resultText == expectedText)
  }
  test("test removing broken brackets function with only empty brackets") {
    val text = "Berlin () is the capital and largest () city of Germany by both area and population."
    val expectedText = "Berlin is the capital and largest city of Germany by both area and population."
    val resultText = AbstractUtils.removeBrokenBracketsInAbstracts(text)
    assert(resultText == expectedText)
  }
  test("test removing broken brackets function with bracket and semicolon") {
    val text = "Berlin (; German: [bɛʁˈliːn]) is the capital and largest city of Germany by both area and population."
    val expectedText = "Berlin is the capital and largest city of Germany by both area and population."
    val resultText = AbstractUtils.removeBrokenBracketsInAbstracts(text)
    assert(resultText == expectedText)
  }
  test("test removing broken brackets function with bracket and comma") {
    val text = "Berlin (, German: [bɛʁˈliːn]) is the capital and largest city of Germany by both area and population."
    val expectedText = "Berlin is the capital and largest city of Germany by both area and population."
    val resultText = AbstractUtils.removeBrokenBracketsInAbstracts(text)
    assert(resultText == expectedText)
  }
}
