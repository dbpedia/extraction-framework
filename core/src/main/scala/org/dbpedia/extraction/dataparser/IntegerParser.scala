package org.dbpedia.extraction.dataparser

import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.wikiparser.Node
import java.text.ParseException
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.config.dataparser.DataParserConfig

/**
 * Parses integer numbers.
 */
class IntegerParser( context : { def language : Language } ,
                     strict : Boolean = false,
                     multiplicationFactor : Double = 1.0,
                     validRange : Double => Boolean = (i => true)) extends DataParser
{
    private val parserUtils = new ParserUtils(context)

    private val logger = Logger.getLogger(getClass.getName)

    private val language = context.language.wikiCode

    override val splitPropertyNodeRegex = if (DataParserConfig.splitPropertyNodeRegexInteger.contains(language))
                                            DataParserConfig.splitPropertyNodeRegexInteger.get(language).get
                                          else DataParserConfig.splitPropertyNodeRegexInteger.get("en").get

    // we allow digits, minus, comma, dot and space in numbers
    private val IntegerRegex = """\D*?(-?[0-9-,. ]+).*""".r

    override def parse(node : Node) : Option[Long] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text);
             value <- parseValue(convertedText) )
        {
            return Some((value * multiplicationFactor).round)
        }

        None
    }

    private def parseValue(input : String) : Option[Double] =   // double is returned because
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
            val result = parserUtils.parse(numberStr).doubleValue
            if( validRange(result) )
            {
                Some(result)
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