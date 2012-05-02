package org.dbpedia.extraction.wikiparser

/**
 * Represents a template property.
 *
 * @param key The key by which this property is identified in the template.
 * @param children The contents of the value of this property
 * @param line The source line number of this property
 */
case class PropertyNode(key : String, override val children : List[Node], override val line : Int) extends Node(children, line)
{
    def toWikiText(): String =
    {
      // named arguments prefix name and "=", positional arguments use only the value
      val prefix = 
      try 
      { 
        key.toInt
        "" 
      } 
      catch 
      { 
        case e : NumberFormatException => key+"=" 
      }
        
      prefix+children.map(_.toWikiText).mkString("")
    }
}