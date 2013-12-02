package org.dbpedia.extraction.mappings.wikitemplate

import xml.NodeSeq._
import collection.mutable.{ListBuffer, Stack}
import xml.{XML, Node => XMLNode}
import scala.util.matching.Regex
import org.dbpedia.extraction.wikiparser._

import MyStack._
import collection.mutable

/**.
 * User: Jonas Brekle <jonas.brekle@gmail.com>
 * Date: 08.05.11
 * Time: 15:55
 */

/**
 * these classes represent configuration from the xml
 * they are simple wrappers that provide a object representation to avoid xml in the extractor
 */
class TripleTemplate(t : XMLNode) {

  val s : String = (t \ "@s").text
  val p : String = (t \ "@p").text
  val o : String = (t \ "@o").text
  val oLang : String = if(t.attribute("oLang").isDefined){(t \ "@oLang").text} else {null}
  val oDatatype : String = if(t.attribute("oDatatype").isDefined){(t \ "@oDatatype").text} else {null}
  val oType : String = if(t.attribute("oType").isDefined){(t \ "@oType").text} else {"URI"}
  val oNewBlock : Boolean =t.attribute("oNewBlock").isDefined && (t \ "@oNewBlock").text.equals("true")
  val optional : Boolean = t.attribute("optional").isDefined && (t \ "@optional").text.equals("true")
}

class ResultTemplate(val triples : Seq[TripleTemplate])

class PostProcessing(var clazz : String, var parameters : Map[String, String]){}

class Tpl (val name : String, val wiki : Stack[Node], val pp : Option[PostProcessing], val resultTemplates : Seq[ResultTemplate])
object Tpl {

  val Cache = mutable.Map[String, Tpl]()

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

  def fromNode(n:XMLNode) : Tpl = {
    val name = (n \ "@name").text

    if(Tpl.Cache.contains(name)){
      return Tpl.Cache(name)
    }

    val pp = if(n.attribute("ppClass").isDefined){Some(new PostProcessing((n \ "@ppClass").text, (n \ "parameters" \ "parameter").map(n=>{ ((n \ "@name").text, (n \ "@value").text) }).toMap))} else None

    val tplString = (n \ "wikiTemplate").text
    // expand the terse template syntax 
    val tplExpanded = expandTpl(tplString)

    //println(tplExpanded);
    val tplText = MyStack.fromString(tplExpanded).filterNewLines
    if(tplExpanded.startsWith("\n")){
      tplText.push(new TextNode("\n",0))
    }
    //println((n \ "@name").text + tpl)
    val rt = (n \ "resultTemplates" \ "resultTemplate").map(
            rtn => new ResultTemplate(
                (rtn \ "triples" \ "triple").map(
                    t => new TripleTemplate(t) 
                )
            )
   )
    
    //return
    val tpl = new Tpl(
      (n \ "@name").text,
      tplText,
      pp, 
      rt
    )

    Tpl.Cache(name) = tpl
    tpl
  }
}

class Block (n : XMLNode, val parent : Block){
      val name = (n \ "@name").text
      val indTpl = (n \ "indicators" \ "indicator").map(indTpl => Tpl.fromNode(indTpl))
      val blocks = (n \ "block").map(b => new Block(b, this)).toList
      val templates  = (n \ "templates" \ "template").map(t=>Tpl.fromNode(t)).toList
      val property = (n \ "@property").text
      val rt = new ResultTemplate((n \ "triples" \ "triple").map(t=>new TripleTemplate(t) ).toList)
}

class Page ( n : XMLNode ) extends Block (n, null
){

}
