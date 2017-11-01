package org.dbpedia.extraction.dataparser

import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.wikiparser.Node
import java.text.ParseException

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.DataParserConfig

import scala.language.reflectiveCalls

/**
 * Parses integer numbers.
 */
@SoftwareAgentAnnotation(classOf[IntegerParser], AnnotationType.Parser)
class IntegerParser( context : { def language : Language } ,
                     strict : Boolean = false,
                     multiplicationFactor : Double = 1.0,
                     validRange : Double => Boolean = i => true) extends DataParser[Long]
{
    private val parserUtils = new ParserUtils(context)

    private val logger = Logger.getLogger(getClass.getName)

    private val language = context.language.wikiCode

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexInteger.contains(language))
                                            DataParserConfig.splitPropertyNodeRegexInteger(language)
                                          else DataParserConfig.splitPropertyNodeRegexInteger("en")

    // we allow digits, minus, comma, dot and space in numbers
    private val IntegerRegex = """\D*?(?:\D\d+\s+)?(-?[0-9,\. ]+).*""".r

    private[dataparser] override def parse(node : Node) : Option[ParseResult[Long]] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text.value);
             value <- parseValue(convertedText) )
        {
            return Some(ParseResult((value * multiplicationFactor).round))
        }

        None
    }

    private def parseValue(input : String) : Option[Double] =   // double is returned because
    {
        val numberStr = if(strict) input.trim else IntegerRegex.findFirstMatchIn(input.trim) match
        {
            case Some(s) => s.subgroups.head.toString // s is the WHOLE MATCH, while we want the matching subgroup
            case None =>
            {
                logger.log(Level.FINE, "Cannot convert '" + input + "' to an integer, IntegerRegex did not match")
                return None
            }
        }

        try
        {
            val result = parserUtils.parse(numberStr).doubleValue
            if( validRange(result) )
            {
                val hasMinusSign = !input.equals(numberStr) && DataParserConfig.dashVariations.contains(input.trim.charAt(0))
                val negatize = if (result>=0 && hasMinusSign) -1 else 1
                Some(negatize * result)
            }
            else
            {
                None
            }
        }
        catch
        {
            case ex : ParseException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                None
            }
            case ex : NumberFormatException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                None
            }
            case ex : ArrayIndexOutOfBoundsException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                None
            }
        }
    }
}