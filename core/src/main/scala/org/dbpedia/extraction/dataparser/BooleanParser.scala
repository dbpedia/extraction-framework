package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser.Node

import scala.util.matching.Regex

/**
 * Returns true for strings that contain "true" or "yes" (in any case), 
 * false for strings that contain "false" or "no" (in any case), and
 * None for other strings.
 * TODO: also look for "0"/"1"? "on"/"off"?
 * TODO: match only full strings, not strings that contain these words?
 */
object BooleanParser extends DataParser
{
  // TODO: these regexes are probably not quite what we want. See BooleanParserTest
  val FALSE_REGEX = new Regex("(?i).*?(no|false).*")
  val TRUE_REGEX = new Regex("(?i).*?(yes|true).*")
  
  override def parse( node : Node ) : Option[Boolean] =
  {
    // Note: BooleanParser.php only checked the children, not the node itself.
    for (child <- node :: node.children; string <- child retrieveText)
    {
      string match
      {
        case FALSE_REGEX(_) => return Some(false)
        case TRUE_REGEX(_) => return Some(true)
        case _ =>
      }
    }
    
    return None
  }
}
