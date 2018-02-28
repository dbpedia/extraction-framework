package org.dbpedia.extraction.wikiparser

import org.scalatest.FunSuite

class ParserFunctionsTest extends FunSuite {

  val trueNode = TextNode("true", 0)
  val falseNode = TextNode("false", 0)

  test("testIfEqFunc") {
    assert(ParserFunctions.ifEqFunc("1", "1", trueNode, falseNode).get == trueNode, "'1', '1' is not equal!")
    assert(ParserFunctions.ifEqFunc("01", "1", trueNode, falseNode).get == trueNode, "'1', '1' is not equal!")
  }

  test("testIfEqFunc$default$4") {

  }

  test("testIfFunc") {

  }

  test("testIfFunc$default$3") {

  }

  test("testIfErrorFunc") {

  }

  test("testIfErrorFunc$default$4") {

  }

  test("testExprFunc") {
    assert(ParserFunctions.exprFunc("1 == 1").toOption.get == 1d, "'1', '1' is not equal!")
    assert(ParserFunctions.exprFunc("01 == 2").toOption.get == 0d, "'1', '1' is not equal!")
    assert(ParserFunctions.exprFunc("  0.21 + 1.21").toOption.get == 1.42d, "'1', '1' is not equal!")
    assert(ParserFunctions.exprFunc("1 and 1").toOption.get == 1d, "'1', '1' is not equal!")
    assert(ParserFunctions.exprFunc(" 1 or 5").toOption.get == 1d, "'1', '1' is not equal!")
  }

  test("testIfExprFunc") {

  }

  test("testIfExprFunc$default$3") {

  }

}
