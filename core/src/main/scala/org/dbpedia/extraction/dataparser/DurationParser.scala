package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.{UnitDatatype, DimensionDatatype, Datatype}
import org.dbpedia.extraction.config.dataparser.DurationParserConfig
import org.dbpedia.extraction.util.Language
import java.text.{ParseException, NumberFormat}
import java.util.logging.{Logger, Level}
import util.matching.Regex

class DurationParser( extractionContext : { def language : Language } )
{
    private val language = extractionContext.language.wikiCode

    private val numberFormat = NumberFormat.getInstance(extractionContext.language.locale)

    private val logger = Logger.getLogger(classOf[DoubleParser].getName)

    private val timeUnits = DurationParserConfig.timesMap.getOrElse(language, DurationParserConfig.timesMap("en"))

    private val timeUnitsRegex = timeUnits.keys.toList.sortWith((a,b) => a.length > b.length).mkString("|")

    val TimeValueColonUnitRegex = ("""^\D*?(-)?\s?(\d+)?\:(\d\d)\:?(\d\d)?\s*(""" + timeUnitsRegex + """)?(\W\D*?|\W*?)$""").r

    // TODO: this regex does not support minus signs
    val TimeValueUnitRegex = ("""(\d[,\.\s\d]*\s*)(""" + timeUnitsRegex + """)""").r

    def parseToSeconds(input : String, inputDatatype : Datatype) : Option[Double] =
    {
        parse(input, inputDatatype) match
        {
            case Some(duration) => Some(duration.toSeconds)
            case None => None
        }
    }

    private def parse(input : String, inputDatatype : Datatype) : Option[Duration] =
    {
        val targetUnit : String = inputDatatype match
        {
            case dt : DimensionDatatype => ""
            case dt : UnitDatatype => dt.name
        }

        input match {
            case TimeValueColonUnitRegex(sign, null, v2, null, null, trail) => {
                // ":xx"
                if(v2 == null)
                {
                    return None
                }
                targetUnit match {
                    case "second" => Some(new Duration(seconds = v2.toDouble, reverseDirection = (sign == "-")))
                    // default: seconds
                    case _ => Some(new Duration(seconds = v2.toDouble, reverseDirection = (sign == "-")))
                }
            }
            case TimeValueColonUnitRegex(sign, null, v2, null, unit, trail) => {
                // ":xx unit"  unit must be seconds
                if(v2 == null)
                {
                    return None
                }
                timeUnits.get(unit.trim).getOrElse("") match {
                    case "second" => Some(new Duration(seconds = v2.toDouble, reverseDirection = (sign == "-")))
                    case _ => None
                }
            }
            case TimeValueColonUnitRegex(sign, v1, v2, null, null, trail) => {
                // "xx:yy"
                if(v1 == null || v2 == null)
                {
                    return None
                }
                targetUnit match {
                    case "hour" => Some(new Duration(hours =  v1.toDouble, minutes = v2.toDouble, reverseDirection = (sign == "-")))
                    case "minute" => Some(new Duration(minutes =  v1.toDouble, seconds = v2.toDouble, reverseDirection = (sign == "-")))
                    // default: minutes
                    case _ => Some(new Duration(minutes =  v1.toDouble, seconds = v2.toDouble, reverseDirection = (sign == "-")))
                }
            }
            case TimeValueColonUnitRegex(sign, v1, v2, null, unit, trail) => {
                // "xx:yy unit"  unit must be hours or minutes
                if(v1 == null || v2 == null)
                {
                    return None
                }
                timeUnits.get(unit.trim).getOrElse("") match {
                    case "hour" => Some(new Duration(hours = v1.toDouble, minutes = v2.toDouble, reverseDirection = (sign == "-")))
                    case "minute" => Some(new Duration(minutes = v1.toDouble, seconds = v2.toDouble, reverseDirection = (sign == "-")))
                    case _ => None
                }
            }
            case TimeValueColonUnitRegex(sign, v1, v2, v3, null, trail) => {
                // "xx:yy:zz"
                if(v1 == null || v2 == null || v3 == null)
                {
                    return None
                }
                targetUnit match {
                    case "hour" => Some(new Duration(hours = v1.toDouble, minutes = v2.toDouble, seconds = v3.toDouble, reverseDirection = (sign == "-")))
                    // default: hours
                    case _ => Some(new Duration(hours = v1.toDouble, minutes = v2.toDouble, seconds = v3.toDouble, reverseDirection = (sign == "-")))
                }
            }
            case TimeValueColonUnitRegex(sign, v1, v2, v3, unit, trail) => {
                // "xx:yy:zz unit"  unit must be hours
                if(v1 == null || v2 == null || v3 == null)
                {
                    return None
                }
                timeUnits.get(unit.trim).getOrElse("") match {
                    case "hour" =>  Some(new Duration(hours = v1.toDouble, minutes = v2.toDouble, seconds = v3.toDouble, reverseDirection = (sign == "-")))
                    case _ => None
                }
            }

            case _ =>
            {
                val durationsMap = TimeValueUnitRegex.findAllIn(input).matchData.map{ m =>
                {
                    val unit = timeUnits.get(m.subgroups(1).replaceAll("""\W""", "")).getOrElse(return None)  // hack to deal with e.g "min)" matches
                    val num = getNum(m).getOrElse(return None)
                    (unit, num) }
                }.toMap

                if (durationsMap.isEmpty)
                    None
                else
                    Some(new Duration(years   = (durationsMap.get("year")  .getOrElse("0")).toDouble,
                                      months  = (durationsMap.get("month") .getOrElse("0")).toDouble,
                                      days    = (durationsMap.get("day")   .getOrElse("0")).toDouble,
                                      hours   = (durationsMap.get("hour")  .getOrElse("0")).toDouble,
                                      minutes = (durationsMap.get("minute").getOrElse("0")).toDouble,
                                      seconds = (durationsMap.get("second").getOrElse("0")).toDouble
                                      //reverseDirection = (sign == "-")
                    ))
            }
        }
    }

    private def getNum(m : Regex.Match) : Option[String] =
    {
        val numberStr = m.subgroups(0)
        try
        {
            Some(numberFormat.parse(numberStr).toString)
        }
        catch
        {
            case ex : ParseException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to a floating point number", ex)
                None
            }
            case ex : NumberFormatException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to a floating point number", ex)
                None
            }
            case ex : ArrayIndexOutOfBoundsException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                None
            }
        }
    }


    private class Duration(years : Double = 0, months : Double = 0, days : Double = 0, hours : Double = 0, minutes : Double = 0, seconds : Double = 0, reverseDirection : Boolean = false)
    {
        def toSeconds : Double =
        {
            // Months are approximated as having 30.4375 days (averaged over four years).
            // Years are approximated as having 365.25 days (averaged over four years).
            (seconds + (minutes*60) + (hours*60*60) + (days*60*60*24) + (months*60*60*24*30.4375) + (years*60*60*24*365.25))
        }

        override def toString =
        // Returns the duration in PnYnMnDTnHnMnS format (for XSD Duration Datatype).
        // TODO (in mappings): extract XSD Duration Datatype
        {
            def getIntsAndCarry(args : List[Tuple2[Double, Int]], res : List[Int], carry : Double) : Tuple3[List[Tuple2[Double, Int]], List[Int], Double] =
            {
                if (args.isEmpty) return (List(), res, 0.0)
                val topLevelTriple = getIntsAndCarry(args.tail, res, carry)
                val thisValue = args.head._1 + args.head._2 * topLevelTriple._3
                (List(), thisValue.floor.toInt :: topLevelTriple._2, thisValue%1)
            }

            val input = List((seconds, 60), (minutes, 60), (minutes, 24), (hours, 24), (days, 365), (years, 1))
            val durationInts = getIntsAndCarry(input , List(), 0)._2

            (if (reverseDirection) "-" else "") +
                "P" +
                    (if (durationInts(5) > 0) durationInts(5) + "Y" else "") +
                    (if (durationInts(4) > 0) durationInts(4) + "M" else "") +
                    (if (durationInts(3) > 0) durationInts(3) + "D" else "") +
            (if (durationInts(0) > 0 || durationInts(1) > 0 || durationInts(2) > 0)
                "T" else "") +
                    (if (durationInts(2) > 0) durationInts(2) + "H" else "") +
                    (if (durationInts(1) > 0) durationInts(1) + "M" else "") +
                    (if (durationInts(0) > 0) durationInts(0) + "S" else "")
        }
    }
}