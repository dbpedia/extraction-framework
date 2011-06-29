package org.dbpedia.extraction.dataparser

import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.wikiparser.Node
import java.text.{NumberFormat, ParseException}
import java.math.RoundingMode
import org.dbpedia.extraction.util.Language
/**
 * Parses integer numbers.
 */
class IntegerParser( extractionContext : { def language : Language } ,
                     strict : Boolean = false,
                     multiplicationFactor : Int = 1,
                     validRange : Int => Boolean = (i => true)) extends DataParser
{
    private val numberFormat = NumberFormat.getIntegerInstance(extractionContext.language.locale)
    numberFormat.setRoundingMode(RoundingMode.HALF_UP)

    private val parserUtils = new ParserUtils(extractionContext)

    private val logger = Logger.getLogger(classOf[IntegerParser].getName)

    override val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete

    private val IntegerRegex = """\D*?(\-?[0-9\-\,\.]+).*""".r

    override def parse(node : Node) : Option[Int] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text);
             value <- parseIntegerValue(convertedText) )
        {
            return Some(value * multiplicationFactor)
        }

        None
    }

    private def parseIntegerValue(input : String) : Option[Int] =
    {

        val numberStr = if(strict) input.trim else IntegerRegex.findFirstMatchIn(input.trim) match
        {
            case Some(s) => s.toString()
            case None =>
            {
                logger.log(Level.FINE, "Cannot convert '" + input + "' to an integer, IntegerRegex did not match")
                return None
            }
        }

        try
        {
            val resultInt = numberFormat.parse(numberStr).intValue
            if( validRange(resultInt) )
            {
                Some(resultInt)
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