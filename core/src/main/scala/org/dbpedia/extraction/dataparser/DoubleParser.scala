package org.dbpedia.extraction.dataparser

import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.wikiparser.Node
import java.text.ParseException

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.DataParserConfig

import scala.language.reflectiveCalls

/**
 * Parses double-precision floating-point numbers.
 */
//TODO a lot of copied code from IntegerParser!
@SoftwareAgentAnnotation(classOf[DoubleParser], AnnotationType.Parser)
class DoubleParser( context : { def language : Language },
                    strict : Boolean = false,
                    multiplicationFactor : Double = 1.0) extends DataParser[Double]
{
    private val parserUtils = new ParserUtils(context)

    private val logger = Logger.getLogger(getClass.getName)

    private val language = context.language.wikiCode

    override val splitPropertyNodeRegex: String = if (DataParserConfig.splitPropertyNodeRegexDouble.contains(language))
                                            DataParserConfig.splitPropertyNodeRegexDouble(language)
                                          else DataParserConfig.splitPropertyNodeRegexDouble("en")

    // we allow digits, minus, comma, dot and space in numbers
    private val DoubleRegex  = """\D*?(-?[0-9-,. ]+).*""".r

    private[dataparser] override def parse(node : Node) : Option[ParseResult[Double]] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text.value);
             value <- parseFloatValue(convertedText) )
        {
            return Some(ParseResult(value * multiplicationFactor))
        }

        None
    }

    private def parseFloatValue(input : String) : Option[Double] =
    {
        val numberStr = if(strict) input.trim else DoubleRegex.findFirstMatchIn(input.trim) match
        {
            case Some(s) => s.toString()
            case None =>
                logger.log(Level.FINE, "Cannot convert '" + input + "' to a floating point number, DoubleRegex did not match")
                return None
        }

        try
        {
            val result = parserUtils.parse(numberStr).doubleValue
            val hasMinusSign = !input.equals(numberStr) && DataParserConfig.dashVariations.contains(input.trim.charAt(0))
            val negatize = if (result>=0 && hasMinusSign) -1 else 1
            Some(negatize * result)
        }
        catch
        {
            case ex : ParseException =>
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to a floating point number", ex)
                None
            case ex : NumberFormatException =>
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to a floating point number", ex)
                None
            case ex : ArrayIndexOutOfBoundsException =>
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                None
        }
    }
}