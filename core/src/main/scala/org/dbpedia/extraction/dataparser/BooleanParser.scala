package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.Node

import scala.language.postfixOps

/**
 * Returns true for strings that contain "true" or "yes" (only as isolated terms),
 * false for strings that contain "false" or "no" (only as isolated terms), and
 * None for other strings.
 * TODO: also look for "0"/"1"? "on"/"off"?
 */
object BooleanParser extends DataParser
{
  private val FALSE_REGEX = """(?i)(?:.*\s)*(no|false)(?:\s.*)*""".r
  private val TRUE_REGEX = """(?i)(?:.*\s)*(yes|true)(?:\s.*)*""".r
  
  override def parse( node : Node ) : Option[ParseResult[Boolean]] =
  {
    // Note: BooleanParser.php only checked the children, not the node itself.
    for (child <- node :: node.children; string <- child retrieveText)
    {
      string match
      {
        case FALSE_REGEX(_) => return Some(ParseResult(false))
        case TRUE_REGEX(_) => return Some(ParseResult(true))
        case _ =>
      }
    }
    
    None
  }
}
