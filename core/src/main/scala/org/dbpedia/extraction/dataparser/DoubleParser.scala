package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.{Node, WikiParserException}
import java.text.ParseException

import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.DataParserConfig

import scala.language.reflectiveCalls
import scala.util.{Failure, Success, Try}

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

    private val logger = ExtractionLogger.getLogger(getClass, context.language)

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
             value <- parseFloatValue(convertedText, node.line) )
        {
            return Some(ParseResult(value * multiplicationFactor))
        }

        None
    }

    private def parseFloatValue(input : String, line: Int) : Try[Double] =
    {
        val numberStr = if(strict) input.trim else DoubleRegex.findFirstMatchIn(input.trim) match
        {
            case Some(s) => s.toString()
            case None =>
                return Failure(new WikiParserException("Cannot convert '" + input + "' to a floating point number, DoubleRegex did not match", line, input, Level.TRACE))
        }

        try
        {
            val result = parserUtils.parse(numberStr).doubleValue
            val hasMinusSign = !input.equals(numberStr) && DataParserConfig.dashVariations.contains(input.trim.charAt(0))
            val negatize = if (result>=0 && hasMinusSign) -1 else 1
            Success(negatize * result)
        }
        catch
        {
            case ex : ParseException =>
                Failure(new WikiParserException("Cannot convert '" + numberStr + "' to a floating point number", line, numberStr, Level.TRACE))
            case ex : NumberFormatException =>
                Failure(new WikiParserException("Cannot convert '" + numberStr + "' to a floating point number", line, numberStr, Level.TRACE))
            case ex : ArrayIndexOutOfBoundsException =>
                Failure(new WikiParserException("Cannot convert '" + numberStr + "' to an integer", line, numberStr, Level.TRACE))
        }
    }
}