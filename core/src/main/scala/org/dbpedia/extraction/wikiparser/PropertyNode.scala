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
    def toWikiText() : String =
    {
        try
        {
            // if key is a number, it did not have a key in the original wiki text
            key.toInt
            children.map(_.toWikiText).mkString("")
        }
        catch
        {
            case e : NumberFormatException => key + "=" + children.map(_.toWikiText).mkString("")
        }
    }
}