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

class Var (val name : String, val property : String, val senseBound : Boolean, val toUri : Boolean, val format : String, val doMapping : Boolean, val values : List[String])
object Var {
  def fromNode(n:XMLNode) = {
    new Var(
      (n \ "@name").text,
      (n \ "@property").text,
      n.attribute("senseBound").isDefined && (n \ "@senseBound").text.equals("true"),
      n.attribute("type").isDefined && (n \ "@type").text.equals("resource"),
      (n \ "@format").text,
      n.attribute("doMapping").isDefined && (n \ "@doMapping").text.equals("true"),
      (n \ "mapping").map((mapping:XMLNode)=>(mapping \"@from").text).asInstanceOf[List[String]]
    )
  }
}

class Tpl (val name : String, val tpl : Stack[org.dbpedia.extraction.wikiparser.Node], val vars : scala.collection.immutable.Seq[Var], var needsPostProcessing : Boolean, var ppClass : Option[String], var ppMethod : Option[String])
object Tpl {
  //$var, () for repetitions, and ##link## to subsumpt the three link types
  def expandTpl(orig : String) : String = {
    println(orig)
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
    println(tplExpanded)
    return tplExpanded
  }

  def fromNode(n:XMLNode) = {
    val pp = n.attribute("needsPostProcessing").isDefined && (n \ "@needsPostProcessing").text.equals("true")

    //post prcessing: class and method names to call via reflection    
    val ppClass = if(pp){
      Some((n \ "@ppClass").text)
    } else {
      None
    }
    val ppMethod = if(pp){
      Some((n \ "@ppMethod").text)
    } else {
      None
    }

    val vars = (n \ "vars" \ "var").map(Var.fromNode(_))
    val tplString = (n \ "wikiSyntax").text
    // expand the terse template syntax 
    val tplExpanded = expandTpl(tplString)

    //println(tplExpanded);
    val tpl =  MyStack.fromString(tplExpanded).filterNewLines
    if(tplExpanded.startsWith("\n")){
        tpl.push(new TextNode("\n",0))
    }
    println(tpl)
    val t = new Tpl(
      (n \ "@name").text,
      tpl,
      vars,
      pp,
      ppClass,
      ppMethod
    )
    t
  }
}

class Block (n : XMLNode, val parent : Block
//val name : String, val indTpl : Tpl, val blocks : List[Block], val templates : List[Tpl], val property : String, val parent : Block
){
      val name = (n \ "@name").text
      val indTpl = if((n \ "template").size > 0) {Tpl.fromNode((n \ "template").head)} else null
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
