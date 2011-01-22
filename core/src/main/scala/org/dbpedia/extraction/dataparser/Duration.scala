package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.ontology.datatypes.{Datatype, UnitDatatype, DimensionDatatype}
import java.util.Locale
import java.text.NumberFormat

/**
 * Represents a time duration in
 * years, months, days, hours, minutes, seconds,
 * possibly in the past-direction.
 */

class Duration(years : Double = 0, months : Double = 0, days : Double = 0, hours : Double = 0, minutes : Double = 0, seconds : Double = 0
             , reverseDirection : Boolean = false)
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
            if (args.isEmpty) return (List(), res, 0)
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

object Duration
{
    // TODO: get this map from data types configuration
    val timesMap = Map(
        "second" -> "second", "s" -> "second", "sec" -> "second", "seconds" -> "second", "secs" -> "second",
            "Sekunde" -> "second", "Sekunden" -> "second", "sekunde" -> "second", "sekunden" -> "second", "sek" -> "second",
        "minute" -> "minute", "m" -> "minute", "min" -> "minute", "minutes" -> "minute", "min." -> "minute", "mins" -> "minute", "minu" -> "minute",
            "Minute" -> "minute", "Minuten" -> "minute", "minuten" -> "minute",
        "hour" -> "hour", "h" -> "hour", "hour" -> "hour", "hr" -> "hour", "hr." -> "hour", "hrs" -> "hour", "hrs." -> "hour",
            "Stunde" -> "hour", "Stunden" -> "hour", "stunde" -> "hour", "stunden" -> "hour", "std" -> "hour", "Std" -> "hour", "std." -> "hour", "Std." -> "hour",
        "day" -> "day", "d" -> "day", "d." -> "day", "days" -> "day",
            "Tag" -> "day", "Tage" -> "day", "tag" -> "day", "tage" -> "day",
        "month" -> "month", "months" -> "month",
            "Monat" -> "month", "Monate" -> "month", "monat" -> "month", "monate" -> "month",
        "year" -> "year", "y" -> "year", "years" -> "year", "yr" -> "year",
            "Jahr" -> "year", "Jahre" -> "year", "jahr" -> "year", "jahre" -> "year"
    )

    val timeUnitsRegex = timesMap.keys.toList.sortWith((a,b) => a.length > b.length).mkString("|")

    val TimeValueColonUnitRegex = ("""^\D*?(-)?\s?(\d+)?\:(\d\d)\:?(\d\d)?\s*(""" + timeUnitsRegex + """)?(\W\D*?|\W*?)$""").r

    // TODO: this regex does not support minus signs
    val TimeValueUnitRegex = ("""(\d[,\.\s\d]*\s*)(""" + timeUnitsRegex + """)""").r

    
    def parse(input : String, inputDatatype : Datatype, locale : Locale) : Option[Duration] =
    {
        val numberformat = NumberFormat.getInstance(locale)
        
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
                timesMap.get(unit.trim).getOrElse("") match {
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
                timesMap.get(unit.trim).getOrElse("") match {
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
                timesMap.get(unit.trim).getOrElse("") match {
                    case "hour" =>  Some(new Duration(hours = v1.toDouble, minutes = v2.toDouble, seconds = v3.toDouble, reverseDirection = (sign == "-")))
                    case _ => None
                }
            }

            case _ => val durationsMap = TimeValueUnitRegex.findAllIn(input).matchData.map{ m => {
                          val unit = timesMap.get(m.subgroups(1).replaceAll("""\W""", "")).getOrElse(return None)  // hack to deal with e.g "min)" matches
                          val num = numberformat.parse(m.subgroups(0).replace(" ", "")).toString
                          (unit, num) } }.toMap
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