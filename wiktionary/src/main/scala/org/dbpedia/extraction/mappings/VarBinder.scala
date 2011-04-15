package org.dbpedia.extraction.mappings
import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import util.control.Breaks._
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map}
import xml.{XML, Node => XMLNode}

//some of my utilities
import MyStack._
import MyNode._
import WiktionaryLogging._

object VarBinder {
  private val language = "de"
  private val subTemplateCache = scala.collection.mutable.Map[String, Stack[Node]]()

  protected def recordVar(tplVarNode : TemplateNode, possibeEndMarkerNode: Option[Node], pageIt : Stack[Node]) : (String, List[Node]) = {
    val varValue = new ListBuffer[Node]()
    printFuncDump("recordVar", new Stack[Node](), pageIt)
    if(possibeEndMarkerNode.isEmpty){
      //when there is no end marker, we take everything we got
      varValue ++=  pageIt
      pageIt.clear
      //println("NO ENDMARKER")
    } else {
      val endMarkerNode = possibeEndMarkerNode.get
      printMsg("endmarker "+endMarkerNode.dumpStrShort)

      //record from the page till we see the endmarker
      var endMarkerFound = false
      breakable {
        while(pageIt.size > 0 ){
          val curNode = pageIt.pop
          //printMsg("curNode "+dumpStrShort(curNode))

          //check for end of the var
          if(endMarkerNode.equalsIgnoreLine(curNode)) {
            //println("endmarker found (equal)")
            endMarkerFound = true
            break
          } else if(curNode.isInstanceOf[TextNode] && endMarkerNode.isInstanceOf[TextNode]){
            if(curNode.asInstanceOf[TextNode].text.equals(endMarkerNode.asInstanceOf[TextNode].text)){
              //println("endmarker found (string equal)")
              endMarkerFound = true
              break
            }
            val idx = curNode.asInstanceOf[TextNode].text.indexOf(endMarkerNode.asInstanceOf[TextNode].text)

            if(idx >= 0){
              //println("endmarker found (substr)")
              endMarkerFound = true
              //if the end marker is __in__ the current node . we take what we need
              val part1 =  curNode.asInstanceOf[TextNode].text.substring(0, idx)  //the endmarker is cut out and thrown away
              val part2 =  curNode.asInstanceOf[TextNode].text.substring(idx+endMarkerNode.asInstanceOf[TextNode].text.size, curNode.asInstanceOf[TextNode].text.size)
              if(!part1.isEmpty){
                 printMsg("var += "+part1)
                varValue append new TextNode(part1, curNode.line)
              }
              //and put the rest back
              if(!part2.isEmpty){
                printMsg("putting back >"+part2+"<")
                pageIt.prependString(part2)
              }
              break
            }
          }

          //not finished, keep recording
          varValue append curNode
          printMsg("var += "+curNode)
        }
      }
      if(!endMarkerFound){
        throw new VarException
      }
    }
    //return tuple consisting of var name and var value
    return (tplVarNode.property("2").get.children(0).asInstanceOf[TextNode].text, varValue.toList)
  }

  //extract does one step e.g. parse a var, or a list etc and then returns
  def parseNode(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseNode", tplIt, pageIt)
    val bindings = new VarBindingsHierarchical
    val currNodeFromTemplate = tplIt.pop
    val currNodeFromPage = pageIt.head    // we dont consume the pagenode here - needs to be consumed after processing
    var pageItCopy = pageIt.clone

    //early detection of error or no action
    if(currNodeFromTemplate.equalsIgnoreLine(currNodeFromPage)){
      //simple match
      pageIt.pop //consume page node
      return bindings
    } else //determine whether they CAN equal
    if(!currNodeFromTemplate.canMatchPageNode(currNodeFromPage)){
      //println("early mismatch: "+currNodeFromTemplate.dumpStrShort+" "+currNodeFromPage.dumpStrShort)
      throw new WiktionaryException("the template does not match the page - different type", bindings, Some(currNodeFromPage))
    }

    currNodeFromTemplate match {
      case tplNodeFromTpl : TemplateNode => {
        if(tplNodeFromTpl.title.decoded == "Extractiontpl"){
          val tplType = tplNodeFromTpl.property("1").get.children(0).asInstanceOf[TextNode].text
          tplType match {
            case "list-start" =>  {
              //val name =  tplNodeFromTpl.property("2").get.children(0).asInstanceOf[TextNode].text
              //take everything from tpl till list is closed
              val listTpl = tplIt.getList
              //take the node after the list as endmarker of this list
              val endMarkerNode = tplIt.findNextNonTplNode  //that will be the node that follows the list in the template
              bindings addChild parseList(listTpl, pageIt, endMarkerNode)
            }
            case "list-end" =>    println("end list - you should not see this")
            case "var" => {
              val endMarkerNode = if(tplIt.size > 0) Some(tplIt.pop) else None
              val binding = recordVar(currNodeFromTemplate.asInstanceOf[TemplateNode], endMarkerNode, pageIt)
              bindings.addBinding(binding._1, binding._2)
            }
            case "link" => {
              if(!currNodeFromPage.isInstanceOf[LinkNode]){
                throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
              }
              val destination = currNodeFromPage match {
                case iln : InternalLinkNode => new TextNode(iln.destination.decodedWithNamespace, 0)
                case iwln : InterWikiLinkNode => new TextNode(iwln.destination.decodedWithNamespace, 0)
                case eln : ExternalLinkNode => new TextNode(eln.destination.toString, 0)
              }
              //extract from the destination link
              bindings addChild parseNodesWithTemplate(
                new Stack[Node]() pushAll tplNodeFromTpl.property("2").get.children.reverse,
                new Stack[Node]() push destination
              )
              if(tplNodeFromTpl.property("3").isDefined){
                //extract from the label
                bindings addChild parseNodesWithTemplate(
                  new Stack[Node]() pushAll tplNodeFromTpl.property("3").get.children.reverse,
                  new Stack[Node]() pushAll currNodeFromPage.children
                )
              }
              pageIt.pop
            }
            case _ =>  { }
          }
        } else {
          //both are normal template nodes
          //parse template properties
          currNodeFromPage match {
            case tplNodeFromPage : TemplateNode => {
              if(!tplNodeFromPage.title.decoded.equals(tplNodeFromTpl.title.decoded)) {
                throw new WiktionaryException("the template does not match the page: unmatched template title", bindings, Some(currNodeFromPage))
              }
              breakable {
                for(key <- tplNodeFromTpl.keySet){
                  if(tplNodeFromTpl.property(key).isDefined && tplNodeFromPage.property(key).isDefined){
                      bindings addChild parseNodesWithTemplate(
                        new Stack[Node]() pushAll tplNodeFromTpl.property(key).get.children.reverse,
                        new Stack[Node]() pushAll tplNodeFromPage.property(key).get.children.reverse
                      )
                  } else {
                    break
                  }
                }
              }
              pageIt.pop
            }
            case _ => {
              printMsg("you should not see this: shouldve been detected earlier")
              throw new WiktionaryException("the template does not match the page: unmatched template property", bindings, Some(currNodeFromPage))
            }
          }
        }
      }
      case textNodeFromTpl : TextNode => {
        //variables can start recording in the middle of a textnode
        currNodeFromPage match {
           case textNodeFromPage : TextNode => {
              if(textNodeFromPage.text.startsWith(textNodeFromTpl.text) && !textNodeFromPage.text.equals(textNodeFromTpl.text)){
                //consume the current node from page
                pageIt.pop

                //cut out whats left (remove what is matched by textNodeFromTpl -> the prefix)
                val remainder = textNodeFromPage.text.substring(textNodeFromTpl.text.size, textNodeFromPage.text.size)

                pageIt.prependString(remainder)
              } else {
                restore(pageIt, pageItCopy)  //still needed? dont think so
                throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
              }
            }
            case _ => {
              printMsg("you should not see this: shouldve been detected earlier")
              throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
            }
        }
      }

      case _ => {
        if(currNodeFromPage.getClass != currNodeFromTemplate.getClass){
          restore(pageIt, pageItCopy) //still needed? dont think so
          printMsg("you should not see this: shouldve been detected earlier")
          throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
        } else {
          if(!(currNodeFromPage.isInstanceOf[SectionNode] && currNodeFromTemplate.isInstanceOf[SectionNode] &&
            currNodeFromPage.asInstanceOf[SectionNode].level != currNodeFromTemplate.asInstanceOf[SectionNode].level)){
            printMsg("same class but not equal. do recursion on children")
            bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll currNodeFromTemplate.children.reverse, new Stack[Node]() pushAll pageIt.pop.children.reverse)
          }
        }
      }
    }
    bindings
  }

  protected def restore(st : Stack[Node], backup : Stack[Node]) : Unit = {
    st.clear
    st pushAll backup.reverse
  }

  protected def parseList(tplIt : Stack[Node], pageIt : Stack[Node], endMarkerNode : Option[Node]) : VarBindingsHierarchical = {
    //printFuncDump("parseList "+name, tplIt, pageIt)
    val bindings = new VarBindingsHierarchical

    try {
      breakable {
        while(pageIt.size > 0 ){
          if(endMarkerNode.isDefined &&
            (
              (pageIt.size > 0 && pageIt.head.equalsIgnoreLine(endMarkerNode.get)) ||
              (pageIt.head.isInstanceOf[TextNode] && endMarkerNode.get.isInstanceOf[TextNode] &&
                pageIt.head.asInstanceOf[TextNode].text.startsWith(endMarkerNode.get.asInstanceOf[TextNode].text)
              )
            )
          ){    //TODO liststart = endmarker
            //printMsg("list ended by endmarker")
            break
          }

          //recurse
          val copyOfTpl = tplIt.clone  //the parsing consumes the template so for multiple matches we need to duplicate it
          bindings addChild parseNodesWithTemplate(copyOfTpl, pageIt)
        }
      }
    } catch {
      case e : WiktionaryException => printMsg("parseList caught an exception - list ended "+e) // now we know the list was finished
      bindings addChild e.vars
    }
    bindings
  }

  // parses the complete buffer
  def parseNodesWithTemplate(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseNodesWithTemplate", tplIt, pageIt)
    val pageItCopy = pageIt.clone

    val bindings = new VarBindingsHierarchical
    while(tplIt.size > 0 && pageIt.size > 0){
        try {
            bindings addChild parseNode(tplIt, pageIt)
        } catch {
          case e : WiktionaryException => {
            pageIt.clear
            pageIt.pushAll(pageItCopy.reverse)
            bindings addChild e.vars   // merge current bindings with previous and "return" them
            throw e.copy(vars=bindings)
          }
        }
    }
    //bindings.dump(0)
    bindings
  }
}

class VarBindingsHierarchical (){
  val children  = new ListBuffer[VarBindingsHierarchical]()
  val bindings = new HashMap[String, List[Node]]()

  def addBinding(name : String, value : List[Node]) : Unit = {
    bindings += (name -> value)
  }
  def addChild(sub : VarBindingsHierarchical) : Unit = {
    if(sub.bindings.size > 0 || sub.children.size > 0){
      children += sub.reduce
    }
  }
  def mergeWith(other : VarBindingsHierarchical) = {
    children ++= other.children
    bindings ++= other.bindings
  }

  def dump(depth : Int = 0){
    if(WiktionaryLogging.enabled){
      val prefix = " "*depth
      println(prefix+"{")
      for(key <- bindings.keySet){
        println(prefix+key+" -> "+bindings.apply(key))
      }
      for(child <- children){
        child.dump(depth + 2)
      }
      println(prefix+"}")
    }
  }

  //make the structure absolutly flat, maybe this is not what you want
  def flat : VarBindingsHierarchical = {
    val copi = new VarBindingsHierarchical
    for(child <- children){
      copi addChild child.flat
    }
    for(key <- bindings.keySet){
      copi.addBinding(key, bindings.apply(key))
    }
    copi
  }

  //remove unnecessary deep paths
  def reduce : VarBindingsHierarchical = {
    val copi = new VarBindingsHierarchical
    if(children.size == 1){
      copi mergeWith children(0).reduce
    } else {
      copi.children ++= children
    }
    //copy bindings
    for(key <- bindings.keySet){
      copi.addBinding(key, bindings.apply(key))
    }
    copi
  }

  def getFirstBinding(key : String) : Option[List[Node]] = {
    if(bindings.contains(key)){
      return Some(bindings(key))
    } else {
      for(child <- children){
        val possibleLang = child.getFirstBinding(key)
        if(possibleLang.isDefined){
          return possibleLang
        }
      }
      return None
    }
  }

  def getSenseBoundVarBinding(key : String) : Map[List[Node], List[Node]] = {
    val ret : Map[List[Node], List[Node]] = new HashMap()
    for(child <- children){
      if(child.bindings.contains(key)){
        val binding = child.bindings(key)
        val sense = getFirstBinding("meaning_id");
        if(sense.isDefined){
          ret += (sense.get -> binding)
        }
      } else {
        ret ++= child.getSenseBoundVarBinding(key)
      }
    }
    ret
  }

  def flatLangPos() : Map[Tuple2[String,String], VarBindingsHierarchical] = {
    val ret : Map[Tuple2[String,String], VarBindingsHierarchical] = new HashMap()
    for(child <- children){ //assumes that this is the top level VarBindingsHierachical object and that each child represents a (lang,pos)-block
      val lang = child.getFirstBinding("language")
      val pos  = child.getFirstBinding("pos")
      if(lang.isDefined && pos.isDefined){
        val langStr = lang.get.apply(0).asInstanceOf[TextNode].text   //assumes that the lang var is bound to e.g. List(TextNode(english))
        val posStr  = pos.get.apply(0).asInstanceOf[TextNode].text    //assumes that the pos  var is bound to e.g. List(TextNode(verb))
        ret += ((langStr, posStr) -> child)
      }
    }
    ret
  }

  def sortByVars() : Tuple2[Map[String, List[Node]], Map[List[Node],Map[String, List[Node]]]] = {
    val normalBindings : Map[String, List[Node]]= new HashMap()
    //get normal var bindings out (these that are unique to a (word,lang,pos) combination)
    for(varName <- VarBindingsHierarchical.vars){
      val currBindings = getFirstBinding(varName)
      if(currBindings.isDefined){
        normalBindings += (varName -> currBindings.get)
      }
    }
    val senseBindingsConverted : Map[List[Node],Map[String, List[Node]]] = new HashMap()
    //get the sense-bound vars (each sense can have a own binding for that var)
    for(varName <- VarBindingsHierarchical.senseVars){
      val senseBindings = getSenseBoundVarBinding(varName)
      val senses = senseBindings.keySet
      for(sense <- senses){
        if(!senseBindingsConverted.contains(sense)){
          senseBindingsConverted += (sense -> new HashMap())
        }
        senseBindingsConverted(sense) += (varName -> senseBindings(sense))
      }
    }
    return (normalBindings, senseBindingsConverted)
  }
}
object VarBindingsHierarchical {
  //TODO use config
  val vars = Set("hyphenation-singular", "hyphenation-plural", "pronunciation-singular", "pronunciation-plural",
    "audio-singular", "audio-plural")
  val senseVars = Set("meaning")
}

