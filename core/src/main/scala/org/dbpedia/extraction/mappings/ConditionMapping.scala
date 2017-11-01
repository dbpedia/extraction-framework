package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.dataparser.StringParser
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser.{InternalLinkNode, TemplateNode}

class ConditionMapping(
  val templateProperty : String,
  val operator : String,
  val value : String,
  val mapping : Extractor[TemplateNode] // must be public val for statistics
) 
extends Extractor[TemplateNode]
{
  /** Check if templateProperty is defined */
  require(operator == "otherwise" || templateProperty != null, "templateProperty must be defined")
  
  /** Check if given operator is supported */
  require(List("isSet", "equals", "contains", "otherwise").contains(operator), "Invalid operator: " + operator +". Supported operators: isSet, equals, contains, otherwise")
  
  /** Check if value is defined */
  require(operator == "otherwise" || operator == "isSet" || value != null, "Value must be defined")

  override val datasets = mapping.datasets

  override def extract(node : TemplateNode, subjectUri : String) : Seq[Quad] =
  {
    // Note: CompositeMapping will call extract() without calling matches() first, so we 
    // have to check matches() here. If we are part of a ConditionalMapping, this call of matches() 
    // will be redundant, but it's cheap, so it's not a problem.
    if (matches(node)) mapping.extract(node, subjectUri)
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
      val propertyText = StringParser.parseWithProvenance(property) match{
        case Some(s) => s.value.toLowerCase.trim
        case None => ""
      }

      operator match
      {
          case "isSet" => ! propertyText.isEmpty
          case "isIn" => value.split(",").map(_.trim.toLowerCase()).contains(propertyText)
          // FIXME: toLowerCase must use correct language locale
          case "equals" => propertyText == value.trim.toLowerCase
          // FIXME: toLowerCase must use correct language locale
          case "contains" => propertyText.contains(value.trim.toLowerCase)
          case "hasLink" => property.children.exists(n => n.isInstanceOf[InternalLinkNode])
          case _ => false
      }
    }
  }
}
