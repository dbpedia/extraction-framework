package org.dbpedia.extraction.mappings

import xml.Node
import xml.NodeSeq._
import collection.mutable.{ListBuffer, Stack}
import xml.{XML, Node => XMLNode}

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

class Var (val name : String, val property : String, val senseBound : Boolean, val toUri : Boolean, val format : String, val doMapping : Boolean)
object Var {
  def fromNode(n:XMLNode) = {
    WiktionaryLogging.printMsg("create var ", 0)
    new Var(
      (n \ "@name").text,
      (n \ "@property").text,
      n.attribute("senseBound").isDefined && (n \ "@senseBound").text.equals("true"),
      n.attribute("type").isDefined && (n \ "@type").text.equals("resource"),
      (n \ "@format").text,
      n.attribute("doMapping").isDefined && (n \ "@doMapping").text.equals("true")
    )
  }
}

class Tpl (val name : String, val tpl : Stack[org.dbpedia.extraction.wikiparser.Node], val vars : scala.collection.immutable.Seq[Var], var needsPostProcessing : Boolean, var ppClass : Option[String], var ppMethod : Option[String])
object Tpl {
  def fromNode(n:XMLNode) = {
    WiktionaryLogging.printMsg("create tpl ", 0)

    val pp = n.attribute("needsPostProcessing").isDefined && (n \ "@needsPostProcessing").text.equals("true")
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
    WiktionaryLogging.printMsg("create tpl before vars", 0)
    val vars = (n \ "vars" \ "var").map(Var.fromNode(_))
    WiktionaryLogging.printMsg("create tpl nodes  before", 0)
    val tpl =  MyStack.fromString((n \ "wikiSyntax").text).filterNewLines
    WiktionaryLogging.printMsg("create tpl nodes  after", 0)
    val t = new Tpl(
      (n \ "@name").text,
      tpl,
      vars,
      pp,
      ppClass,
      ppMethod
    )
    WiktionaryLogging.printMsg("after create tpl ", 0)
    t
  }
}

class Block (val indTpl : Tpl, val blocks : Option[Block], val templates : List[Tpl], val property : String){
  override def clone = new Block(indTpl, blocks, templates, property)
}
object Block {
  def fromNode(n:XMLNode) : Block = {
    WiktionaryLogging.printMsg("create block ", 0)
    new Block(
      Tpl.fromNode((n \ "template").head),
      if((n \ "block").size > 0){
        Some(Block.fromNode((n \ "block").head))
      }else{None},
      (n \ "templates" \ "template").map(Tpl.fromNode(_)).toList,
      (n \ "property" \ "@uri").text
    )
  }
}

class Page (blocks : Option[Block], templates : List[Tpl], property : String) extends Block (null, blocks, templates, property)
object Page {
  def fromNode(n:XMLNode) : Page =   {
    WiktionaryLogging.printMsg("create page ", 0)
    val p = new Page(
    if((n \ "block").size > 0){
      Some(Block.fromNode((n \ "block")(0)))
    }else{None},
    (n \ "templates" \ "template").map((n:Node)=>Tpl.fromNode(n)).toList,
    ""
    )
    WiktionaryLogging.printMsg("c112", 0)
    p
  }
}
