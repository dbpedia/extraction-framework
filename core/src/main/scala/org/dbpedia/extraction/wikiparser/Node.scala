package org.dbpedia.extraction.wikiparser

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import java.net.URLEncoder

/**
 * Base class of all nodes in the abstract syntax tree.
 * This class is thread-safe.
 */
abstract class Node(val children : List[Node], val line : Int)
{
    @volatile private var _parent : Node = null
    
    //Set the parent of all children
    for(child <- children) child._parent = this

    /**
     * The parent node.
     */
    def parent = _parent

    //TODO hack until a better way is found to replace TableMappings by TemplateMappings and append them to the original AST
    def parent_=(n : Node) = _parent = n

    private val annotations = new HashMap[String, Any]() with SynchronizedMap[String, Any]

    /**
     * Convert back to original (or equivalent) wiki markup string.
     */
    def toWikiText() : String

    /**
     *  Retrieves the root node of this AST.
     *
     * @return The root Node
     */
    def root : PageNode =
    {
        var node = this
        while(node.parent != null)
        {
            node = node.parent;
        }
        return node.asInstanceOf[PageNode];
    }
   
    /**
     * Retrieves the section of this node
     * 
     * @return The section of this node
     */
    def section : SectionNode =
    {
        var section : SectionNode = null;
        for(node <- root.children)
        {
            if(node.line > line)
            {
                return section;
            }

            if(node.isInstanceOf[SectionNode])
            {
                section = node.asInstanceOf[SectionNode];
            }
        }
        return section
    }

    /**
     * Retrieves the text denoted by this node.
     * Only works on nodes that only contain text.
     * Returns null if this node contains child nodes other than TextNode.
     */
    def retrieveText : Option[String] =
    {
    	if(isInstanceOf[TextNode])
    	{
    		return Some(asInstanceOf[TextNode].text)
    	}
    	else if(children.length == 1 && children(0).isInstanceOf[TextNode])
        {
            return Some(children(0).asInstanceOf[TextNode].text)
        }
        else
        {
            return None
        }
    }

    /*
     * Returns an annotation.
     *
     * @param key key of the annotation
     * @return The value of the annotation as an option if an annotation with the given key exists. None, otherwise.
     */
    def annotation(key : String) : Option[Any] =
    {
    	return annotations.get(key)
    }

    /**
     * Sets a user-defined annotation.
     *
     * @param key The key of the annotation
     * @param value The value of the annotation
     */
    def setAnnotation(key : String, value : Any)
    {
    	annotations(key) = value
    }
    
    /**
     * URL of source page and line number.
     */
    def sourceUri : String =
    {
        //Get current section
        val section = this.section

        //Build source URI
        var sourceURI = sourceUriPrefix

        if(section != null)
        {
            sourceURI += "section=" + URLEncoder.encode(section.name, "UTF-8")
            sourceURI += "&relative-line=" + (line - section.line)
            sourceURI += "&absolute-line=" + line
        }
        else if(line >= 1)
        {
            sourceURI += "absolute-line=" + line
        }
        
        return sourceURI
    }

    /**
     * Get first part of source URL.
     * TODO: It's ugly to have such a special-purpose function here. Is there a better way?
     * @return first part of source URL, containing the page name and the separator character, 
     * but not the line number etc.
     */
    private def sourceUriPrefix : String =
    {
        val page = root

        // TODO: make base URI configurable
        "http://" + page.title.language.wikiCode + ".wikipedia.org/wiki/" + page.title.encodedWithNamespace + '#'
    }
}
