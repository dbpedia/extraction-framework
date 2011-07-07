package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.Datatype
import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.dataparser.DateTimeParserConfig
import org.dbpedia.extraction.util.{Language, Date}
import org.dbpedia.extraction.mappings.Redirects

/**
 * Parses a data time.
 */
class DateTimeParser ( context : {
                            def language : Language
                            def redirects : Redirects },
                       datatype : Datatype,
                       val strict : Boolean = false) extends DataParser
{
    require(datatype != null, "datatype != null")

    private val logger = Logger.getLogger(classOf[UnitValueParser].getName)

    // language-specific configurations

    private val language = if(DateTimeParserConfig.supportedLanguages.contains(context.language.wikiCode)) context.language.wikiCode else "en"

    private val months = DateTimeParserConfig.monthsMap.getOrElse(language, DateTimeParserConfig.monthsMap("en"))
    private val eraStr = DateTimeParserConfig.eraStrMap.getOrElse(language, DateTimeParserConfig.eraStrMap("en"))
    private val cardinalityRegex = DateTimeParserConfig.cardinalityRegexMap.getOrElse(language, DateTimeParserConfig.cardinalityRegexMap("en"))
    private val templates = DateTimeParserConfig.templateDateMap.getOrElse(language, Map())

    // parse logic configurations

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete

    private val monthRegex = months.keySet.mkString("|")
    private val eraRegex = eraStr.keySet.mkString("|")

    private val prefix = if(strict) """\s*""" else """.*?"""
    private val postfix = if(strict) """\s*""" else ".*"

    // catch dates like: "8 June 07" or "07 June 45"
    private val DateRegex1 = ("""(?iu)""" + prefix + """([0-9]{1,2})\s*("""+monthRegex+""")\s*([0-9]{2})(?!\d).*""" + postfix).r

    // catch dates like: "[[29 January]] [[300 AD]]", "[[23 June]] [[2008]] (UTC)", "09:32, 6 March 2000 (UTC)" or "3 June 1981"
    private val DateRegex2 = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*("""+monthRegex+""")\]?\]?,? \[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?(?!\d)""" + postfix).r

    // catch dates like: "[[January 20]] [[1995 AD]]", "[[June 17]] [[2008]] (UTC)" or "January 20 1995"
    private val DateRegex3 = ("""(?iu)""" + prefix + """\[?\[?("""+monthRegex+""")\s*,?\s+([0-9]{1,2})\]?\]?\s*[.,]?\s+\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?""" + postfix).r

    // catch dates like: "24-06-1867", "24/06/1867" or "bla24-06-1867bla"
    private val DateRegex4 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/]([0-9]{1,2}+)[-/]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "24-june-1867", "24/avril/1867" or "bla24|juillet|1867bla"
    private val DateRegex5 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/\|](""" + monthRegex + """)[-/\|]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "1990 06 24", "1990-06-24", "1990/06/24" or "1977-01-01 00:00:00.000000"
    private val DateRegex6 = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{3,4})[-/\s]([0-9]{1,2})[-/\s]([0-9]{1,2})(?!\d).*""").r

    // catch dates like: "20 de Janeiro de 1999", "[[1º de Julho]] de [[2005]]"
    private val DateRegex7 = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*d?e?\s*(""" + monthRegex + """)\]?\]?\s*d?e?\s*\[?\[?([0-9]{0,4})\s*?\]?\]?(?!\d)""" + postfix).r

    private val DayMonthRegex1 = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?\s*\[?\[?([1-9]|0[1-9]|[12][0-9]|3[01])(?!\d)""" + postfix).r

    private val DayMonthRegex2 = ("""(?iu)""" + prefix + """(?<!\d)([1-9]|0[1-9]|[12][0-9]|3[01])\s*(""" + cardinalityRegex + """)?\]?\]?\s*(of)?\s*\[?\[?("""+monthRegex+""")\]?\]?""" + postfix).r

    private val MonthYearRegex = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?,?\s*\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?""" + postfix).r

    //added case insensitive match
    private val YearRegexes = for(i <- (1 to 4).reverse) yield ("""(?iu)""" + prefix + """(?<![\d\pL\w])(\d{""" + i + """})(?!\d)\s*(""" + eraRegex + """)?""" + postfix).r


    override def parse(node : Node) : Option[Date] =
    {
        try
        {
            for( child @ TemplateNode(_, _, _) <- node.children;
                 date <- catchTemplate(child))
            {
                return Some(date)
            }

            for(date <- findDate(nodeToString(node)))
            {
                return Some(date)
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
                     TextNode(text, _) <- property.children)
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
                         TextNode(text, _) <- property.children)
                    {
                        if (text !=  propVal)
                        {
                            yearNum  = currentTemplate.getOrElse("elseYear",  "")
                            monthNum = currentTemplate.getOrElse("elseMonth", "")
                            dayNum   = currentTemplate.getOrElse("elseDay",   "")
                        }

                    }
                }

                // get values from defined year month day
                for (yearProperty <- node.property(yearNum);
                     monthProperty <- node.property(monthNum);
                     dayProperty <- node.property(dayNum);
                     year <- yearProperty.children.collect{case TextNode(text, _) => text}.headOption;
                     month <- monthProperty.children.collect{case TextNode(text, _) => text}.headOption;
                     day <- dayProperty.children.collect{case TextNode(text, _) => text}.headOption)
                {
                    try
                    {
                        return Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
                    }
                    catch
                    {
                        case e : IllegalArgumentException =>
                    }
                }
            }
        }

        logger.log(Level.FINE, "Template unknown: " + node.title);
        None
    }

    private def findDate(input: String) : Option[Date] =
    {
        for(date <- catchDate(input))
        {
            return Some(date)
        }

        datatype.name match
        {
            case "xsd:gDay" =>
            {
                logger.fine("Method for day Extraction not yet implemented.")
                None
            }
            case "xsd:gMonth" =>
            {
                logger.fine("Method for month Extraction not yet implemented.")
                None
            }
            case "xsd:gYear" =>
            {
                for(date <- catchMonthYear(input))
                {
                    return Some(date)
                }
                catchYear(input)
            }
            case "xsd:gMonthDay" =>
            {
                catchDayMonth(input)
            }
            case "xsd:gYearMonth" =>
            {
                catchMonthYear(input)
            }
            case _ => None
        }
    }

    /**
     * Finds year, month and day of a provided string
     *
     * Provided Data might be a Date like: [[January 20]] [[2001]], [[1991-10-25]] or 3 June 1981
     * Returns a normalized Date value (eg: 1984-01-29) if a Date is found in the string, NULL otherwise.
     *
     * @param	string	$input	Literaltext, that matched to be a Date
     * 			string	$language language of Literaltext, eg: 'en' or 'de'
     * @return 	string	Date or NULL
     */
    private def catchDate(input: String) : Option[Date] =
    {
        for(DateRegex1(day, month, year) <- List(input))
        {
            // the century (1900 or 2000) depends on the last 2-digit number in the inputstring: >10 -> 1900
            // TODO: replace with more flexible test
            var century = "20"
            if (year.toInt > 20)
            {
                century = "19"
            }
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return new Some(new Date(Some((century+year).toInt), Some(monthNumber.toInt), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex2(day, dunno, month, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return new Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex3(month, day, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return new Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex4(day, month, year) <- List(input))
        {
            return new Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
        }

        for(DateRegex5(day, month, year) <- List(input))
        {
            try
            {
                val monthNumber = months(month.toLowerCase)
                return new Some(new Date(Some(year.toInt), Some(monthNumber), Some(day.toInt), datatype))
            }
            catch
            {
                case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex6(year, month, day) <- List(input))
        {
            return new Some(new Date(Some(year.toInt), Some(month.toInt), Some(day.toInt), datatype))
        }

        for(DateRegex7(day, month,year) <- List(input))
        {
            return new Some(new Date(Some(day.toInt), Some(month.toInt), Some(year.toInt), datatype))
        }

        None
    }

    private def catchDayMonth(input: String) : Option[Date] =
    {
        for(result <- DayMonthRegex1.findFirstMatchIn(input))
        {
            val month = result.group(1)
            val day = result.group(2)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return new Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }
        for(result <- DayMonthRegex2.findFirstMatchIn(input))
        {
            val day = result.group(1)
            val month = result.group(4)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return new Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = datatype))
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
                case Some(monthNumber) => return new Some(new Date(year = Some((eraIdentifier+year).toInt), month = Some(monthNumber), datatype = datatype))
                case None => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }
        None
    }

    private def catchYear(input: String) : Option[Date] =
    {
        for(yearRegex <- YearRegexes)
        {
            input match
            {
                case yearRegex(year, era) =>
                {
                    val eraIdentifier = getEraSign(era)
                    return new Some(new Date(year = Some((eraIdentifier+year).toInt), datatype = datatype))
                }
                case _ =>
            }
        }

        None
    }

    private def nodeToString(node : Node) : String = node match
    {
        case TextNode(text, _) => text
        case _ => node.children.map(nodeToString).mkString
    }

    private def getEraSign(input : String) : String =
    {
        if (input == null) return ""

        // '.' is used in regex as '\\.'
        val tmpInp = input.replace(".", "\\.").toLowerCase

        for ( (key, value) <- eraStr if value == (-1) )
        {
            if (key.toLowerCase == tmpInp.substring(0, math.min(key.size,tmpInp.size)) )
            {
                return "-"
            }
        }
        ""
    }
}