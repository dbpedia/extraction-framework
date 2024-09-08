package org.dbpedia.extraction.util
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class GeezNumberUtilsTest extends FlatSpec with Matchers {

  "ConvertGeezToArabicNumeral" should "return 2" in {
    convertGeezToArabicNumeral("፪") should equal(
      Some(2)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 10" in {
    convertGeezToArabicNumeral("፲") should equal(
      Some(10)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 107" in {
    convertGeezToArabicNumeral("፻፯") should equal(
      Some(107)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 113" in {
    convertGeezToArabicNumeral("፻፲፫") should equal(
      Some(113)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 898" in {
    convertGeezToArabicNumeral("፰፻፺፰") should equal(
      Some(898)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 1111111111" in {
    convertGeezToArabicNumeral("፲፩፼፲፩፻፲፩፼፲፩፻፲፩") should equal(
      Some(1111111111)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 1234" in {
    convertGeezToArabicNumeral("፲፪፻፴፬") should equal(
      Some(1234)
    )
  }
  "ConvertGeezToArabicNumeral" should "return 12345" in {
    convertGeezToArabicNumeral("፼፳፫፻፵፭") should equal(
      Some(12345)
    )
  }
  "ConvertGeezToArabicNumeral" should "return 7654321" in {
    convertGeezToArabicNumeral("፯፻፷፭፼፵፫፻፳፩") should equal(
      Some(7654321)
    )
  }
  "ConvertGeezToArabicNumeral" should "return 17654321" in {
    convertGeezToArabicNumeral("፲፯፻፷፭፼፵፫፻፳፩") should equal(
      Some(17654321)
    )
  }
  "ConvertGeezToArabicNumeral" should "return 51615131" in {
    convertGeezToArabicNumeral("፶፩፻፷፩፼፶፩፻፴፩") should equal(
      Some(51615131)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 3030" in {
    convertGeezToArabicNumeral("፴፻፴") should equal(
      Some(3030)
    )
  }

  "ConvertGeezToArabicNumeral" should "return 333333333" in {
    convertGeezToArabicNumeral("፫፼፴፫፻፴፫፼፴፫፻፴፫") should equal(
      Some(333333333)
    )
  }

  private val geezNumberParser = new GeezNumberUtils()

  private def convertGeezToArabicNumeral(input: String): Option[Int] = {

    geezNumberParser.convertGeezToArabicNumeral(input)

  }
}
