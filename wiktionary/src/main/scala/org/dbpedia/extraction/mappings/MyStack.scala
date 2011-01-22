package org.dbpedia.extraction.mappings

import collection.mutable.{ListBuffer, Stack}
import org.dbpedia.extraction.wikiparser.{TemplateNode, TextNode, Node}
import util.control.Breaks._
import MyStringTrimmer._
import java.util.regex.Pattern


class MyStack(s : Stack[Node]) {
  val stack : Stack[Node] = s
  def prependString(str : String) : Unit  = {
    if(stack.head.isInstanceOf[TextNode]){
      val head = stack.pop
      val newhead = new TextNode(str + head.asInstanceOf[TextNode].text, head.asInstanceOf[Node].line)
      stack.push(newhead)
    } else {
      val newhead = new TextNode(str, stack.head.asInstanceOf[Node].line)
      stack.push(newhead)
    }
  }

  def reversed : Stack[Node]  = {
     new Stack().pushAll(stack.reverse)
  }

   def getList : Stack[Node] = {
      val list = new ListBuffer[Node]()
      var i=0
      breakable {
        while (stack.size > 0){
          val cur = stack.pop
          cur match {
            case tn : TemplateNode => {
              if(cur.asInstanceOf[TemplateNode].title.decoded == "Extractiontpl"){
                val tplType = cur.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text
                tplType match {
                  case "list-start" =>     i += 1
                  case "list-end" =>       {
                    if(i == 0){
                      break  //return inner of the list. the list start was consumed before calling this function, now we return before appending the end marker
                    } else {
                      i -= 1
                    }
                  }
                  case _ =>
                }
              }
            }
            case _ =>
          }
          list prepend cur
        }
      }
      val st = new Stack[Node]() pushAll list
      st
    }

  def findNextNonTplNode() : Option[Node] = stack.find(node => !(node.isInstanceOf[TemplateNode] && node.asInstanceOf[TemplateNode].title.decoded == "Extractiontpl"))

  //fulltrim textnodes (all multiple whitespaces and linebreaks are reduced to single ones) and remove textnodes, that are empty afterwards
  def getFullTrimmed() : Stack[Node] = {
    val copy = stack.clone
    val ret = new Stack[Node]()
    stack.foreach(node => {
      node match {
        case tn : TextNode => {
          if(tn.text.fullTrim.equals("")){
            var nextNode : Node = stack.pop
            while (nextNode.isInstanceOf[TextNode] && nextNode.asInstanceOf[TextNode].text.fullTrim.equals("")){
              nextNode = stack.pop
            }
            nextNode match {
              case tn2 : TextNode => ret push tn2.asInstanceOf[TextNode].copy(text=tn2.text.fullTrim)
              case n : Node =>  ret push n.copy(children=new Stack[Node]().pushAll(n.children.reverse).getFullTrimmed.toList)
            }
          } else {
            ret push tn.copy(text=tn.text.fullTrim)
          }
        }
        case n : Node => ret push n.copy(children=new Stack[Node]().pushAll(n.children.reverse).getFullTrimmed.toList)
      }
    })
    return new Stack[Node]() pushAll ret  //reverse
  }
}
object MyStack {
  implicit def convert1(s : Stack[Node]) : MyStack = {new MyStack(s)}
  implicit def convert2(s : MyStack) : Stack[Node] = { s.stack }
}

object TimeMeasurement {
  def measure(code : => Unit) = new {
    def report(reporterFunc : Long => Unit) = {
      val before = System.currentTimeMillis
      code
      val after = System.currentTimeMillis
      val duration = after - before
      reporterFunc(duration)
    }
  }
}

class MyStringTrimmer(s : String){
  val str = s
  //reduce multiple whitespaces and linebreaks then trim
  def fullTrim() : String = str.replaceAll("\\r?\\n{2,}", "\n").replaceAll("\\s{2,}", " ").trim
}
object MyStringTrimmer {
  implicit def String2MyStringTrimmer(s : String) : MyStringTrimmer = new MyStringTrimmer(s)
  implicit def MyStringTrimmer2String(s : MyStringTrimmer) : String = s.str
}