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
    if(stack.size == 0){
      stack push new TextNode(str,0)
    } else {
      if(stack.head.isInstanceOf[TextNode]){
        val head = stack.pop
        val newhead = new TextNode(str + head.asInstanceOf[TextNode].text, head.line)
        stack.push(newhead)
      } else {
        val newhead = new TextNode(str, stack.head.line)
        stack.push(newhead)
      }
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
      var next = stack.pop
      while(next.isInstanceOf[TextNode] && next.asInstanceOf[TextNode].text.fullTrim.equals("")){
        next = stack.pop
      }
      next match {
        case tn : TextNode => return tn.copy(text=tn.text.fullTrim)
        case _ => return next
      }
    } else return stack.pop
  }

  def fullTrimmedHead() : Node = {
    val node = stack.fullTrimmedPop
    stack.push(node)
    return node
  }

  def filterTrimmed = {
    val otherStack = new  Stack[Node]()
    while(stack.size > 0){
      try {
        otherStack push stack.fullTrimmedPop
      } catch {
        case e : NoSuchElementException =>
      }
    }
    stack.clear
    stack.pushAll(otherStack)
  }

  /**
   * filters newline textnodes that come between a extractiontpl-template and a section node
   *
   * example page
   * == sec ==
   * === sec2 ==
   * ...
   *
   * should be parsed with the template
   * {{extractiontpl|list-start}}
   * == sec ==
   * {{extractiontpl|list-start}}
   * === sec 2 ==
   * ...
   *
   * on the parsed page there will be only two section nodes
   * on the parsed template there will be this:
   * <tpl><tn="\n"><sec><tpl>
   */
  def filterNewLines() = {
    val otherStack = new  Stack[Node]()
    val list = stack.toList
    for(i <- list.indices) {
      if(i > 0 && i < list.indices.last){
        if(!
          (
            (
            (list(i-1).isInstanceOf[SectionNode] && list(i+1).isInstanceOf[TemplateNode] && list(i+1).asInstanceOf[TemplateNode].title.decoded == "Extractiontpl")
            ||
            (list(i+1).isInstanceOf[SectionNode] && list(i-1).isInstanceOf[TemplateNode] && list(i-1).asInstanceOf[TemplateNode].title.decoded == "Extractiontpl")
            )
            && list(i).isInstanceOf[TextNode] && list(i).asInstanceOf[TextNode].text.equals("\n")
          )
        ){
           otherStack push list(i)
        } else {
          //println("filterNewLines: leave out " +list(i) +" context: "+list(i-1)+list(i)+list(i+1))
        }
      } else otherStack push list(i)
    }
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
  def fullTrim() : String = str.replaceAll("\\r?\\n\\s{1,}\\r?\\n", "\n\n").replaceAll("^\\s{1,}\\r?\\n", "\n").replaceAll("\\r?\\n\\s{1,}$", "\n").replaceAll("\\s{2,}", " ")
}
object MyStringTrimmer {
  implicit def String2MyStringTrimmer(s : String) : MyStringTrimmer = new MyStringTrimmer(s)
  implicit def MyStringTrimmer2String(s : MyStringTrimmer) : String = s.str
}