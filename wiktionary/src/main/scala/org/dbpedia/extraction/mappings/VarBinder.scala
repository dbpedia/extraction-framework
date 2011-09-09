package org.dbpedia.extraction.mappings
import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import util.control.Breaks._
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map, Queue}
import xml.{XML, Node => XMLNode}

//some of my utilities
import MyStack._
import MyNode._
import WiktionaryLogging._

/**
 * static functions composing a mechanism to match a template (containing variables) to a actual page, and bind values to the variables
 *
 * entry function is parseNodesWithTemplate
 */
object VarBinder {
  //subtemplates are somewhat deprecated, but
  // this caches them to avoid repeated read of files
  private val subTemplateCache = scala.collection.mutable.Map[String, Stack[Node]]()

   /**
   * given a template and a page, match the template to the page, return VarBindings
   *
   */
  def parseNodesWithTemplate(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseNodesWithTemplate", tplIt, pageIt)
    //used as a backup (in case of a exeption when the template does not match)
    val pageItCopy = pageIt.clone

    //we try to do some fuzzy parsing, we allow 1 error in a sliding window of 5 nodes
    val lastResults = new Queue[Boolean]();
    val windowSize = 5
    val maxFailures = 1
    val minCorrect = 1  //but there needs to be at least one correct, to prevent buffers of size 1 with one mismatch to count as correct

    val bindings = new VarBindingsHierarchical
    while(tplIt.size > 0 && pageIt.size > 0){
        try {
          //try to match node-by-node
          if(lastResults.size == windowSize){
            lastResults.dequeue()
          }
          bindings addChild parseNode(tplIt, pageIt)
          lastResults.enqueue(true)
        } catch {
          case e : WiktionaryException => {
            //the template does not match the page
            lastResults.enqueue(false)
            val failures = lastResults.count(!_)
            val correct = lastResults.size - failures
            printMsg("failures="+failures+" correct="+correct+" queue="+lastResults)
            if(failures > maxFailures || correct < minCorrect){
              //too many errors
              printMsg("too many errors")
              pageIt.clear
              pageIt.pushAll(pageItCopy.reverse)  // restore the page
              bindings addChild e.vars   // merge current bindings with previous and "return" them
              throw e.copy(vars=bindings)
            }
          }
        }
    }
    //bindings.dump(0)
    bindings
  }

  /**
   *  matches a template node to a page node
   *  if both a normal wikisyntax nodes - check of they match: if true return, if not throw exception
   *  if the template node is a "special node" (indicating e.g. a variable or list), trigger their handling
   */
  protected def parseNode(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
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
              val endMarkerNode = tplIt.findNextNonTplNode
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
              printMsg("you should not see this: shouldve been detected earlier (node type does not match)")
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
              printMsg("you should not see this: shouldve been detected earlier (node type does not match)")
              throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
            }
        }
      }

      case _ => {
        if(currNodeFromPage.getClass != currNodeFromTemplate.getClass){
          restore(pageIt, pageItCopy) //still needed? dont think so
          printMsg("you should not see this: shouldve been detected earlier (node type does not match)")
          throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
        } else {
          if(!(currNodeFromPage.isInstanceOf[SectionNode] && currNodeFromTemplate.isInstanceOf[SectionNode] &&
            currNodeFromPage.asInstanceOf[SectionNode].level != currNodeFromTemplate.asInstanceOf[SectionNode].level)){
            printMsg("same class but not equal. do recursion on children")
            bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll currNodeFromTemplate.children.reverse, new Stack[Node]() pushAll pageIt.pop.children.reverse)
          } else {
            //sections with different level
            //TODO check canEqual
            printMsg("you should not see this: shouldve been detected earlier (section nodes with different level)")
            restore(pageIt, pageItCopy)
            throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
          }
        }
      }
    }
    bindings
  }

  /**
   * in the template there can be defined "lists" which are repetitive parts, like in regex: tro(lo)* matches trolololo
   */
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

  /**
   * given a var-node and a page and an optional endmarker, bind the first n nodes to the var,
   * until the endmarker is seen or the page ends. bind means returning a tuple of varname and nodes
   */
  protected def recordVar(tplVarNode : TemplateNode, possibeEndMarkerNode: Option[Node], pageIt : Stack[Node]) : (String, List[Node]) = {
    val varValue = new ListBuffer[Node]()
    printFuncDump("recordVar", new Stack[Node](), pageIt)
    if(possibeEndMarkerNode.isEmpty){
      //when there is no end marker, we take everything we got
      varValue ++=  pageIt
      pageIt.clear
      printMsg("no endmarker. taking all.")
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
              } else {
                printMsg("putting nothing back")
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
        throw new WiktionaryException("endMarker of variable not found", new VarBindingsHierarchical, None)
      } else {
        pageIt push endMarkerNode
      }
    }
    //return tuple consisting of var name and var value
    return (tplVarNode.property("2").get.children(0).asInstanceOf[TextNode].text, varValue.toList)
  }


  /**
   * silly helper function
   */
  protected def restore(st : Stack[Node], backup : Stack[Node]) : Unit = {
    st.clear
    st pushAll backup.reverse
  }
}

/**
 * represents bound variables
 * contains a mapping from var names to page nodes
 * and can contain recursivly other VarBindingsHierarchical objects - forming a hierarchy that corresponds to the occurence in parsetree (somewhat strange, maybe unneccesary)
 */
class VarBindingsHierarchical (){
  val children  = new ListBuffer[VarBindingsHierarchical]()
  val bindings = new HashMap[String, List[Node]]()

  /**
   * add a binding
   */
  def addBinding(name : String, value : List[Node]) : Unit = {
    bindings += (name -> value)
  }

  /**
   * add a child tree of varbindings
   */
  def addChild(sub : VarBindingsHierarchical) : Unit = {
    if(sub.bindings.size > 0 || sub.children.size > 0){
      children += sub.reduce //avoids unbranched arms in the tree
    }
  }
  def mergeWith(other : VarBindingsHierarchical) = {
    children ++= other.children
    bindings ++= other.bindings
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

  /**
   * given a variable name, find the first binding in this varbindings instance
   */
  def getFirstBinding(key : String) : Option[List[Node]] = {
    //TODO maybe a breadth-first-search suits better
    if(bindings.contains(key)){
      return Some(bindings(key))
    } else {
      for(child <- children){
        val possibleBinding = child.getFirstBinding(key)
        if(possibleBinding.isDefined){
          return possibleBinding
        }
      }
      return None
    }
  }

  /**
   * given a variable name, find all bindings in this varbindings instance
   */
  def getAllBindings(key : String) : List[List[Node]] = {
    if(bindings.contains(key)){
      return List(bindings(key))
    } else {
      val otherBindings = new ListBuffer[List[Node]]()
      for(child <- children){
        otherBindings appendAll child.getAllBindings(key)
      }
      return otherBindings.toList //to immutable
    }
  }

  /**
   * given a variable name of a sense-bound var, retrieve a mapping from sense-identifier to binding (distinct)
   */
  def getFirstSenseBoundVarBinding(key : String, meaningIdVarName : String) : Map[List[Node], List[Node]] = {
    val ret = new HashMap[List[Node], List[Node]]()
    for(child <- children){
      if(child.bindings.contains(key)){
        val binding = child.bindings(key)
        //TODO expand notations like "[1-3] xyz" to "[1] xyz\n[2] xyz\n[3} xyz"
        val sense = getFirstBinding(meaningIdVarName);
        if(sense.isDefined){
          ret += (sense.get -> binding)
        }
      } else {
        ret ++= child.getFirstSenseBoundVarBinding(key, meaningIdVarName)
      }
    }
    ret
  }

  /**
   * given a variable name of a sense-bound var, retrieve a mapping from sense-identifier to binding (with multiple values)
   */
  def getAllSenseBoundVarBindings(key : String, meaningIdVarName : String) : Map[List[Node], ListBuffer[List[Node]]] = {
    val ret =  new HashMap[List[Node], ListBuffer[List[Node]]]()
    for(child <- children){
      if(child.bindings.contains(key)){
        val binding = child.bindings(key)
        //TODO expand notations like "[1-3] xyz" to "[1] xyz\n[2] xyz\n[3} xyz"
        val sense = getFirstBinding(meaningIdVarName);
        if(sense.isDefined){
          if(!ret.contains(sense.get)){
            ret += (sense.get -> new ListBuffer[List[Node]]())
          }
          ret(sense.get).append(binding)
        }
      } else {
        val childBindings = child.getAllSenseBoundVarBindings(key, meaningIdVarName)
        childBindings.foreach({case(sense, bindings) => {
          if(!ret.contains(sense)){
            ret += (sense -> new ListBuffer[List[Node]]())
          }
          ret(sense).appendAll(bindings)
        }})
      }
    }
    ret
  }

  /**
   * print for debug info
   */
  def dump(depth : Int = 0){
    if(true){
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
}


