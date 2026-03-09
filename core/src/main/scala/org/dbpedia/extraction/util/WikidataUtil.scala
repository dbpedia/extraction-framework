package org.dbpedia.extraction.util

import org.wikidata.wdtk.datamodel.implementation.MonolingualTextValueImpl
import org.wikidata.wdtk.datamodel.interfaces._

/**
 * Created by ali on 2/1/15.
 */
object WikidataUtil {
  // Constants to avoid SonarCloud duplicate literal warnings
  private val XSD_GYEAR = "xsd:gYear"
  private val XSD_STRING = "xsd:string"
  private val XSD_DATE = "xsd:date"
  private val XSD_GYEAR_MONTH = "xsd:gYearMonth"
  private val XSD_FLOAT = "xsd:float"
  private val XSD_DATETIME = "xsd:dateTime"
  
  def replacePunctuation(value:String,lang:String=""): String = {
    value.replace("(" + lang + ")", "").replace("[","").replace("]","").replace("\"","").trim()
  }

  def replaceItemId(item:String):String= {
    item.replace("(item)","").toString().trim()
  }

  def replaceString(str:String):String = {
    str.replace("(String)","").trim()
  }
  def replaceSpaceWithUnderscore(str: String): String = {
    str.replace(" ", "_")
  }
  def getItemId(value:Value) = value match {
    case v:ItemIdValue => replaceItemId(v.toString).replace(wikidataDBpNamespace,"")
    case _ => "V"+getHash(value)
  }

  def getUrl(value: Value): String = {
    value.toString.split(" ")(0)
  }
  def getId(value:Value): String = {
    value.toString.split(" ")(0).replace(WikidataUtil.wikidataDBpNamespace, "")
  }

  def getStatementUri(subject:String, property:String,value:Value):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value)
  }

  def getStatementUriWithHash(subject:String, property:String,value:Value,statementId:String):String = {
    subject+"_"+ property.replace(WikidataUtil.wikidataDBpNamespace, "").trim+"_" + getItemId(value) + "_" + getStatementHash(statementId)
  }

  def getStatementHash(statementId:String): String ={
    statementId.substring(statementId.indexOf("$")+1,statementId.indexOf("$")+6)
  }

  def getHash(value:Value):String={
    val hash_string = value.toString
    StringUtils.md5sum(hash_string).substring(0,5)
  }

  def getDatatype(value: Value): String = value match {
    case value: ItemIdValue => {
      null
    }
    case value: StringValue => {
      if (value.toString.contains("http://") || value.toString.contains("https://"))
        null
      else
        XSD_STRING
    }

    case value: TimeValue => {
      value.getPrecision match {
        case 14 => XSD_DATETIME     // second precision
        case 13 => XSD_DATETIME     // minute precision
        case 12 => XSD_DATETIME     // hour precision
        case 11 => XSD_DATE         // day precision
        case 10 => XSD_GYEAR_MONTH  // month precision
        case 9 => XSD_GYEAR         // year precision
        case 8 => XSD_GYEAR         // decade precision
        case 7 => XSD_GYEAR         // century precision
        case 6 => XSD_GYEAR         // millennium precision
        case 5 => XSD_GYEAR         // ten thousand years precision
        case 4 => XSD_GYEAR         // hundred thousand years precision
        case 3 => XSD_GYEAR         // million years precision
        case 2 => XSD_GYEAR         // ten million years precision
        case 1 => XSD_GYEAR         // hundred million years precision
        case 0 => XSD_GYEAR         // billion years precision
        case _ => XSD_GYEAR         // fallback to year
      }
    }
    case value: GlobeCoordinatesValue => {
      XSD_STRING
    }
    case value: QuantityValue => {
      XSD_FLOAT
    }
    case value : MonolingualTextValue => {
      XSD_STRING
    }
    case _=> null
  }
  
   // Get calendar model URI from TimeValue
  def getCalendarModel(value: Value): Option[String] = value match {
    case timeValue: TimeValue => Some(timeValue.getPreferredCalendarModel)
    case _ => None
  }

  /**
   * Check if date uses Julian calendar
   */
  def isJulianCalendar(calendarModelUri: String): Boolean = {
    calendarModelUri.contains("Q1985786") // Julian calendar item ID
  }

  /**
   * Convert Julian date to Gregorian using Julian Day Number as intermediate
   * Based on Fliegel and van Flandern (1968) algorithm
   * Handles all dates including BCE dates
   */
  private def julianToGregorian(year: Long, month: Byte, day: Byte): (Long, Byte, Byte) = {
    // Step 1: Convert Julian calendar date to Julian Day Number
    val jdn = julianCalendarToJDN(year, month, day)
    
    // Step 2: Convert Julian Day Number to Gregorian calendar date
    val (gregYear, gregMonth, gregDay) = jdnToGregorianCalendar(jdn)
    
    (gregYear, gregMonth, gregDay)
  }

  /**
   * Convert Julian calendar date to Julian Day Number
   * Valid for all dates producing JDN >= 0
   */
  private def julianCalendarToJDN(year: Long, month: Byte, day: Byte): Long = {
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    
    day + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
  }

  /**
   * Convert Gregorian calendar date to Julian Day Number
   * Valid for all dates producing JDN >= 0
   */
  private def gregorianCalendarToJDN(year: Long, month: Byte, day: Byte): Long = {
    val a = (14 - month) / 12
    val y = year + 4800 - a
    val m = month + 12 * a - 3
    
    day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
  }

  /**
   * Convert Julian Day Number to Gregorian calendar date
   */
  private def jdnToGregorianCalendar(jdn: Long): (Long, Byte, Byte) = {
    val a = jdn + 32044
    val b = (4 * a + 3) / 146097
    val c = a - (146097 * b) / 4
    val d = (4 * c + 3) / 1461
    val e = c - (1461 * d) / 4
    val m = (5 * e + 2) / 153
    
    val day = (e - (153 * m + 2) / 5 + 1).toByte
    val month = (m + 3 - 12 * (m / 10)).toByte
    val year = 100 * b + d - 4800 + m / 10
    
    (year, month, day)
  }

  /**
   * Format year value for XSD compliance, handling BCE dates
   */
  private def formatYear(year: Long): String = {
    if (year < 0) {
      // BCE dates: -469 becomes "-0469" for xsd:gYear
      f"${year}%05d"
    } else if (year == 0) {
      // Year 0 doesn't exist in Gregorian calendar, map to 1 BCE
      "-0001"
    } else {
      // Positive years need at least 4 digits
      f"${year}%04d"
    }
  }

  /**
   * Validate and format date components (day precision)
   */
  private def formatDate(year: Long, month: Byte, day: Byte): Option[String] = {
    // Validate month and day are in valid ranges
    if (month < 1 || month > 12) {
      println(s"WARNING: Invalid month $month for date $year-$month-$day, falling back to year only")
      return None
    }
    if (day < 1 || day > 31) {
      println(s"WARNING: Invalid day $day for date $year-$month-$day, falling back to year only")
      return None
    }
    
    val yearStr = if (year < 0) f"${year}%05d" else f"${year}%04d"
    Some(f"$yearStr-${month}%02d-${day}%02d")
  }

  /**
   * Format year-month, ensuring valid month
   */
  private def formatYearMonth(year: Long, month: Byte): Option[String] = {
    if (month < 1 || month > 12) {
      println(s"WARNING: Invalid month $month for year-month $year-$month, falling back to year only")
      return None
    }
    
    val yearStr = if (year < 0) f"${year}%05d" else f"${year}%04d"
    Some(f"$yearStr-${month}%02d")
  }

  /**
   * Format datetime with time components (hour, minute, second precision)
   */
  private def formatDateTime(year: Long, month: Byte, day: Byte, hour: Byte, minute: Byte, second: Byte, precision: Byte): Option[String] = {
    // First validate date components
    if (month < 1 || month > 12 || day < 1 || day > 31) {
      println(s"WARNING: Invalid date components for datetime $year-$month-$day, falling back to year only")
      return None
    }
    
    // Validate time components
    if (hour < 0 || hour > 23) {
      println(s"WARNING: Invalid hour $hour for datetime, falling back to date only")
      return formatDate(year, month, day)
    }
    if (minute < 0 || minute > 59) {
      println(s"WARNING: Invalid minute $minute for datetime, falling back to date only")
      return formatDate(year, month, day)
    }
    if (second < 0 || second > 59) {
      println(s"WARNING: Invalid second $second for datetime, falling back to date only")
      return formatDate(year, month, day)
    }
    
    val yearStr = if (year < 0) f"${year}%05d" else f"${year}%04d"
    val dateTimeStr = precision match {
      case 14 => f"$yearStr-${month}%02d-${day}%02dT${hour}%02d:${minute}%02d:${second}%02d"
      case 13 => f"$yearStr-${month}%02d-${day}%02dT${hour}%02d:${minute}%02d:00"
      case 12 => f"$yearStr-${month}%02d-${day}%02dT${hour}%02d:00:00"
      case _ => f"$yearStr-${month}%02d-${day}%02dT00:00:00"
    }
    Some(dateTimeStr)
  }

  /**
   * Calculate approximate year from large time spans
   */
  private def approximateYear(year: Long, precision: Byte): Long = {
    precision match {
      case 0 => (year / 1000000000) * 1000000000  // Billion years
      case 1 => (year / 100000000) * 100000000    // Hundred million years
      case 2 => (year / 10000000) * 10000000      // Ten million years
      case 3 => (year / 1000000) * 1000000        // Million years
      case 4 => (year / 100000) * 100000          // Hundred thousand years
      case 5 => (year / 10000) * 10000            // Ten thousand years
      case 6 => (year / 1000) * 1000              // Millennium
      case 7 => (year / 100) * 100                // Century
      case 8 => (year / 10) * 10                  // Decade
      case _ => year                               // Year or finer
    }
  }

  def getValue(value: Value): String = value match {
    case value: ItemIdValue => {
      WikidataUtil.replaceItemId(value.toString)
    }
    case value: StringValue => {
      WikidataUtil.replacePunctuation(replaceString(value.toString))
    }

    case value: TimeValue => {
      val year = value.getYear
      val month = value.getMonth
      val day = value.getDay
      val hour = value.getHour
      val minute = value.getMinute
      val second = value.getSecond
      val precision = value.getPrecision
      
      // Handle calendar conversion if needed (only for month precision and finer)
      // Year-only (precision 9) does not need conversion as the year number itself
      // doesn't change between Julian and Gregorian calendars  only the day offset matters
      val (finalYear, finalMonth, finalDay) = getCalendarModel(value) match {
        case Some(calendarUri) if isJulianCalendar(calendarUri) && precision >= 10 => {
          julianToGregorian(year, month, day)
        }
        case _ => (year, month, day) // Gregorian, unknown calendar, or year-only precision
      }
      
      precision match {
        case 14 | 13 | 12 => {
          // Second, minute, or hour precision
          formatDateTime(finalYear, finalMonth, finalDay, hour, minute, second, precision)
            .getOrElse(formatDate(finalYear, finalMonth, finalDay).getOrElse(formatYear(finalYear)))
        }
        case 11 => {
          // Day precision - must have valid date
          formatDate(finalYear, finalMonth, finalDay).getOrElse(formatYear(finalYear))
        }
        case 10 => {
          // Month precision - must have valid month
          formatYearMonth(finalYear, finalMonth).getOrElse(formatYear(finalYear))
        }
        case 9 => {
          // Year precision
          formatYear(year)
        }
        case 8 | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 => {
          // Decade, century, millennium, or larger time spans
          // Approximate to the appropriate scale
          val approxYear = approximateYear(year, precision)
          formatYear(approxYear)
        }
        case _ => {
          // Unknown precision - default to year
          formatYear(year)
        }
      }
    }
    case value: GlobeCoordinatesValue => {
      value.getLatitude + " " + value.getLongitude
    }

    case value: QuantityValue => {
      value.getNumericValue.toString
    }
    case value: MonolingualTextValue => {
      // Do we need value.getLanguageCode?
      value.getText
    }
    case value: PropertyIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: FormIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: LexemeIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case value: SenseIdValue => {
      getWikidataNamespace(value.getIri)
    }
    case _=> value.toString
  }
  def getWikiCommonsUrl(file: String): String = {
    val url = "http://commons.wikimedia.org/wiki/File:"+WikidataUtil.replaceSpaceWithUnderscore(file)
    url
  }
  def getWikidataNamespace(namespace: String): String = {
    namespace.replace(WikidataUtil.wikidataDBpNamespace, "http://www.wikidata.org/entity/")
  }

   lazy val wikidataDBpNamespace = Language("wikidata").resourceUri.namespace
}
