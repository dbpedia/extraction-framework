package org.dbpedia.extraction.wikiparser

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import java.net.URLEncoder

import org.dbpedia.extraction.util.RichString.toRichString // implicit

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
    lazy val root : PageNode =
    {
        var node = this
        
        while(node.parent != null)
        {
            node = node.parent;
        }
        
        node.asInstanceOf[PageNode];
    }
   
    /**
     * Retrieves the section of this node
     * 
     * @return The section of this node, may be null
     */
    lazy val section : SectionNode = findSection
    
    private def findSection : SectionNode = {
      
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
        
        section
    }

    /**
     * Retrieves the text denoted by this node.
     * Only works on nodes that only contain text.
     * Returns None if this node contains child nodes other than TextNode.
     */
    final def retrieveText: Option[String] = retrieveText(true)

    protected def retrieveText(recurse: Boolean): Option[String] = {
      if (recurse && children.length == 1) children(0).retrieveText(false) else None
    }

    /**
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
     * IRI of source page and line number.
     * TODO: rename to sourceIri.
     */
    def sourceUri : String =
    {
        val sb = new StringBuilder
        
        sb append root.title.pageIri

        if (section != null)
        {
            // FIXME: section name escaping is incorrect. We need to escape at least control chars.
            sb append '#' append "section=" append section.name.replace(' ', '_').escape('.', "\"#%<>?[\\]^`{|}")
            sb append "&relative-line=" append (line - section.line)
            sb append "&absolute-line=" append line
        }
        else if (line >= 1)
        {
            sb append '#' append "absolute-line=" append line
        }
        
        sb.toString
    }

}
