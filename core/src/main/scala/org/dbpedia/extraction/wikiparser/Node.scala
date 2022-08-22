package org.dbpedia.extraction.wikiparser

import scala.collection.mutable
import scala.collection.mutable.HashMap
import org.dbpedia.extraction.util.StringUtils.{escape,replacements}
import org.dbpedia.extraction.util.WikiUtil

/**
 * Base class of all nodes in the abstract syntax tree.
 * 
 * This class is NOT thread-safe.
 */
abstract class Node( val children : List[Node], val line : Int)
{
    /**
     * CAUTION: code outside this class should change the parent only under very rare circumstances.
     */
    var parent: Node = null
    
    //Set the parent of all children
    for(child <- children) child.parent = this

    private val annotations = new HashMap[AnnotationKey[_], Any]()

  /**
    * TODO the UriGenerator class needs to be replaced (see bottom)
    */
    private val uriGenerator = new UriGenerator()

    def generateUri(baseUri : String, node : Node) = uriGenerator.generate(baseUri, node)

    def generateUri(baseUri : String, name : String) = uriGenerator.generate(baseUri, name)

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
            node = node.parent
        }
        
        node.asInstanceOf[PageNode]
    }
   
    /**
     * Retrieves the section of this node
     * 
     * @return The section of this node, may be null
     */
    lazy val section : SectionNode = findSection
    
    private def findSection : SectionNode = {
      
        var section : SectionNode = null
        
        for(node <- root.children)
        {
            if(node.line > line)
            {
                return section
            }

            if(node.isInstanceOf[SectionNode])
            {
                section = node.asInstanceOf[SectionNode]
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
     */
    def sourceIri : String =
    {
        val sb = new java.lang.StringBuilder
        
        sb append root.title.pageIri
        if (root.revision >= 0) sb append "?oldid=" append root.revision

        if (section != null)
        {
            sb append '#' append "section="
            escape(sb, WikiUtil.cleanSpace(section.name), Node.fragmentEscapes)
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
  val fragmentEscapes = {
    val chars = ('\u0000' to '\u001F').mkString + "\"#%<>[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    val replace = replacements('%', chars)
    // don't escape space, replace it by underscore
    replace(' ') = "_"
    replace
  }
}


private class UriGenerator
{
    var uris = Map[String, Int]()

    def generate(baseUri : String, node : Node) : String =
    {
        node match
        {
            case _ : Node =>
            {
                val sb = new StringBuilder()
                nodeToString(node, sb)
                generate(baseUri, sb.toString())
            }
            case null => generate(baseUri, "") //TODO forbid node==null ?
        }
    }

    def generate(baseUri : String, name : String) : String =
    {
        var uri : String = baseUri

        if(name != "")
        {
            var text = name
            
            
			//TODO CENTRALIZE STRING CLEANING
            //Normalize text
            text = WikiUtil.removeWikiEmphasis(text)
            text = text.replace("&nbsp;", " ")//TODO decode all html entities here
            text = text.replace('"', ' ')
            text = text.replace('(', ' ')
            text = text.replace(')', ' ')
            text = text.replace('\n', ' ')
            text = text.replace('\r', ' ')
            text = text.replace('\t', ' ')
            text = text.replace('\u0091', ' ')
            text = text.replace('\u0092', ' ')
            text = text.replaceAll("\\<.*?\\>", "") //strip tags
            text = WikiUtil.cleanSpace(text)
            if(text.length > 50) text = text.substring(0, 50)
            text = WikiUtil.wikiEncode(text)

			
			
            //Test if the base URI ends with a prefix of text
            var i = Math.max(baseUri.lastIndexOf('_'), baseUri.lastIndexOf('/')) + 1
            var done = false
            while(!done && i > 0 && baseUri.length - i < text.length)
            {
                if(baseUri.regionMatches(true, i, text, 0, baseUri.length - i))
                {
                    text = text.substring(baseUri.length - i)
                    done = true
                }

                i -= 1
            }

            //Remove leading underscore
            if(!text.isEmpty && text(0) == '_')
            {
                text = text.substring(1)
            }

            //Generate URI
            uri = baseUri + "__" + text
        }

        val index = uris.getOrElse(uri, 0) + 1
        uris = uris.updated(uri, index)
        uri + "__" + index.toString
    }

    private def nodeToString(node : Node, sb : mutable.StringBuilder)
    {
        node match
        {
            case TextNode(text, _, _) => sb.append(text)
            case null => //ignore
            case _ : TemplateNode => //ignore
            case _ : TableNode => //ignore
            case _ => node.children.foreach(child => nodeToString(child, sb))
        }
    }
}
