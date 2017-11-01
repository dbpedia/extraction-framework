package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause, RecordEntry}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.dataparser.{DataParserConfig, DateTimeParserConfig}
import org.dbpedia.extraction.util.{Date, Language}
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.ontology.Ontology

import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
  * Parse date time
  * @param context - the extraction context
  * @param datatype - the target datatype
  * @param strict - TODO not sure
  * @param tryMinorTypes - if true, after unsuccessfully trying to parse a string into xsd:date (time) we will try to parse it into monthYear and finally year.
  */
@SoftwareAgentAnnotation(classOf[DateTimeParser], AnnotationType.Parser)
class DateTimeParser ( context : {
      def language : Language
      def ontology : Ontology
      def redirects : Redirects
      def recorder[T: ClassTag] : ExtractionRecorder[T] },
     datatype : Datatype,
     val strict : Boolean = false,
     val tryMinorTypes : Boolean = false) extends DataParser[Date]
{
    require(datatype.!=(null), "datatype != null")

  private val recorder = context.recorder[PageNode]

  //datatypes
  val dtDate: Datatype = context.ontology.getOntologyDatatype("xsd:date").get
  val dtDay: Datatype = context.ontology.getOntologyDatatype("xsd:gDay").get
  val dtMonth: Datatype = context.ontology.getOntologyDatatype("xsd:gMonth").get
  val dtYear: Datatype = context.ontology.getOntologyDatatype("xsd:gYear").get
  val dtYearMonth: Datatype = context.ontology.getOntologyDatatype("xsd:gYearMonth").get
  val dtMonthDay: Datatype = context.ontology.getOntologyDatatype("xsd:gMonthDay").get

    // language-specific configurations

    protected val language: String = if(DateTimeParserConfig.supportedLanguages.contains(context.language.wikiCode)) context.language.wikiCode else "en"

    protected val months: _root_.scala.collection.immutable.Map[_root_.java.lang.String, Int] = DateTimeParserConfig.monthsMap.getOrElse(language, DateTimeParserConfig.monthsMap.apply("en"))
    protected val eraStr: _root_.scala.collection.immutable.Map[_root_.java.lang.String, Int] = DateTimeParserConfig.eraStrMap.getOrElse(language, DateTimeParserConfig.eraStrMap.apply("en"))
    protected val cardinalityRegex: String = DateTimeParserConfig.cardinalityRegexMap.getOrElse(language, DateTimeParserConfig.cardinalityRegexMap.apply("en"))
    protected val templates: _root_.scala.collection.immutable.Map[_root_.java.lang.String, _root_.scala.collection.immutable.Map[_root_.java.lang.String, _root_.java.lang.String]] = DateTimeParserConfig.templateDateMap.getOrElse(language, Map.apply())

    // parse logic configurations

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexDateTime.contains(language))
                                                    DataParserConfig.splitPropertyNodeRegexDateTime.apply(language)
                                                  else
                                                    DataParserConfig.splitPropertyNodeRegexDateTime.apply("en")

    protected val monthRegex: _root_.scala.Predef.String = months.keySet.mkString("|")
    protected val eraRegex: _root_.scala.Predef.String = eraStr.keySet.mkString("|")

    protected val prefix: String = if(strict) """\s*""" else """.*?"""
    protected val postfix: String = if(strict) """\s*""" else ".*"

    // catch dates like: "8 June 07" or "07 June 45"
    protected val DateRegex1: scala.util.matching.Regex = ("""(?iu)""" + prefix + """([0-9]{1,2})\s*("""+monthRegex+""")\s*([0-9]{2})(?!\d)\s*(?!\s)(?!"""+ eraRegex +""").*""".+(postfix)).r

    // catch dates like: "[[29 January]] [[300 AD]]", "[[23 June]] [[2008]] (UTC)", "09:32, 6 March 2000 (UTC)" or "3 June 1981"
    protected val DateRegex2: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*("""+monthRegex+""")\]?\]?,? \[?\[?(-?[0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?(?!\d)""" + postfix).r

    // catch dates like: "[[January 20]] [[1995 AD]]", "[[June 17]] [[2008]] (UTC)" or "January 20 1995"
    protected val DateRegex3: scala.util.matching.Regex = ("""(?iu)""" + prefix + """\[?\[?("""+monthRegex+""")\s*,?\s+([0-9]{1,2})\]?\]?(?:""" + cardinalityRegex + """)?\s*[.,]?\s+\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?\]?\]?""" + postfix).r

    // catch dates like: "24-06-1867", "24/06/1867" or "bla24-06-1867bla"
    protected val DateRegex4: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/]([0-9]{1,2}+)[-/]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "24-june-1867", "24/avril/1867" or "bla24|juillet|1867bla"
    protected val DateRegex5: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{1,2}+)[-/\|](""" + monthRegex + """)[-/\|]([0-9]{3,4}+)(?!\d)""" + postfix).r

    // catch dates like: "1990 06 24", "1990-06-24", "1990/06/24" or "1977-01-01 00:00:00.000000"
    protected val DateRegex6: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)([0-9]{3,4})[-/\s]([0-9]{1,2})[-/\s]([0-9]{1,2})(?!\d).*""").r

    // catch dates like: "20 de Janeiro de 1999", "[[1º de Julho]] de [[2005]]"
    protected val DateRegex7: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)\[?\[?([0-9]{1,2})(\.|""" + cardinalityRegex + """)?\s*d?e?\s*(""" + monthRegex + """)\]?\]?\s*d?e?\s*\[?\[?([0-9]{0,4})\s*?\]?\]?(?!\d)""" + postfix).r

    // catch dates like: "1520, March 16"
    protected val DateRegex8: scala.util.matching.Regex = ("""(?iu)""" + prefix + """([0-9]{3,4})[,]?\s+(""" + monthRegex + """)\s+([0-9]{1,2})(?:""" + cardinalityRegex + """)?\s*""").r

    protected val DayMonthRegex1: scala.util.matching.Regex = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?\s*\[?\[?([1-9]|0[1-9]|[12][0-9]|3[01])(?!\d)""" + postfix).r

    protected val DayMonthRegex2: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<!\d)([1-9]|0[1-9]|[12][0-9]|3[01])\s*(""" + cardinalityRegex + """)?\]?\]?\s*(of)?\s*\[?\[?("""+monthRegex+""")\]?\]?""" + postfix).r

    protected val MonthYearRegex: scala.util.matching.Regex = ("""(?iu)""" + prefix + """("""+monthRegex+""")\]?\]?,?\s*\[?\[?([0-9]{1,4})\s*(""" + eraRegex + """)?""" + postfix).r

    protected val YearRegex: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(?<![\d\pL\w])(-?\d{1,4})(?!\d)\s*(""" + eraRegex + """)?""" + postfix).r

    protected val YearRegex2: scala.util.matching.Regex = ("""(?iu)""" + prefix + """(""" + eraRegex + """)(?<![\d])(\d{1,4})(?!\d)\s*""" + postfix).r

  private[dataparser] override def parse(node : Node) : Option[ParseResult[Date]] =
    {
      try
      {
          for( child @ TemplateNode(_, _, _, _) <- node.children;
               date <- catchTemplate(child))
          {
              return Some.apply(ParseResult.apply(date))
          }

          for(date <- findDate(node))
          {
              return Some.apply(ParseResult.apply(date))
          }
      }
      catch
      {
          case ex : IllegalArgumentException  => recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Exception, Language.getOrElse(language, Language.None), "Error while parsing date", ex))
          case ex : NumberFormatException => recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Exception, Language.getOrElse(language, Language.None), "Error while parsing date", ex))
      }

      None
    }

    private def catchTemplate(node: TemplateNode) : Option[Date] =
    {
        val templateName: String = context.redirects.resolve(node.title).decoded.toLowerCase

        for(currentTemplate <- templates.get(templateName))
        {
            //check for text template e.g. Birth-date
            if (currentTemplate.keySet.contains("text")) {
                // find date in "text" value property
                for (property <- node.property(currentTemplate.getOrElse("text", ""));
                     TextNode(text, _, _) <- property.children)
                {
                    return findDate(node)
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
                    val propNum: String = currentTemplate.getOrElse("ifPropertyNum", "")
                    val propVal: String = currentTemplate.getOrElse("ifPropertyNumHasValue", "")

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

      recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Template unknown: " + node.title))
      None
    }

    protected def getDateByParameters(node: TemplateNode, yearNum: String, monthNum: String, dayNum: String): Option[Date] = {
        // get values from defined year month day
      val year: _root_.scala.Option[Int] = node.property(yearNum) match{
        case Some(yearProperty) => yearProperty.children.collect({ case TextNode(text, _, _) => text }).headOption match{
            case Some(yy) => yy match {
              case YearRegex(y, era) =>
                val eraIdentifier: _root_.scala.Predef.String = getEraSign(era)
                Some.apply((eraIdentifier + y).toInt)
              case YearRegex(y) => Some.apply(y.toInt)
              case YearRegex2(era, y) =>
                val eraIdentifier: _root_.scala.Predef.String = getEraSign(era)
                Some.apply((eraIdentifier + y).toInt)
              case _ => None
            }
            case None => None
        }
        case None => None
      }
        val month: _root_.scala.Option[Int] = node.property(monthNum) match {
          case Some(monthProperty) => monthProperty.children.collect({ case TextNode(text, _, _) => text }).headOption match {
            case Some(m) => months.get(m.toLowerCase) match {
              case Some(s) => Some.apply(s)
              case None => Some.apply(m.toInt)
            }
            case None => None
          }
          case None => None
        }
        val day: _root_.scala.Option[Int] = node.property(dayNum) match{
            case Some(dayProperty) => dayProperty.children.collect({ case TextNode(text, _, _) => text }).headOption match{
              case Some(d) => Some.apply(d.toInt)
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
        Some.apply(new Date(year, month, day, dt))
      else
        None
    }

    private def findDate(node: Node) : Option[Date] =
    {
      val input = nodeToString(node).trim
        for(date <- catchDate(input, node))
        {
            return Some.apply(date)
        }

        datatype match
        {
            case `dtDay` =>
              recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Method for day Extraction not yet implemented."))
              None
            case `dtMonth` =>
              recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Method for day Extraction not yet implemented."))
              None
            case `dtYear` =>
                for(date <- catchMonthYear(input, node))
                {
                    return Some.apply(date)
                }
                catchYear(input)
            case `dtMonthDay` =>
                catchDayMonth(input, node)
            case `dtYearMonth` =>
                catchMonthYear(input, node)
            case _ => None
        }
    }

    /**
     * Finds year, month and day of a provided string
     *
     * Provided Data might be a Date like: [[January 20]] [[2001]], [[1991-10-25]] or 3 June 1981
     * Returns a normalized Date value (eg: 1984-01-29) if a Date is found in the string, NULL otherwise.
     *
     * @param  input    Literaltext, that matched to be a Date
     *             string    $language language of Literaltext, eg: 'en' or 'de'
      * @param  node    The origin node
     * @return     string    Date or NULL
     */
    private def catchDate(input: String, node: Node) : Option[Date] =
    {
        for(DateRegex1(day, month, year) <- List.apply(input))
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
                case Some(monthNumber) => return Some.apply(new Date(Some.apply((century+year).toInt), Some.apply(monthNumber.toInt), Some.apply(day.toInt), datatype))
                case None =>
                  recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }

        for(DateRegex2(day, dunno, month, year, era) <- List.apply(input))
        {
            val eraIdentifier: _root_.scala.Predef.String = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some.apply(new Date(Some.apply((eraIdentifier+year).toInt), Some.apply(monthNumber), Some.apply(day.toInt), datatype))
                case None => recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }

        for(DateRegex3(month, day, year, era) <- List.apply(input))
        {
            val eraIdentifier: _root_.scala.Predef.String = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some.apply(new Date(Some.apply((eraIdentifier+year).toInt), Some.apply(monthNumber), Some.apply(day.toInt), datatype))
                case None => recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }

        for(DateRegex4(day, month, year) <- List.apply(input))
        {
            return Some.apply(new Date(Some.apply(year.toInt), Some.apply(month.toInt), Some.apply(day.toInt), datatype))
        }

        for(DateRegex5(day, month, year) <- List.apply(input))
        {
            try
            {
                val monthNumber: Int = months.apply(month.toLowerCase)
                return Some.apply(new Date(Some.apply(year.toInt), Some.apply(monthNumber), Some.apply(day.toInt), datatype))
            }
            catch
            {
                case ex: NoSuchElementException =>
                  recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }

        for(DateRegex6(year, month, day) <- List.apply(input))
        {
            return Some.apply(new Date(Some.apply(year.toInt), Some.apply(month.toInt), Some.apply(day.toInt), datatype))
        }

        for(DateRegex7(day, month, year) <- List.apply(input))
        {
            return Some.apply(new Date(Some.apply(year.toInt), Some.apply(month.toInt), Some.apply(day.toInt), datatype))
        }

        for(DateRegex8(year, month, day) <- List.apply(input))
        {
            months.get(month.toLowerCase) match
            {
              case Some(monthNumber) => return Some.apply(new Date(Some.apply(year.toInt), Some.apply(monthNumber), Some.apply(day.toInt), datatype))
              case None =>
                recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }

      if(tryMinorTypes)
        catchMonthYear(input, node) match{
          case Some(d) => Some.apply(new Date(year = d.year, month = d.month, day= Some.apply(1), datatype = dtDate))
          case None => None
        }
      else
        None
    }

    private def catchDayMonth(input: String, node: Node) : Option[Date] =
    {
        for(result <- DayMonthRegex1.findFirstMatchIn(input))
        {
            val month: _root_.scala.Predef.String = result.group(1)
            val day: _root_.scala.Predef.String = result.group(2)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some.apply(new Date(month = Some.apply(monthNumber), day = Some.apply(day.toInt), datatype = dtMonthDay))
                case None =>
                  recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }
        for(result <- DayMonthRegex2.findFirstMatchIn(input))
        {
            val day: _root_.scala.Predef.String = result.group(1)
            val month: _root_.scala.Predef.String = result.group(4)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some.apply(new Date(month = Some.apply(monthNumber), day = Some.apply(day.toInt), datatype = dtMonthDay))
                case None =>
                  recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }
        None
    }

    private def catchMonthYear(input: String, node: Node) : Option[Date] =
    {
        for(result <- MonthYearRegex.findFirstMatchIn(input))
        {
            val month: _root_.scala.Predef.String = result.group(1)
            val year: _root_.scala.Predef.String = result.group(2)
            val era: _root_.scala.Predef.String = result.group(3)
            val eraIdentifier: _root_.scala.Predef.String = getEraSign(era)
            months.get(month.toLowerCase) match
            {
                case Some(monthNumber) => return Some.apply(new Date(year = Some.apply((eraIdentifier+year).toInt), month = Some.apply(monthNumber), datatype = dtYearMonth))
                case None =>
                  recorder.enterProblemRecord(new RecordEntry[PageNode](node.root, RecordCause.Internal, Language.getOrElse(language, Language.None), "Month with name '"+month+"' (language: "+language+") is unknown"))
            }
        }
        if(tryMinorTypes)
          catchYear(input) match{
            case Some(d) => Some.apply(new Date(year = d.year, month = Some.apply(1), day= Some.apply(1), datatype = dtDate))
            case None =>
          }
        None
    }

    private def catchYear(input: String) : Option[Date] =
    {
        for(result <- YearRegex.findFirstMatchIn(input))
        {
            val year: _root_.scala.Predef.String = result.group(1)
            val eraIdentifier: _root_.scala.Predef.String = getEraSign(result.group(2))
            return Some.apply(new Date(year = Some.apply((eraIdentifier+year).toInt), datatype = dtYear))
        }
        for(result <- YearRegex2.findFirstMatchIn(input))
        {
            val year: _root_.scala.Predef.String = result.group(2)
            val eraIdentifier: _root_.scala.Predef.String = getEraSign(result.group(1))
            return Some.apply(new Date(year = Some.apply((eraIdentifier+year).toInt), datatype = dtYear))
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
        val tmpInp: String = input.replace(".", "\\.").toLowerCase

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