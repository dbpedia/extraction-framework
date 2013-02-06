package org.dbpedia.extraction.wikiparser

import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap
import java.lang.StringBuilder
import org.dbpedia.extraction.util.StringUtils.{escape,replacements}

/**
 * Base class of all nodes in the abstract syntax tree.
 * 
 * This class is NOT thread-safe.
 */
abstract class Node(val children : List[Node], val line : Int)
{
    /**
     * CAUTION: code outside this class should change the parent only under very rare circumstances.
     */
    var parent: Node = null
    
    //Set the parent of all children
    for(child <- children) child.parent = this

    private val annotations = new HashMap[AnnotationKey[_], Any]()

    /**
     * Convert back to original (or equivalent) wiki markup string.
     */
    def toWikiText: String

    /**
     * Get plain text content of this node and all child nodes, without markup. Since templates
     * are not expanded, this will not work well for templates.
     */
    def toPlainText: String

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
     * Retrieves some text from this node. Only works on a TextNode or a Node that has 
     * a single TextNode child. Returns None iff this node is not a TextNode and contains 
     * child nodes other than a single TextNode.
     * 
     * TODO: the behavior of this method is weird, but I don't dare to change it because existing
     * code may rely on its current behavior. New code should probably use toPlainText.
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
    @unchecked // we know the type is ok - setAnnotation doesn't allow any other type
    def getAnnotation[T](key: AnnotationKey[T]): Option[T] = {
      annotations.get(key).asInstanceOf[Option[T]]
    }

    /**
     * Sets a user-defined annotation.
     *
     * @param key The key of the annotation
     * @param value The value of the annotation
     */
    def setAnnotation[T](key: AnnotationKey[T], value: T): Unit = {
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
        if (root.revision >= 0) sb append "?oldid=" append root.revision

        if (section != null)
        {
            sb append '#' append "section="
            escape(sb, section.name, Node.fragmentEscapes)
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

object Node {
  // For this list of characters, see ifragment in RFC 3987 and 
  // https://sourceforge.net/mailarchive/message.php?msg_id=28982391
  // Only difference to ipchar: don't escape '?'. We don't escape '/' anyway.
  private val fragmentEscapes = {
    val chars = ('\u0000' to '\u001F').mkString + "\"#%<>[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    val replace = replacements('%', chars)
    // don't escape space, replace it by underscore
    replace(' ') = "_"
    replace
  }
}
