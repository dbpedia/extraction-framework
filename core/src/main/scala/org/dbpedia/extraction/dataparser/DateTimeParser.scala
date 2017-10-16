package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.Datatype
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.dataparser.{DataParserConfig, DateTimeParserConfig}
import org.dbpedia.extraction.util.{Date, Language}
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology

import scala.language.reflectiveCalls

/**
 * Parses a data time.
 */
class DateTimeParser ( context : {
                            def language : Language
                            def ontology : Ontology
                            def redirects : Redirects },
                       datatype : Datatype,
                       val strict : Boolean = false) extends DataParser
{
    require(datatype != null, "datatype != null")

    protected val logger = Logger.getLogger(getClass.getName)

  //datatypes
  val dtDate = context.ontology.getOntologyDatatype("xsd:date").get
  val dtDay = context.ontology.getOntologyDatatype("xsd:gDay").get
  val dtMonth = context.ontology.getOntologyDatatype("xsd:gMonth").get
  val dtYear = context.ontology.getOntologyDatatype("xsd:gYear").get
  val dtYearMonth = context.ontology.getOntologyDatatype("xsd:gYearMonth").get
  val dtMonthDay = context.ontology.getOntologyDatatype("xsd:gMonthDay").get

    // language-specific configurations

    protected val language = if(DateTimeParserConfig.supportedLanguages.contains(context.language.wikiCode)) context.language.wikiCode else "en"

    protected val months = DateTimeParserConfig.monthsMap.getOrElse(language, DateTimeParserConfig.monthsMap("en"))
    protected val eraStr = DateTimeParserConfig.eraStrMap.getOrElse(language, DateTimeParserConfig.eraStrMap("en"))
    protected val cardinalityRegex = DateTimeParserConfig.cardinalityRegexMap.getOrElse(language, DateTimeParserConfig.cardinalityRegexMap("en"))
    protected val templates = DateTimeParserConfig.templateDateMap.getOrElse(language, Map())

    // parse logic configurations

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexDateTime.contains(language))
                                                    DataParserConfig.splitPropertyNodeRegexDateTime(language)
                                                  else
                                                    DataParserConfig.splitPropertyNodeRegexDateTime("en")

    protected val monthRegex = months.keySet.mkString("|")
    protected val eraRegex = eraStr.keySet.mkString("|")

    protected val prefix = if(strict) """\s*""" else """.*?"""
    protected val postfix = if(strict) """\s*""" else ".*"

    // catch dates like: "8 June 07" or "07 June 45"
    protected val DateRegex1 = ("""(?iu)""" + prefix + """([0-9]{1,2})\s*("""+monthRegex+""")\s*([0-9]{2})(?!\d)\s*(?!\s)(?!"""+ eraRegex +""").*""" + postfix).r

    // catch dates like: "[[29 January]] [[300 AD]]", "[[23 June]] [[2008]] (UTC)", "09:32, 6 March 2000 (UTC)" or "3 June 1981"
    protected val DateRegex2 = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*("""+monthRegex+""")\]?\]?,? \[?\[?(-?[0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?(?!\d)""" + postfix).r

    // catch dates like: "[[January 20]] [[1995 AD]]", "[[June 17]] [[2008]] (UTC)" or "January 20 1995"
    protected val DateRegex3 = ("""(?iu)""" + prefix + """\[?\[?("""+monthRegex+""")\s*,?\s+([0-9]{1,2})\]?\]?(?:""" + cardinalityRegex + """)?\s*[.,]?\s+\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?""" + postfix).r

    // catch dates like: "24-06-1867", "24/06/1867" or "bla24-06-1867bla"
    protected val DateRegex4 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/]([0-9]{1,2}+)[-/]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "24-june-1867", "24/avril/1867" or "bla24|juillet|1867bla"
    protected val DateRegex5 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/\|](""" + monthRegex + """)[-/\|]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "1990 06 24", "1990-06-24", "1990/06/24" or "1977-01-01 00:00:00.000000"
    protected val DateRegex6 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{3,4})[-/\s]([0-9]{1,2})[-/\s]([0-9]{1,2})(?!\d).*""").r

    // catch dates like: "20 de Janeiro de 1999", "[[1º de Julho]] de [[2005]]"
    protected val DateRegex7 = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*d?e?\s*(""" + monthRegex + """)\]?\]?\s*d?e?\s*\[?\[?([0-9]{0,4})\s*?\]?\]?(?!\d)""" + postfix).r

    // catch dates like: "1520, March 16"
    protected val DateRegex8 = ("""(?iu)""" + prefix + """([0-9]{3,4})[,]?\s+(""" + monthRegex + """)\s+([0-9]{1,2})(?:""" + cardinalityRegex + """)?\s*""").r

    protected val DayMonthRegex1 = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?\s*\[?\[?([1-9]|0[1-9]|[12][0-9]|3[01])(?!\d)""" + postfix).r

    protected val DayMonthRegex2 = ("""(?iu)""" + prefix + """(?<!\d)([1-9]|0[1-9]|[12][0-9]|3[01])\s*(""" + cardinalityRegex + """)?\]?\]?\s*(of)?\s*\[?\[?("""+monthRegex+""")\]?\]?""" + postfix).r

    protected val MonthYearRegex = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?,?\s*\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?""" + postfix).r

    protected val YearRegex = ("""(?iu)""" + prefix + """(?<![\d\pL\w])(-?\d{1,4})(?!\d)\s*(""" + eraRegex + """)?""" + postfix).r

    protected val YearRegex2 = ("""(?iu)""" + prefix + """(""" + eraRegex + """)(?<![\d])(\d{1,4})(?!\d)\s*""" + postfix).r

    override def parse(node : Node) : Option[ParseResult[Date]] =
    {
        try
        {
            for( child @ TemplateNode(_, _, _, _) <- node.children;
                 date <- catchTemplate(child))
            {
                return Some(ParseResult(date))
            }

            for(date <- findDate(nodeToString(node).trim))
            {
                return Some(ParseResult(date))
            }
        }
        catch
        {
            case ex : IllegalArgumentException  => logger.log(Level.FINE, "Error while parsing date", ex)
            case ex : NumberFormatException => logger.log(Level.FINE, "Error while parsing date", ex)
        }

        None
    }

    private def catchTemplate(node: TemplateNode) : Option[Date] =
    {
        val templateName = context.redirects.resolve(node.title).decoded.toLowerCase

        for(currentTemplate <- templates.get(templateName))
        {
            //check for text template e.g. Birth-date
            if (currentTemplate.keySet.contains("text")) {
                // find date in "text" value property
                for (property <- node.property(currentTemplate.getOrElse("text", ""));
                     TextNode(text, _, _) <- property.children)
                {
                    return findDate(text)
                }
            }
            else
            {
                var yearNum  = currentTemplate.getOrElse("year",  "")
                var monthNum = currentTemplate.getOrElse("month", "")
                var dayNum   = currentTemplate.getOrElse("day",   "")

                //check for conditional mapping
                if (currentTemplate.keySet.contains("ifPropertyNum"))
                {
                    //evaluate !if
                    val propNum = currentTemplate.getOrElse("ifPropertyNum", "")
                    val propVal = currentTemplate.getOrElse("ifPropertyNumHasValue", "")

                    for (property <- node.property(propNum);
                         TextNode(text, _, _) <- property.children)
                    {
                        if (text !=  propVal)
                        {
                            yearNum  = currentTemplate.getOrElse("elseYear",  "")
                            monthNum = currentTemplate.getOrElse("elseMonth", "")
                            dayNum   = currentTemplate.getOrElse("elseDay",   "")
                        }

                    }
                }

                return getDateByParameters(node, yearNum, monthNum, dayNum)
            }
        }

        logger.log(Level.FINE, "Template unknown: " + node.title)
        None
    }

    protected def getDateByParameters(node: TemplateNode, yearNum: String, monthNum: String, dayNum: String): Option[Date] = {
        // get values from defined year month day
      val year = node.property(yearNum) match{
        case Some(yearProperty) => yearProperty.children.collect { case TextNode(text, _, _) => text }.headOption match{
            case Some(yy) => yy match {
              case YearRegex(y, era) =>
                val eraIdentifier = getEraSign(era)
                Some((eraIdentifier + y).toInt)
              case YearRegex(y) => Some(y.toInt)
              case YearRegex2(era, y) =>
                val eraIdentifier = getEraSign(era)
                Some((eraIdentifier + y).toInt)
              case _ => None
            }
            case None => None
        }
        case None => None
      }
        val month = node.property(monthNum) match {
          case Some(monthProperty) => monthProperty.children.collect { case TextNode(text, _, _) => text }.headOption match {
            case Some(m) => months.get(m.toLowerCase) match {
              case Some(s) => Some(s)
              case None => Some(m.toInt)
            }
            case None => None
          }
          case None => None
        }
        val day = node.property(dayNum) match{
            case Some(dayProperty) => dayProperty.children.collect { case TextNode(text, _, _) => text }.headOption match{
              case Some(d) => Some(d.toInt)
              case None => None
            }
            case None => None
        }
        //year can contain era
      var dt = datatype
      day match{
        case None => dt = dtYearMonth
        case Some(_) =>
      }
      month match{
        case None => dt = dtYear
        case Some(_) =>
      }
      if(year.nonEmpty)
        Some(new Date(year, month, day, dt))
      else
        None
    }

    private def findDate(input: String) : Option[Date] =
    {
        for(date <- catchDate(input))
        {
            return Some(date)
        }

        datatype match
        {
            case `dtDay` =>
                logger.fine("Method for day Extraction not yet implemented.")
                None
            case `dtMonth` =>
                logger.fine("Method for month Extraction not yet implemented.")
                None
            case `dtYear` =>
                for(date <- catchMonthYear(input))
                {
                    return Some(date)
                }
                catchYear(input)
            case `dtMonthDay` =>
                catchDayMonth(input)
            case `dtYearMonth` =>
                catchMonthYear(input)
            case _ => None
        }
    }

    /**
     * Finds year, month and day of a provided string
     *
     * Provided Data might be a Date like: [[January 20]] [[2001]], [[1991-10-25]] or 3 June 1981
     * Returns a normalized Date value (eg: 1984-01-29) if a Date is found in the string, NULL otherwise.
     *
     * @param    string    $input    Literaltext, that matched to be a Date
     *             string    $language language of Literaltext, eg: 'en' or 'de'
     * @return     string    Date or NULL
     */
    private def catchDate(input: String) : Option[Date] =
    {
        for(DateRegex1(day, month, year) <- List(input))
        {
            // the century (1900 or 2000) depends on the last 2-digit number in the inputstring: >20 -> 1900
            // TODO: replace with more flexible test
            var century = "20"
            if (year.toInt > 20)
            {
                century = "19"
            }
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(Some((century+year).toInt), Some(monthNumber.toInt), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex2(day, dunno, month, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex3(month, day, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex4(day, month, year) <- List(input))
        {
            return Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
        }

        for(DateRegex5(day, month, year) <- List(input))
        {
            try
            {
                val monthNumber = months(month.toLowerCase)
                return Some(new Date(Some(year.toInt), Some(monthNumber), Some(day.toInt), datatype))
            }
            catch
            {
                case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex6(year, month, day) <- List(input))
        {
            return Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
        }

        for(DateRegex7(day, month, year) <- List(input))
        {
            return Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
        }

        for(DateRegex8(year, month, day) <- List(input))
        {
            months.get(month.toLowerCase) match
            {
              case Some(monthNumber) => return Some(new Date(Some(year.toInt), Some(monthNumber), Some(day.toInt), datatype))
              case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

      catchMonthYear(input) match{
        case Some(d) => Some(new Date(year = d.year, month = d.month, day= Some(1), datatype = dtDate))
        case None => catchYear(input) match{
          case Some(d) => Some(new Date(year = d.year, month = Some(1), day= Some(1), datatype = dtDate))
          case None => None
        }
      }
    }

    private def catchDayMonth(input: String) : Option[Date] =
    {
        for(result <- DayMonthRegex1.findFirstMatchIn(input))
        {
            val month = result.group(1)
            val day = result.group(2)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = dtMonthDay))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }
        for(result <- DayMonthRegex2.findFirstMatchIn(input))
        {
            val day = result.group(1)
            val month = result.group(4)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = dtMonthDay))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }
        None
    }

    private def catchMonthYear(input: String) : Option[Date] =
    {
        for(result <- MonthYearRegex.findFirstMatchIn(input))
        {
            val month = result.group(1)
            val year = result.group(2)
            val era = result.group(3)
            val eraIdentifier = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some(new Date(year = Some((eraIdentifier+year).toInt), month = Some(monthNumber), datatype = dtYearMonth))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }
        None
    }

    private def catchYear(input: String) : Option[Date] =
    {
        for(result <- YearRegex.findFirstMatchIn(input))
        {
            val year = result.group(1)
            val eraIdentifier = getEraSign(result.group(2))
            return Some(new Date(year = Some((eraIdentifier+year).toInt), datatype = dtYear))
        }
        for(result <- YearRegex2.findFirstMatchIn(input))
        {
            val year = result.group(2)
            val eraIdentifier = getEraSign(result.group(1))
            return Some(new Date(year = Some((eraIdentifier+year).toInt), datatype = dtYear))
        }
        None
    }

    private def nodeToString(node : Node) : String = node match
    {
        case TextNode(text, _, _) => text
        case _ => node.children.map(nodeToString).mkString
    }

    private def getEraSign(input : String) : String =
    {
        if (input == null) return ""

        // '.' is used in regex as '\\.'
        val tmpInp = input.replace(".", "\\.").toLowerCase

        for ( (key, value) <- eraStr if value == (-1) )
        {
            if (key.toLowerCase == tmpInp.substring(0, math.min(key.length,tmpInp.length)) )
            {
                return "-"
            }
        }
        ""
    }
}