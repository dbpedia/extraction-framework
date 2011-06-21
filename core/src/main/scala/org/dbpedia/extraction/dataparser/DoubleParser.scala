package org.dbpedia.extraction.dataparser

import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.wikiparser.{PropertyNode, NodeUtil, Node}
import java.text.{ParseException, NumberFormat}

/**
 * Parses double-precision floating-point numbers.
 */
//TODO test after re-factor
class DoubleParser(extractionContext : ExtractionContext, val strict : Boolean = false) extends DataParser
{
    private val numberFormat = NumberFormat.getInstance(extractionContext.language.locale)

    private val parserUtils = new ParserUtils(extractionContext)

    private val logger = Logger.getLogger(classOf[DoubleParser].getName)

    private val splitPropertyNodeRegex = """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete

    private val DoubleRegex  = """\D*?(\-?[0-9\-\,\.]+).*""".r

    override def parse(node : Node) : Option[Double] =
    {
        for( text <- StringParser.parse(node);
             convertedText = parserUtils.convertLargeNumbers(text);
             value <- parseFloatValue(convertedText) )
        {
            return Some(value)
        }
        
        None
    }

    override def splitPropertyNode(propertyNode : PropertyNode) : List[Node] =
    {
        NodeUtil.splitPropertyNode(propertyNode, splitPropertyNodeRegex)
    }

    private def parseFloatValue(input : String) : Option[Double] =
    {
        val numberStr = if(strict) input.trim else DoubleRegex.findFirstMatchIn(input.trim).toString

        try
        {
            Some(numberFormat.parse(input).doubleValue)
        }
        catch
        {
            case ex : ParseException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an floating point number", ex)
                None
            }
        }
    }
}