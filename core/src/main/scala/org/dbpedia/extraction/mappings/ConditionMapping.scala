package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.{Node, TemplateNode}
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.dataparser.StringParser

class ConditionMapping(
  templateProperty : String,
  operator : String,
  value : String,
  val mapping : Mapping[TemplateNode] // must be public val for statistics
) 
extends Mapping[TemplateNode]
{
  /** Check if templateProperty is defined */
  require(operator == "otherwise" || templateProperty != null, "templateProperty must be defined")
  
  /** Check if given operator is supported */
  require(List("isSet", "equals", "contains", "otherwise").contains(operator), "Invalid operator: " + operator +". Supported operators: isSet, equals, contains, otherwise")
  
  /** Check if value is defined */
  require(operator == "otherwise" || operator == "isSet" || value != null, "Value must be defined")

  override val datasets = mapping.datasets

  override def extract(node : TemplateNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    // Note: CompositeMapping will call extract() without calling matches() first, so we 
    // have to check matches() here. If we are part of a ConditionalMapping, this call of matches() 
    // will be redundant, but it's cheap, so it's not a problem.
    if (matches(node)) mapping.extract(node, subjectUri, pageContext)
    else Seq.empty
  }

  def matches(node : TemplateNode) : Boolean =
  {
    if (operator == "otherwise") true
    else {
      
      // Note: if you add operators whose evaluation may be expensive, you should probably
      // add a constructor parameter that tells us if the call to matches() in extract() can
      // be omitted. This constructor argument should be true if this object is part of 
      // a ConditionalMapping (which calls matches() before extract()) and false otherwise.
      val property = node.property(templateProperty).getOrElse(return false)
      val propertyText = StringParser.parse(property).getOrElse("").toLowerCase.trim

      operator match
      {
          case "isSet" => ! propertyText.isEmpty
          // FIXME: toLowerCase must use correct language locale
          case "equals" => propertyText == value.toLowerCase
          // FIXME: toLowerCase must use correct language locale
          case "contains" => propertyText.contains(value.toLowerCase)
          case _ => false
      }
    }
  }
  
}
