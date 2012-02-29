package org.dbpedia.extraction.mappings

import xml.Node
import xml.NodeSeq._
import collection.mutable.{ListBuffer, Stack}
import xml.{XML, Node => XMLNode}
import scala.util.matching.Regex
import org.dbpedia.extraction.wikiparser._

import MyStack._

/**.
 * User: Jonas Brekle <jonas.brekle@gmail.com>
 * Date: 08.05.11
 * Time: 15:55
 */

/**
 * these classes represent configuration from the xml
 * they are simple wrappers that provide a object representation to avoid xml in the extractor
 */
class TripleTemplate(val s : String, val p : String, val o : String, val oType : String = "URI", val oNewBlock : Boolean = false)

class ResultTemplate(val triples : Seq[TripleTemplate]){}

class PostProcessing(var clazz : String, var parameters : Map[String, String]){}

class Tpl (val name : String, val wiki : Stack[org.dbpedia.extraction.wikiparser.Node], val pp : Option[PostProcessing], val resultTemplates : Seq[ResultTemplate])
object Tpl {
  //$var, ()*+? for repetitions, and ~~link~~ to subsumpt the three link types
  def expandTpl(orig : String) : String = {
    //println(orig)
    val pattern = new Regex("(?<!\\\\)\\$[a-zA-Z0-9]+")
    val tplVarsExpanded = pattern.replaceAllIn(
       orig, 
       (m) => "{{Extractiontpl|var|"+m.matched.replace("$","")+"}}"
    )

    var tpl = tplVarsExpanded.replace("\\(","§$%o%$§").replace("\\)","§$%c%$§")
    val pattern2 = new Regex("\\((([^\\(\\)])*?)\\)([\\+\\*\\?])")
    while(pattern2.findFirstIn(tpl).isDefined){
      tpl = pattern2.replaceAllIn(tpl, 
           (m) => "{{Extractiontpl|list-start|"+m.group(3)+"}}"+m.group(1).replace("(","").replace(")","")+"{{Extractiontpl|list-end}}"
      )
    }
    tpl = tpl.replace("§$%o%$§", "(").replace("§$%c%$§", ")")

    val pattern3 = new Regex("(?<!\\\\)~~(.*?)~~")
    val tplLinksExpanded = pattern3.replaceAllIn(tpl, 
       (m) => "{{Extractiontpl|link|"+m.matched.replace("~","")+"}}")
    val tplExpanded = tplLinksExpanded.replace("\\","")
    //println(tplExpanded)
    return tplExpanded
  }

  def fromNode(n:XMLNode) = {
    val pp = if(n.attribute("ppClass").isDefined){Some(new PostProcessing((n \ "@ppClass").text, (n \ "parameters" \ "parameter").map(n=>{ ((n \ "@name").text, (n \ "@value").text) }).toMap))} else None

    val tplString = (n \ "wikiTemplate").text
    // expand the terse template syntax 
    val tplExpanded = expandTpl(tplString)

    //println(tplExpanded);
    val tpl = MyStack.fromString(tplExpanded).filterNewLines
    if(tplExpanded.startsWith("\n")){
        tpl.push(new TextNode("\n",0))
    }
    
    val rt = (n \ "resultTemplates" \ "resultTemplate").map(
            rtn => new ResultTemplate(
                (rtn \ "triples" \ "triple").map(
                    t => new TripleTemplate( 
                        (t \ "@s").text, 
                        (t \ "@p").text, 
                        (t \ "@o").text,
                        if(t.attribute("oType").isDefined){(t \ "@oType").text} else {"URI"} ,
                        t.attribute("oNewBlock").isDefined && (t \ "@oNewBlock").text.equals("true") 
                    ) 
                )
            )
   )
    
    //return
    new Tpl(
      (n \ "@name").text,
      tpl,
      pp, 
      rt
    )
  }
}

class Block (n : XMLNode, val parent : Block
//val name : String, val indTpl : Tpl, val blocks : List[Block], val templates : List[Tpl], val property : String, val parent : Block
){
      val name = (n \ "@name").text
      val indTpl = if((n \ "indicator").size > 0) {Tpl.fromNode((n \ "indicator").head)} else null
      val blocks = (n \ "block").map(b => new Block(b, this)).toList
      val templates  = (n \ "templates" \ "template").map(t=>Tpl.fromNode(t)).toList
      val property = (n \ "@property").text

  //override def clone = new Block(name, indTpl, blocks, templates, property, parent)
}

class Page ( n : XMLNode
//name : String, blocks : List[Block], templates : List[Tpl], property : String
) extends Block (n, null
){

}
