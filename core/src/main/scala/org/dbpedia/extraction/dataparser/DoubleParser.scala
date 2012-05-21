package org.dbpedia.extraction.dataparser

import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.wikiparser.Node
import java.text.{ParseException, NumberFormat}
import org.dbpedia.extraction.util.Language

/**
 * Parses double-precision floating-point numbers.
 */
//TODO a lot of copied code from IntegerParser!
class DoubleParser( extractionContext : { def language : Language },
                    strict : Boolean = false,
                    multiplicationFactor : Double = 1.0) extends DataParser
{
    private val numberFormat = NumberFormat.getInstance(extractionContext.language.locale)

    private val parserUtils = new ParserUtils(extractionContext)

    private val logger = Logger.getLogger(classOf[DoubleParser].getName)

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete

    // we allow digits, minus, comma, dot and space in numbers
    private val DoubleRegex  = """\D*?(-?[0-9-,. ]+).*""".r

    override def parse(node : Node) : Option[Double] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text);
             value <- parseFloatValue(convertedText) )
        {
            return Some(value * multiplicationFactor)
        }

        None
    }

    private def parseFloatValue(input : String) : Option[Double] =
    {
        val numberStr = if(strict) input.trim else DoubleRegex.findFirstMatchIn(input.trim) match
        {
            case Some(s) => s.toString()
            case None =>
            {
                logger.log(Level.FINE, "Cannot convert '" + input + "' to a floating point number, DoubleRegex did not match")
                return None
            }
        }

        try
        {
            Some(numberFormat.parse(numberStr).doubleValue)
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
}