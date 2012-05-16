package org.dbpedia.extraction.wikiparser

/**
 * Represents a template.
 *
 * @param title The title of the page, where this template is defined
 * @param children The properties of this template
 * @param line The source line number of this property
 */
case class TemplateNode(title : WikiTitle, override val children : List[PropertyNode], override val line : Int) extends Node(children, line)
{
    private val propertyMap : Map[String, PropertyNode] = Map.empty ++ (for(property <- children) yield (property.key, property))
    
    /**
     * Retrieves a property by its key.
     * As in Wikipedia the search is case insensitive in the first character and case sensitive in the rest.
     *
     * @param key The key of the wanted property.
     * @return PropertyNode The propertyNode or None if no property with a given key has been found.
     */
    def property(key : String) : Option[PropertyNode] =
    {
        return propertyMap.get(key);
    }

    def keySet :  scala.collection.Set[String] = propertyMap.keySet

    def toWikiText() : String = "{{" + title.decoded + (if(children.isEmpty) "" else "|") + children.map(_.toWikiText).mkString("|") + "}}"
}
