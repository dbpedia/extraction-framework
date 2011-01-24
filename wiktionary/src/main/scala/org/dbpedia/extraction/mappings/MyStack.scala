package org.dbpedia.extraction.mappings

import collection.mutable.{ListBuffer, Stack}
import util.control.Breaks._
import java.util.regex.Pattern
import org.dbpedia.extraction.wikiparser._
import MyStack._
import MyStringTrimmer._

class MyStack(s : Stack[Node]) {
  val stack : Stack[Node] = s
  def prependString(str : String) : Unit  = {
    if(stack.head.isInstanceOf[TextNode]){
      val head = stack.pop
      val newhead = new TextNode(str + head.asInstanceOf[TextNode].text, head.line)
      stack.push(newhead)
    } else {
      val newhead = new TextNode(str, stack.head.line)
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

  def findNextNonTplNode() : Option[Node] =
    stack.find(
      node => !(node.isInstanceOf[TemplateNode] && node.asInstanceOf[TemplateNode].title.decoded == "Extractiontpl"))

  def fullTrimmedPop() : Node = {
    if(stack.head.isInstanceOf[TextNode]){
      val next = stack.pop.asInstanceOf[TextNode]
      return next.copy(text=next.text.fullTrim)
    } else return stack.pop
  }
  def fullTrimmedHead() : Node = {
    if(stack.head.isInstanceOf[TextNode]){
      val next = stack.pop.asInstanceOf[TextNode]
      val nextReplaced = next.copy(text=next.text.fullTrim)
      stack.push(nextReplaced)
    }
    return stack.head
  }

  /**
   * filters newline textnodes that come between a extractiontpl-template and a section node
   *
   */
  def filterNewLines() = {
    var wait = false
    var count = 0
    val otherStack = new  Stack[Node]()
     stack.foreach(n => {
       n match {
         case tn : TemplateNode => if(tn.title.decoded == "Extractiontpl") wait = true
         case sn : SectionNode => if(wait == true) {
           wait = false
           count = 0
           otherStack.head match {
             case textNode : TextNode => if(textNode.text.equals("")){
               otherStack.pop //drop that text node
             }
             case _ =>
           }

         }
         case _ =>
       }
       otherStack.push(n)

       if(count == 3){
         count = 0
         wait = false
       }

       if(wait){
         count += 1
       }
     })
    stack.clear
    stack.pushAll(otherStack)
  }
}

object MyStack {
  implicit def convert1(s : Stack[Node]) : MyStack = { new MyStack(s) }
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
  //reduce multiple whitespaces and lines with only whitespaces. then trim
  def fullTrim() : String = str.replaceAll("\\r?\\n\\s{1,}\\r?\\n", "\n\n").replaceAll("^\\s{1,}\\r?\\n", "\n").replaceAll("\\r?\\n\\s{1,}$", "\n").replaceAll("\\s{2,}", " ").trim
}
object MyStringTrimmer {
  implicit def String2MyStringTrimmer(s : String) : MyStringTrimmer = new MyStringTrimmer(s)
  implicit def MyStringTrimmer2String(s : MyStringTrimmer) : String = s.str
}