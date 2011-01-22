package org.dbpedia.extraction.dataparser

import scala.util.matching.Regex
import java.util.logging.{Logger,Level}
import org.dbpedia.extraction.mappings.ExtractionContext
import org.dbpedia.extraction.wikiparser.{PropertyNode, NodeUtil, Node}

/**
 * Parses double-precision floating-point numbers.
 */
class DoubleParser(extractionContext : ExtractionContext, val strict : Boolean = false) extends DataParser
{
    private val language = extractionContext.language.wikiCode
    
    private val logger = Logger.getLogger(classOf[DoubleParser].getName)

    private val prefix = if(strict) """\s*""" else """[\D]*?"""

    private val postfix = if(strict) """\s*""" else ".*"

    private val DOUBLE_REGEX1 = new Regex(prefix + """(?<!-)([0-9\-]+(?:\,[0-9]{3})*(?:\.[0-9]+)?)""" + postfix)
    private val DOUBLE_REGEX2 = new Regex(prefix + """(?<!-)([0-9\-]+(?:\.[0-9]{3})*(?:\,[0-9]+)?)""" + postfix)

    override def parse(node : Node) : Option[Double] =
    {
        for( text <- StringParser.parse(node);
             convertedText = ParserUtils.convertLargeNumbers(text, extractionContext.language);
             value <- parseFloatValue(convertedText) )
        {
            return Some(value)
        }
        
        return None
    }

    override def splitPropertyNode(propertyNode : PropertyNode) : List[Node] =
    {
        //TODO this split regex might not be complete
        NodeUtil.splitPropertyNode(propertyNode, """<br\s*\/?>|\n| and | or |;""")
    }

    private def parseFloatValue(input : String) : Option[Double] =
    {
        val numberStr =
            if (language == "en" || language == "ja" || language == "zh")
            {
                input match
                {
                    case DOUBLE_REGEX1(num) => num.replace(",","")
                    case _ =>
                    {
                        logger.fine("No floating point number found in '" + input + "'")
                        return None
                    }
                }
            }
            else
            {
                input match
                {
                    case DOUBLE_REGEX2(num) => num.replace(".","").replace(",",".")
                    case _ =>
                    {
                        logger.fine("No floating point number found in '" + input + "'")
                        return None
                    }
                }
            }

        try
        {
            Some(numberStr.toDouble)
        }
        catch
        {
            case ex : NumberFormatException =>
            {
                logger.log(Level.FINE, "Cannot convert '" + numberStr + "' to an floating point number", ex)
                None
            }
        }
    }
}