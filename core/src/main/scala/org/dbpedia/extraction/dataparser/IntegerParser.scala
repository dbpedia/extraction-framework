package org.dbpedia.extraction.dataparser

import scala.util.matching.Regex
import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.wikiparser.{NodeUtil, PropertyNode, Node}
import java.text.{NumberFormat, ParseException}
import java.math.RoundingMode

/**
 * Parses integer numbers.
 */
class IntegerParser(extractionContext : ExtractionContext,
                    val strict : Boolean = false,
                    val validRange : Int => Boolean = (i => true)) extends DataParser
{
    private val logger = Logger.getLogger(classOf[IntegerParser].getName)

    private val prefix = if(strict) """\s*""" else """[\D]*?"""

    private val postfix = if(strict) """\s*""" else ".*"

    private val IntRegex = new Regex(prefix + """(?<!-)([0-9\,\.\-]+)""" + postfix)

    private val formatter = NumberFormat.getIntegerInstance(extractionContext.language.locale)
    formatter.setRoundingMode(RoundingMode.HALF_UP)

    override def parse(node : Node) : Option[Int] =
    {
        for( text <- StringParser.parse(node);
             convertedText = ParserUtils.convertLargeNumbers(text, extractionContext.language);
             value <- parseIntegerValue(convertedText) )
        {
            return Some(value)
        }
        
        None
    }

    override def splitPropertyNode(propertyNode : PropertyNode) : List[Node] =
    {
        //TODO this split regex might not be complete
        NodeUtil.splitPropertyNode(propertyNode, """<br\s*\/?>|\n| and | or |;""")
    }
    
    private def parseIntegerValue(input : String) : Option[Int] =
    {
        val numberStr = input match
        {
            case IntRegex(num) => num
            case _ =>
            {
                logger.fine("No integer found in '" + input + "'")
                return None
            }
        }

        try
        {
            val resultInt = formatter.parse(numberStr).intValue
            if ( validRange(resultInt) )
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
                return None
            }
            case ex : NumberFormatException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                return None
            }
            case ex : ArrayIndexOutOfBoundsException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an integer", ex)
                return None
            }
        }
    }
}