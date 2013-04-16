package org.dbpedia.extraction.mappings.wikitemplate

import collection.mutable.{ListBuffer, Stack}
import io.{Source}
import util.control.Breaks._
import java.util.regex.Pattern
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.mappings.WiktionaryPageExtractor

import MyStack._
import MyNode._
import MyString._
import MyNodeList._
import MyLinkNode._

case class WiktionaryException(val s: String, val vars : VarBindingsHierarchical, val unexpectedNode : Option[Node]) extends  Exception(s) {}

case class ContinueException(val s: String) extends Exception(s) {}
/**
 * extend the stack class (by using a wrapper and implicit conversion - scala magic)
 */
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

  def toShortDumpString : String = {
    s.toList.toShortDumpString
  }

  def toWikiText : String = {
    s.toList.toWikiText
  }

  def toReadableString : String = {
    s.toList.toReadableString
  }

  /**
   * reverse a stack
   * the reverse function return (for some unknown reason) no stack, but a Seq...
   */
  def reversed : Stack[Node]  = {
     new Stack().pushAll(stack.reverse)
  }

  /**
   * return all nodes until we see a "list-end" node
   * if "list-start" nodes occur on the way, we need to keep track of them, so we dont see their "lower-level" "list-end" nodes as our searched list end
   */
  def getList : Stack[Node] = {
      val list = new ListBuffer[Node]()
      var i = 0
      breakable {
        while (stack.size > 0){
          val cur = stack.pop
          cur match {
            case tn : TemplateNode => {
              if(tn.title.decoded == "Extractiontpl"){
                val tplType = tn.property("1").get.children(0).asInstanceOf[TextNode].text
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

  /**
   * get the next "normal" node (no var node etc.)
   */
  def findNextNonTplNode() : Option[Node] =
    stack.find(
      node => !(node.isInstanceOf[TemplateNode] && node.asInstanceOf[TemplateNode].title.decoded == "Extractiontpl") && !(node.isInstanceOf[TextNode] && node.asInstanceOf[TextNode].text == "")
    )

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
    val otherStack = new Stack[Node]()
    //stack.foreach(n=>println(n.dumpStrShort))
    val list = stack.toList
    for(i <- list.indices) {
      if(i > 0 && i < list.indices.last){
        if(
          (
            (
            (list(i-1).isInstanceOf[SectionNode] && list(i+1).isInstanceOf[TemplateNode] && list(i+1).asInstanceOf[TemplateNode].title.decoded == "Extractiontpl")
            ||
            (list(i+1).isInstanceOf[SectionNode] && list(i-1).isInstanceOf[TemplateNode] && list(i-1).asInstanceOf[TemplateNode].title.decoded == "Extractiontpl")
            )
            && list(i).isInstanceOf[TextNode] && list(i).asInstanceOf[TextNode].text.startsWith("\n") && list(i).asInstanceOf[TextNode].text.length > 1
          )
        ){
           otherStack push list(i).asInstanceOf[TextNode].copy(text=list(i).asInstanceOf[TextNode].text.substring(1)) //strip that addional newline
        } else {
          otherStack push list(i) //ok
        }
      } else otherStack push list(i)
    }
    stack.clear
    stack.pushAll(otherStack)
    //println("after")
    //stack.foreach(n=>println(n.dumpStrShort))
    stack
  }

  def filterEmptyTextNodes() = {
    val otherStack = stack.reverse
    //println("filterEmptyTextNodes called")
    //stack.foreach(n=>println(n.dumpStrShort))
    stack.clear
    otherStack.foreach(i=> {
      i match {
        case tn : TextNode => if(!tn.text.equals("")){stack.push(i)}
        case sn : SectionNode => stack.push(sn.copy(children=sn.children.filter((c) => !c.isInstanceOf[TextNode] || !c.asInstanceOf[TextNode].text.equals(""))))
        case _ => stack push i
      }
    }
    )
    //println("after")
    //stack.foreach(n=>println(n.dumpStrShort))
    stack
  }

  /**
   * filter whitespaces
   */
  def filterSpaces() = {
    val otherStack = new Stack[Node]()
    //println("filter spaces called")
    //stack.foreach(n=>println(n.dumpStrShort))
    stack.foreach(i=> {
      i match {
        case tn : TextNode => otherStack push tn.copy(text=tn.text.replace("  ", " ").replace(" \n", "\n"))
        case _ => otherStack push i
      }
    })
    stack.clear
    stack.pushAll(otherStack)
    //println("after")
    //stack.foreach(n=>println(n.dumpStrShort))
    stack
  }

  def dropUntilAndPop(f : (Node) => Boolean) = {
    stack.dropWhile(!f(_))
    stack.pop
  }

  def reverseSwapList() = {
    val otherStack = new Stack[Node]()
    val thisReverse = stack.reverse.filter( (n : Node)=>
        n.isInstanceOf[TemplateNode] && 
        n.asInstanceOf[TemplateNode].title.decoded == "Extractiontpl" && 
        (
            n.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text == "list-start" || 
            n.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text == "list-end"
        )
    )
    val thisClone = stack.clone

    stack.foreach(i=> {

      i match {
        case tn : TemplateNode => if(tn.title.decoded == "Extractiontpl"){
                val tplType = tn.property("1").get.children(0).asInstanceOf[TextNode].text
                tplType match {
                  case "list-start" => {
                    val correspondingEnd = thisReverse.pop
                    otherStack push correspondingEnd
                  }
                  case "list-end" => {
                    val correspondingStart = thisReverse.pop
                    otherStack push correspondingStart
                  }
                  case _ => otherStack push i
                }
        } else {otherStack push i}
        case _ => otherStack push i
      }
    })
    otherStack
  }
}

object MyStack {
  /**
 * these functions tell how to convert to the wrapper implicitly
 */
  implicit def Stack2MyStack(s : Stack[Node]) : MyStack = { new MyStack(s) }
  implicit def MyStack2Stack(s : MyStack) : Stack[Node] = { s.stack }

  val parser = WikiParser.getInstance("sweble")
  /**
   * parse a string as wikisyntax and return the nodes as a stack
   */
  def fromString(in : String) : Stack[Node] = {
    //normalize

    //fix restrictive parsing of sections (must be \n== xy ==\n - but in case start of file or end of file, the newlines are omitted)
    //force leading and trailing \n
    var prependedNewline = false
    var appendedNewline = false
    //and useless whitespaces
    val str = 
        (if(in.startsWith("=")){prependedNewline = true; "\n"} else {""}) +
        in +
        (if(in.endsWith("=")){appendedNewline = true; "\n"} else {""})

    //println("after normalizations >"+str+"<")
    val page : PageNode = parser(
        new WikiPage(
          new WikiTitle("wiktionary extraction subtemplate", Namespace.Main, Language.English), str //parsing
        )
    )
    val nodes = new Stack[Node]()

    if(appendedNewline && (page.children.last match {case TextNode("\n", _)=>true; case _ => false})){
      nodes.pushAll(page.children.reverse.tail) //without the last
    } else {
      nodes.pushAll(page.children.reverse)
    }
    if(prependedNewline && (nodes.head match {case TextNode("\n", _)=>true; case _ => false})){
        nodes.pop
    }

    nodes.filterEmptyTextNodes
    nodes.filterSpaces

    //println("dumping subtemplate ")
    //nodes.foreach((n: Node) => println(n.dumpStrShort))

    nodes
  }

  /**
   * read a file containing wikisyntax and return the nodes as a stack
   * currently not used
   */
  def fromParsedFile(name : String) : Stack[Node] = {
    fromString(Source.fromFile(name).mkString)
  }

}

/**
 * possibility of scala to _kind of_ extend the language with own constructs:
 * i "define" the keywords measure and report...
 * code within the measure-block is executed with timekeeping (how many millisoconds the execution took)
 * the result is handed over to the report block, which needs to be a function (which prints it or so)
 */
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

/**
 * extend the string class with some "inner-trim" functionality
 */
class MyString(val str : String){
  //reduce multiple whitespaces and lines with only whitespaces. then trim
  def fullTrim() : String = str.replaceAll("\\r?\\n\\s{1,}\\r?\\n", "\n\n").replaceAll("^\\s{1,}\\r?\\n", "\n").replaceAll("\\r?\\n\\s{1,}$", "\n").replaceAll("\\s{2,}", " ")
}

object MyString {
  implicit def String2MyString(s : String) : MyString = new MyString(s)
  implicit def MyString2String(s : MyString) : String = s.str
}

object Logging {
  var level = 0  // will be read from config and then overwritten
  val st_depth_start = new Exception("").getStackTrace.length + 1

  //print info about a function call, and the template and page (the first n nodes)
  def printFuncDump(name : String, tplIt : Stack[Node], pageIt : Stack[Node], thisLevel : Int) : Unit = {
    if(thisLevel <= level){
      val st_depth = new Exception("").getStackTrace.length - st_depth_start
      val prefix =  " " * st_depth
      println(prefix + "------------")
      println(prefix + "<entering " + name +">")
      println(prefix + "<template (next 3)>")
      println(prefix + tplIt.take(3).map(_.dumpStrShort).mkString)
      println(prefix + "<page (next 3)>")
      println(prefix + pageIt.take(3).map(_.dumpStrShort).mkString)
      println(prefix + "------------\n\n")
    }
  }

  //print a message that is indented by it call stack depth (?) :)
  def printMsg(str : String, thisLevel : Int) : Unit = {
    if(thisLevel <= level){
      val st_depth = new Exception("").getStackTrace.length  - st_depth_start
      val prefix =  " " * st_depth
      println(prefix + str)
    }
  }
}

/**
 * wrapper class to extend the Node class (from dbpedia core) with some (crude) functionality
 */
class MyNode (val n : Node){
  def dump(depth : Int = 0) : Unit =
  {
    println(n.dumpStr(depth))
  }

  def dumpStr(depth : Int = 0) : String =
  {
    var str = ""
    //indent by depth
    val prefix = " "*depth

    //print class
    str += prefix + n.getClass +"\n"

    //dump node properties according to its type
    n match {
      case tn : TextNode =>  str += prefix+"text : "+tn.text +"\n"
      case tn : SectionNode => {
        str += prefix+"secname : "+tn.name +"\n"
        str += prefix+"level : "+tn.level +"\n"
      }
      case tn : TemplateNode =>  str += prefix+"tplname : "+tn.title.decoded +"\n"
      case tn : ExternalLinkNode =>  str += prefix+"destNodes : "+tn.destinationNodes +"\n"
      case tn : InternalLinkNode =>  str += prefix+"destNodes : "+tn.destinationNodes +"\n"
      case pn : PropertyNode =>
    }

    //dump children
    str += prefix+"children:  {" +"\n"
    n.children.foreach(
      child => str += child.dumpStr(depth + 2)
    )
    str += prefix+"}" +"\n"
    str
  }

  def dumpStrShort() : String =
  {
    n match {
      case tn : TemplateNode => "<tpl>"+tn.toWikiText+"</tpl> "
      case tn : TextNode => "<text>"+tn.toWikiText+"</text> "
      case ln : ExternalLinkNode => "<ExternalLinkNode>"+ln.toWikiText+"</link> "
      case ln : InternalLinkNode => "<InternalLinkNode>"+ln.toWikiText+"</link> "
      case ln : InterWikiLinkNode => "<InterWikiLinkNode>"+ln.toWikiText+"</link> "
      case sn : SectionNode=> "<section>"+sn.toWikiText+"</section> "
      case node : Node=> "<other "+node.getClass+">"+node.retrieveText.getOrElse("") + "</other> "
    }
  }

  //why are line numbers stored - this messes up the equals method,
  //we fix this using the scala 2.8 named constructor arguments aware copy method of case classes
  def equalsIgnoreLine(other : Node) : Boolean = {
    if(n.getClass != other.getClass){
      return false
    }
    
    //the copy method is not available in the implementation of the abstract node-class
    //so we cast to all subclasses
    n match {
      case tn : TemplateNode => other.asInstanceOf[TemplateNode].copy(line=0).equals(tn.copy(line=0))
      case tn : TextNode => other.asInstanceOf[TextNode].copy(line=0).equals(tn.copy(line=0))
      case tn : SectionNode => other.asInstanceOf[SectionNode].copy(line=0).equals(tn.copy(line=0))
      case tn : ExternalLinkNode => other.asInstanceOf[ExternalLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : InternalLinkNode => other.asInstanceOf[InternalLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : InterWikiLinkNode => other.asInstanceOf[InterWikiLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : PropertyNode => other.asInstanceOf[PropertyNode].copy(line=0).equals(tn.copy(line=0))
      case tn : TableNode => other.asInstanceOf[TableNode].copy(line=0).equals(tn.copy(line=0))
    }
  }

  def canMatchPageNode(other : Node) : Boolean = {
    n match {
      case tplNode : TemplateNode => if(tplNode.title.decoded.equals("Extractiontpl")) return true
      case secNode : SectionNode => if(other.isInstanceOf[SectionNode]) return other.asInstanceOf[SectionNode].level == secNode.level
      case txtNode : TextNode => {
        other match {
          case otherTxtNode  : TextNode => {
            return otherTxtNode.text.startsWith(txtNode.text)
          }
          case _ => return false
        }
      }
      case _ =>
    }
    return n.getClass == other.getClass
  }
}


object MyNode{
  implicit def Node2MyNode(node : Node) : MyNode = new MyNode(node)
  implicit def MyNode2Node(mynode : MyNode) : Node = mynode.n
}

class MyNodeList(val nl : List[Node]) {

  def toShortDumpString : String = {
    nl.map((node:Node) => node.dumpStrShort).mkString(" ")
  }

  def toWikiText : String = {
    nl.map((node:Node) => node.toWikiText).mkString(" ")
  }

  def toReadableString : String = nl.map(n => { n match {
    case tn : TemplateNode => if(WiktionaryPageExtractor.templateRepresentativeProperty.contains(tn.title.decoded)){
        val pKey = WiktionaryPageExtractor.templateRepresentativeProperty(tn.title.decoded)
        val p = tn.property(pKey)
        if(p.isDefined){
            p.get.children.toReadableString
        } else {
            ""
        }
    } else {
        ""
    }
    case ln : LinkNode => ln.children.toReadableString
    case _ => n.retrieveText.getOrElse("") 
  }}).mkString

  def toStack = new Stack[Node]() pushAll nl.reverse
}
object MyNodeList {
  implicit def MyNodeList2NodeList(mnl : MyNodeList) : List[Node] = mnl.nl
  implicit def NodeList2MyNodeList(nl : List[Node]) : MyNodeList = new MyNodeList(nl)
}

object MyLinkNode{
  implicit def LinkNode2MyLinkNode(node : LinkNode) : MyLinkNode = new MyLinkNode(node)
  implicit def MyLinkNode2LinkNode(mynode : MyLinkNode) : LinkNode = mynode.n
}

class MyLinkNode(val n : LinkNode){
  def getDestination : String = n match {
      case eln : ExternalLinkNode => eln.destination.toString
      case iln : InternalLinkNode => iln.destination.decoded
      case iwln : InterWikiLinkNode => iwln.destination.decoded
      case _  =>  throw new IllegalStateException(n.toPlainText + ": Link type not supported!")
  }
  def getFullDestination(ns:String) : String = {
      val rawDestination = n.getDestination                    
      if(n.isInstanceOf[ExternalLinkNode] || rawDestination.startsWith("http://")){
          //external link
          rawDestination
      } else {
          //interal link
          ns + WiktionaryPageExtractor.urify(rawDestination)
      }
  }
  def getLabel : String = n.children.toReadableString
}
