package org.dbpedia.extraction.dataparser
import java.util.logging.{Logger, Level}
import scala.util.matching.Regex
import org.dbpedia.extraction.config.dataparser.{
  EthiopianDateParserConfig,
  DateTimeParserConfig
}
import org.dbpedia.extraction.util.{Language, Date}
import org.dbpedia.extraction.util.{GeezNumberUtils}
import org.dbpedia.extraction.ontology.datatypes.Datatype

class EthiopianDateParser(datatype: Datatype, val strict: Boolean = false) {
  require(datatype != null, "datatype != null")
  @transient private val logger = Logger.getLogger(getClass.getName)

  val geezNumberParser = new GeezNumberUtils()
  private val monthsMap = EthiopianDateParserConfig.monthsMap
  private val monthsName = monthsMap.keys.mkString("|")
  private val geezNumberDate =
    EthiopianDateParserConfig.geezNumberDateMap.values.mkString("|")

  private val gregorianDateIndicator = s""".*(እ.ኤ.አ).*""".r
  private val prefix = if (strict) """\s*""" else """.*?"""
  private val postfix = if (strict) """\s*""" else ".*"

  // catches dd-mm-yyyy including a 13th month 21 13 2013, 21-13-2013, 21/13/2013, 21-13-2013, 21/13/2013
  private val dateRegex1: Regex =
    s"""$prefix\\b(0?[1-9]|[12][0-9]|3[01])\\b[-/\\s]\\b(0?[1-9]|1[0-2]|13)\\b[-/\\s](\\d{4}|[\\u1369-\\u137C]+)$postfix""".r

  // Regex for dates containing geez characters
  // catches dates like ጥቅምት-21-2013 or ጥቅምት/21/2013 or ጥቅምት 21 2013
  private val dateRegex2: Regex =
    s"""$prefix($monthsName)[\\s/-](\\b(0?[1-9]|[12][0-9]|3[01])\\b)[\\s/-](\\d{4}|[\\u1369-\\u137C]+)$postfix""".r

  // catches dates dd-month-yyyy like 21-ጥቅምት-2013 or 21/ጥቅምት/2013 or 21 ጥቅምት 2013
  private val dateRegex3: Regex =
    s"""$prefix(\\b(0?[1-9]|[12][0-9]|3[01])\\b)[\\s/-]($monthsName)[\\s/-](\\d{4}|[\\u1369-\\u137C]+)$postfix""".r

  // catches dates month-dd-yyyy ጥቅምት ፳፩ ፳፻፲፫ or ጥቅምት/፳፩/፳፻፲፫ or ጥቅምት ፳፩ ፳፻፲፫ mmmm-dd-yyyy
  private val dateRegex4: Regex =
    s"""$prefix(\\b$monthsName)[\\s/-]($geezNumberDate|0?[1-9]|[12][0-9]|3[01])[\\s/-](\\d{4}|[\\u1369-\\u137C]+)$postfix""".r

  // catches dates like ፳፩ ጥቅምት ፳፻፲፫ or ፳፩/ጥቅምት/፳፻፲፫ or 21/ጥቅምት/2013 dd-mmmm-yyyy
  private val dateRegex5: Regex =
    s"""$prefix(\\b$geezNumberDate|0?[1-9]|[12][0-9]|3[01])[\\s/-]($monthsName)[\\s/-](\\d{4}|[\\u1369-\\u137C]+)$postfix""".r

  def catchGeezDate(dateString: String): Option[(String, String, String)] = {

    for (dateRegex1(day, month, year) <- List(dateString)) {
      return Some((year, month, day))
    }

    // Amharic month names (month-day-year)
    for (dateRegex2(month, day, year) <- List(dateString)) {
      return Some((year, month, day))
    }

    // Amharic month names (day-month-year)
    for (dateRegex3(day, month, year) <- List(dateString)) {
      return Some((year, month, day))
    }

    // dates that contain geez/Amharic numbers (month-day-year)
    for (dateRegex4(month, day, year) <- List(dateString)) {
      return Some((year, month, day))
    }

    // dates that contain geez/Amharic numbers (day-month-year)
    for (dateRegex5(day, month, year) <- List(dateString)) {
      return Some((year, month, day))
    }

    None
  }

  def isLeapYear(year: Int): Boolean = {
    return (year % 4 == 3)
  }

  def isValidEthiopianCalendarDate(year: Int, month: Int, day: Int): Boolean = {
    // Validate year
    if (year <= 0) {
      logger.log(Level.FINE, "Year must be greater than 0.")
      return false
    }

    // Validate month
    if (month < 1 || month > 13) {
      logger.log(
        Level.FINE,
        s"Month must be between 1 and 13. Provided month: $month."
      )
      return false
    }

    // Validate day
    if (day < 1 || day > 30) {
      logger.log(
        Level.FINE,
        s"Day must be between 1 and 30. Provided day: $day."
      )
      return false
    }

    // Validate case for Pagume (month 13 in Ethiopian Calendar)
    if (month == 13) {
      if (day > 6) {
        logger.log(
          Level.FINE,
          s"Day in Pagume cannot exceed 6. Provided day: $day."
        )
        return false
      }
      if (!isLeapYear(year) && day > 5) {
        logger.log(
          Level.FINE,
          s"Pagume only has 5 days in non-leap years. Provided day: $day."
        )
        return false
      }
    }

    true
  }

  private def ethiopianDateToJDN(year: Int, month: Int, day: Int): Double = {
    val EPOCH: Long = 1723856
    val julianDayNumber: Double =
      (EPOCH + 365) + 365 * (year - 1) + (year / 4).toInt + 30 * month + day - 31
    return julianDayNumber
  }

  def geezToGregorianDateConverter(
      year: Int,
      month: Int,
      day: Int,
      datatype: Datatype
  ): Option[Date] = {
    val JDN: Double = ethiopianDateToJDN(year, month, day)
    val Q: Double = JDN + 0.5
    val Z: Long = Q.toLong
    val W: Long = ((Z - 1867216.25) / 36524.25).toLong
    val X: Long = (W / 4).toLong
    val A: Long = Z + 1 + W - X
    val B: Long = A + 1524
    val C: Long = ((B - 122.1) / 365.25).toLong
    val D: Long = (365.25 * C).toLong
    val E: Long = ((B - D) / 30.6001).toLong
    val F: Long = (30.6001 * E).toLong
    val gregorianDay: Int = (B - D - F + (Q - Z)).toInt
    val gregorianMonth: Long = if (E - 1 <= 12) E - 1 else E - 13
    val gregorianYear: Long = if (month <= 2) C - 4715 else C - 4716

    Some(
      new Date(
        Some(gregorianYear.toInt),
        Some(gregorianMonth.toInt),
        Some(gregorianDay.toInt),
        datatype
      )
    )
  }

  def isArabicNumeral(str: String): Boolean = {
    str.forall(c => c.isDigit)
  }

  def formatDate(
      dateString: Option[(String, String, String)]
  ): Option[(Int, Int, Int)] = {

    dateString.flatMap { case (year, month, day) =>
      val yearNum =
        if (isArabicNumeral(year)) year.toInt
        else geezNumberParser.convertGeezToArabicNumeral(year).getOrElse(0)
      val monthNum =
        if (isArabicNumeral(month)) month.toInt
        else {
          EthiopianDateParserConfig.monthsMap.getOrElse(
            month,
            geezNumberParser.convertGeezToArabicNumeral(month).getOrElse(0)
          )
        }
      val dayNum =
        if (isArabicNumeral(day)) day.toInt
        else geezNumberParser.convertGeezToArabicNumeral(day).getOrElse(0)

      return Some((yearNum, monthNum, dayNum))

    }
  }

  def findGeezDate(input: String): Option[Date] = {
    val isGregorianDate = (gregorianDateIndicator.findFirstIn(input)).isDefined

    if (isGregorianDate) {
      return None
    }

    val dateString: Option[(String, String, String)] = catchGeezDate(input)
    val (yearNum, monthNum, dayNum) =
      formatDate(dateString).getOrElse((0, 0, 0))

    if (!isValidEthiopianCalendarDate(yearNum, monthNum, dayNum)) {
      return None
    }

    for (
      date <- geezToGregorianDateConverter(yearNum, monthNum, dayNum, datatype)
    ) {

      return Some(date)
    }
    None

  }
}
