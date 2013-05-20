/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.extraction.dataparser

import org.dbpedia.extraction.wikiparser._

object ListParser extends DataParser {
    val splitPropertyNodesRegex = Map (
        "en"-> List("""<br\s*\/?>"""),
	"fr"-> List("""[C-c]lr""")
    )

    def parseList (node : Node, language : String) : Option[PropertyNode] = {
        var propertyNode = node.asInstanceOf[PropertyNode]
        
        if (splitPropertyNodesRegex.contains (language)) {
            var currentNodes = List[Node]()
            var cleanedPropertyNode = new PropertyNode ("", List[Node](), 0)
        
            for(child <- propertyNode.children) child match {
                case TemplateNode(title, children, line, titleParsed) => {
                    for (listRegex <- splitPropertyNodesRegex.get(language)) {
                        for(regex <- listRegex) {
                            if (title.decoded matches regex) {
                                currentNodes = currentNodes ::: List[Node](new TextNode("<br />", line))
                            }
                        }
                    }
                }
                case _ => currentNodes = currentNodes ::: List[Node](child)
            }
            cleanedPropertyNode = new PropertyNode(propertyNode.key, currentNodes, propertyNode.line)
            cleanedPropertyNode.parent = node.parent
            
            return parse(cleanedPropertyNode)
        }
        
        return parse(node)
    }
  
    override def parse(node : Node) : Option[PropertyNode] =
    {
        return Some(node.asInstanceOf[PropertyNode])
    }
}
