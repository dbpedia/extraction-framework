package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.{Datatype}
import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.util.{Language, Date}
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.wikiparser._

/**
 * Parses a data time.
 */
class DateTimeParser (extractionContext : ExtractionContext, datatype : Datatype, val strict : Boolean = false) extends DataParser
{
    require(datatype != null, "datatype != null")

    private val months = Map(
    		"en" -> Map("january"->1,"february"->2,"march"->3,"april"->4,"may"->5,"june"->6,"july"->7,"august"->8,"september"->9,"october"->10,"november"->11,"december"->12),
    		"de" -> Map("januar"->1,"februar"->2,"märz"->3,"maerz"->3,"april"->4,"mai"->5,"juni"->6,"juli"->7,"august"->8,"september"->9,"oktober"->10,"november"->11,"dezember"->12),
    		"el" -> Map("ιανουάριος"->1,"φεβρουάριος"->2,"μάρτιος"->3,"απρίλιος"->4,"μάϊος"->5,"μάιος"->5,"ιούνιος"->6,"ιούλιος"->7,"αύγουστος"->8,"σεπτέμβριος"->9,"οκτώβριος"->10,"νοέμβριος"->11,"δεκέμβριος"->12,
    		            "ιανουαρίου"->1,"φεβρουαρίου"->2,"μαρτίου"->3,"απριλίου"->4,"μαΐου"->5,"μαίου"->5,"ιουνίου"->6,"ιουλίου"->7,"αυγούστου"->8,"σεπτεμβρίου"->9,"οκτωβρίου"->10,"νοεμβρίου"->11,"δεκεμβρίου"->12),  // overrides previous definitions!!?!
    		"fr" -> Map("janvier"->1,"février"->2,"mars"->3,"avril"->4,"mai"->5,"juin"->6,"juillet"->7,"août"->8,"septembre"->9,"octobre"->10,"novembre"->11,"décembre"->12),
    		"it" -> Map("gennaio"->1,"febbraio"->2,"marzo"->3,"aprile"->4,"maggio"->5,"giugno"->6,"luglio"->7,"agosto"->8,"settembre"->9,"ottobre"->10,"novembre"->11,"dicembre"->12),
    		"pl" -> Map("stycznia"->1,"lutego"->2,"marca"->3,"kwietnia"->4,"maja"->5,"czerwca"->6,"lipca"->7,"sierpnia"->8,"września"->9,"października"->10,"listopada"->11,"grudnia"->12),
            "hr" -> Map("siječanj"->1,"veljača"->2,"ožujak"->3,"travanj"->4,"svibanj"->5,"lipanj"->6,"srpanj"->7,"kolovoz"->8,"rujan"->9,"listopad"->10,"studeni"->11,"prosinac"->12))

    //private val supportedLanguages = Set("en", "de", "fr", "it", "el", "pl", "hr")
    private val supportedLanguages = months.keySet
    private val language = if(supportedLanguages.contains(extractionContext.language.wikiCode)) extractionContext.language.wikiCode else "en"

    //maybe add this to infobox extractor RankRegex val
    private val cardinality = Map(
        "en" -> "st|nd|rd|th",
        "el" -> "η|ης"
    )
    // -1 is for BC
    //TODO matches anything e.g. 20 bd
    private val eraStr =  Map(
        "en" -> Map("BCE" -> 1, "BC" -> (-1), "CE"-> 1, "AD"-> 1, "AC"-> (-1), "CE"-> 1),
        "el" -> Map("ΠΧ"-> (-1), "Π\\.Χ\\."-> (-1), "Π\\.Χ"-> (-1) , "ΜΧ"-> 1 , "Μ\\.Χ\\."-> 1, "Μ\\.Χ"-> 1)
    )

    private val monthRegex = months.get(language).getOrElse(months("en")).keySet.mkString("|")
    private val cardinalityRegex = cardinality.get(language).getOrElse(cardinality("en"))
    private val eraRegex = eraStr.get(language).getOrElse(eraStr("en")).keySet.mkString("|")

    private val logger = Logger.getLogger(classOf[UnitValueParser].getName)

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

        return None
    }

    override def splitPropertyNode(propertyNode : PropertyNode) : List[Node] =
    {
        //TODO this split regex might not be complete
        NodeUtil.splitPropertyNode(propertyNode, """<br\s*\/?>|\n| and | or |;""")
    }

    private def catchTemplate(node: TemplateNode) : Option[Date] =
    {
    	val templateName = extractionContext.redirects.resolve(node.title).decoded

		val childrenChilds = for(child <- node.children) yield
            { for(childrenChild @ TextNode(_, _)<- child.children) yield childrenChild }

        if (language == "en")
        {
            if (templateName == "Birth-date")
            {
                for (property <- node.property("1");
                     TextNode(text, _) <- property.children)
                {
                    return findDate(text)
                }
            }
            // http://en.wikipedia.org/wiki/Template:Birth_date_and_age
            // {{Birth date|year_of_birth|month_of_birth|day_of_birth|...}}
            // Sometimes the templates are used wrong like this:
            // {{birth date|df=yes|1833|10|21}}
            // TODO: fix problem with gYear gDate e.q. Alfred Nobel
            else if (templateName == "Birth date and age" || templateName == "Birth date and age2" ||
                templateName == "Death date and age" || templateName == "Birth date" ||
                templateName == "Death date" || templateName == "Bda" || templateName == "Dob")
            {
                for (yearProperty <- node.property("1"); monthProperty <- node.property("2"); dayProperty <- node.property("3");
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
            // http://en.wikipedia.org/wiki/Template:BirthDeathAge
            // {{BirthDeathAge|birth_or_death_flag|year_of_birth|month_of_birth|day_of_birth|year_of_death|month_of_death|day_of_death|...}}
            else if (templateName == "Birth Death Age")
            {
                // gets the text from the single textNode of the first PropertyNode
                // {{BirthDeathAge|BIRTH_OR_DEATH_FLAG|year_of_birth|month_of_birth|day_of_birth|year_of_death|month_of_death|day_of_death|...}}
                for (property <- node.property("1")) property.retrieveText match
                {
                    case Some("B") =>
                    {
                        for (yearProperty <- node.property("2"); monthProperty <- node.property("3"); dayProperty <- node.property("4");
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
                    case _ =>
                    {
                        for (yearProperty <- node.property("5"); monthProperty <- node.property("6"); dayProperty <- node.property("7");
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
            }
        }
        else if (language == "el")
        {
            //birth_year|birth_month|birth_day}} //same, parse the first 3 each time
            //death_year|death_month|death_dat|birth_year|birth_month|birth_day}}
            if (templateName.toLowerCase == "ηγη"  || templateName.toLowerCase == "ημερομηνία γέννησης και ηλικία" ||
                templateName.toLowerCase == "ηθηλ" || templateName.toLowerCase == "ημερομηνία θανάτου και ηλικία" ||
                templateName.toLowerCase == "ημερομηνία γέννησης")
            {
                for (yearProperty <- node.property("1"); monthProperty <- node.property("2"); dayProperty <- node.property("3");
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
        return None
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
                 return None
             }
             case "xsd:gMonth" =>
             {
                 logger.fine("Method for month Extraction not yet implemented.")
                 return None
             }
             case "xsd:gYear" =>
             {
                for(date <- catchMonthYear(input))
                {
                    return Some(date)
                }
                return catchYear(input)
             }
             case "xsd:gMonthDay" =>
             {
                return catchDayMonth(input)
             }
             case "xsd:gYearMonth" =>
             {
                 return catchMonthYear(input)
             }
             case _ => return None
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
            try
            {
                val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(Some((century+year).toInt), Some(monthNumber.toInt), Some(day.toInt), datatype))
            }
            catch
            {
                case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex2(day, dunno, month, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            try
            {
                val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
            }
            catch
            {
                case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
            }
        }

        for(DateRegex3(month, day, year, era) <- List(input))
        {
            val eraIdentifier = getEraSign(era)
            try
            {
                val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(Some((eraIdentifier+year).toInt), Some(monthNumber), Some(day.toInt), datatype))
            }
            catch
            {
            case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
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
                val monthNumber = months(language)(month.toLowerCase())
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

        return None

    }

    private def catchDayMonth(input: String) : Option[Date] =
    {
    	for(result <- DayMonthRegex1.findFirstMatchIn(input))
    	{
    		val month = result.group(1)
    		val day = result.group(2)
	    	try
	    	{
	    		val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = datatype))
	    	}
	    	catch
	    	{
	    		case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
	    	}
    	}
        for(result <- DayMonthRegex2.findFirstMatchIn(input))
        {
        	val day = result.group(1)
        	val month = result.group(4)
        	try
        	{
        		val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(month = Some(monthNumber), day = Some(day.toInt), datatype = datatype))
        	}
        	catch
        	{
        		case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
        	}
    	}
        return None
    }

    private def catchMonthYear(input: String) : Option[Date] =
    {
    	for(result <- MonthYearRegex.findFirstMatchIn(input))
    	{
    		val month = result.group(1)
    		val year = result.group(2)
    		val era = result.group(3)
            val eraIdentifier = getEraSign(era)
	    	try
	    	{
	    		val monthNumber = months(language)(month.toLowerCase())
                return new Some(new Date(year = Some((eraIdentifier+year).toInt), month = Some(monthNumber), datatype = datatype))
	    	}
	    	catch
	    	{
	    		case ex: NoSuchElementException => logger.log(Level.FINE, "Month with name '"+month+"' (language: "+language+") is unknown")
	    	}
    	}
    	return None
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

        return None
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
        val tmpMap = if (eraStr.contains(language)) eraStr(language) else eraStr("en")
        for ( (key, value) <- tmpMap
              if value==(-1))
        {
            if (key.toLowerCase == tmpInp.substring(0,Math.min(key.size,tmpInp.size)) )
                return "-"
        }
        return ""
    }
}