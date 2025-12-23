package org.dbpedia.extraction.util

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.wikidata.wdtk.datamodel.implementation.TimeValueImpl

@RunWith(classOf[JUnitRunner])
class WikidataUtilPrecisionTest extends FunSuite {
  
  // Constants to fix SonarCloud warnings
  private val XSD_GYEAR = "xsd:gYear"
  private val XSD_DATETIME = "xsd:dateTime"
  
  val gregorian = "http://www.wikidata.org/entity/Q1985727"
  
  test("Precision 1: Hundred million years should return xsd:gYear") {
    val tv = new TimeValueImpl(-541000000L, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 1.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_GYEAR, s"Expected $XSD_GYEAR but got $datatype")
    println(s"✅ [1] Hundred million years: $datatype")
  }
  
  test("Precision 2: Ten million years should return xsd:gYear") {
    val tv = new TimeValueImpl(-66000000L, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 2.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_GYEAR, s"Expected $XSD_GYEAR but got $datatype")
    println(s"✅ [2] Ten million years: $datatype")
  }
  
  test("Precision 3: Million years should return xsd:gYear") {
    val tv = new TimeValueImpl(-2500000L, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 3.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_GYEAR, s"Expected $XSD_GYEAR but got $datatype")
    println(s"✅ [3] Million years: $datatype")
  }
  
  test("Precision 4: Hundred thousand years should return xsd:gYear") {
    val tv = new TimeValueImpl(-300000L, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 4.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_GYEAR, s"Expected $XSD_GYEAR but got $datatype")
    println(s"✅ [4] Hundred thousand years: $datatype")
  }
  
  test("Precision 5: Ten thousand years should return xsd:gYear") {
    val tv = new TimeValueImpl(-12000L, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 0.toByte, 5.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_GYEAR, s"Expected $XSD_GYEAR but got $datatype")
    println(s"✅ [5] Ten thousand years: $datatype")
  }
  
  test("Precision 12: Hour should return xsd:dateTime") {
    val tv = new TimeValueImpl(1969L, 7.toByte, 20.toByte, 20.toByte, 0.toByte, 0.toByte, 12.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_DATETIME, s"Expected $XSD_DATETIME but got $datatype")
    println(s"✅ [12] Hour: $datatype")
  }
  
  test("Precision 13: Minute should return xsd:dateTime") {
    val tv = new TimeValueImpl(1963L, 11.toByte, 22.toByte, 12.toByte, 30.toByte, 0.toByte, 13.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_DATETIME, s"Expected $XSD_DATETIME but got $datatype")
    println(s"✅ [13] Minute: $datatype")
  }
  
  test("Precision 14: Second should return xsd:dateTime") {
    val tv = new TimeValueImpl(2023L, 6.toByte, 15.toByte, 14.toByte, 30.toByte, 45.toByte, 14.toByte, 0, 0, 0, gregorian)
    val datatype = WikidataUtil.getDatatype(tv)
    assert(datatype == XSD_DATETIME, s"Expected $XSD_DATETIME but got $datatype")
    println(s"✅ [14] Second: $datatype")
  }
}