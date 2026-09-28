package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}

@RunWith(classOf[JUnitRunner])
class EthiopianDateParserTest extends FlatSpec with Matchers {

  private val datatype = new Datatype("xsd:date")
  private val parser = new EthiopianDateParser(datatype, strict = true)

  private def parse(input: String): Option[String] =
    parser.findGeezDate(input).map(_.toString)

  "EthiopianDateParser" should "convert every Ethiopian month to the correct Gregorian year" in {
    val conversions = Seq(
      (2013, 1, 1, "2020-09-11"),
      (2013, 2, 1, "2020-10-11"),
      (2013, 3, 1, "2020-11-10"),
      (2013, 4, 1, "2020-12-10"),
      (2013, 5, 1, "2021-01-09"),
      (2013, 6, 1, "2021-02-08"),
      (2013, 7, 1, "2021-03-10"),
      (2013, 8, 1, "2021-04-09"),
      (2013, 9, 1, "2021-05-09"),
      (2013, 10, 1, "2021-06-08"),
      (2013, 11, 1, "2021-07-08"),
      (2013, 12, 1, "2021-08-07"),
      (2013, 13, 1, "2021-09-06")
    )

    conversions.foreach { case (year, month, day, expected) =>
      parser.geezToGregorianDateConverter(year, month, day, datatype)
        .map(_.toString) should equal(Some(expected))
    }
  }

  it should "parse the standard Amharic day-marker format as written" in {
    val articleDates = Seq(
      "መስከረም ፳ ቀን ፲፱፻፶፱ ዓ.ም." -> "1966-09-30",
      "ጥቅምት ፪ ቀን ፲፱፻፷፩ ዓ.ም." -> "1968-10-12",
      "ጥር ፲፫ ቀን ፲፱፻፲፩ ዓ.ም." -> "1919-01-21",
      "የካቲት ፲፩ ቀን ፲፱፻፶፯ ዓ.ም." -> "1965-02-18"
    )

    articleDates.foreach { case (input, expected) =>
      parse(input) should equal(Some(expected))
    }
  }

  it should "recognize supported Ethiopian era markers" in {
    val eraMarkers = Seq("ዓ.ም.", "ዓ.ም", "ዓ/ም", "ዓመተ ምሕረት", "ዓመተ ምኅረት", "አ.ም.")

    eraMarkers.foreach { marker =>
      parse(s"መስከረም 1 ቀን 2013 $marker") should equal(Some("2020-09-11"))
    }
  }

  it should "recognize observed Ethiopian month spelling variants" in {
    val monthVariants = Seq(
      "ኅዳር" -> "2020-11-10",
      "ህዳር" -> "2020-11-10",
      "ኀዳር" -> "2020-11-10",
      "ታኅሣሥ" -> "2020-12-10",
      "ታኅሳስ" -> "2020-12-10",
      "ታህሳስ" -> "2020-12-10",
      "ታኅሣስ" -> "2020-12-10",
      "ሚያዚያ" -> "2021-04-09",
      "ሓምሌ" -> "2021-07-08",
      "ሃምሌ" -> "2021-07-08",
      "ኃምሌ" -> "2021-07-08",
      "ነሓሴ" -> "2021-08-07",
      "ነሃሴ" -> "2021-08-07",
      "ጳጉሜን" -> "2021-09-06"
    )

    monthVariants.foreach { case (month, expected) =>
      parse(s"$month 1 2013") should equal(Some(expected))
    }
  }

  it should "parse Arabic and single-digit Geez days in both supported orders" in {
    parse("ጥቅምት 21 2013") should equal(Some("2020-10-31"))
    parse("21 ጥቅምት 2013") should equal(Some("2020-10-31"))
    parse("ታኅሣሥ ፪ ቀን ፲፱፻፶፮ ዓ.ም.") should equal(Some("1963-12-12"))
  }
}
