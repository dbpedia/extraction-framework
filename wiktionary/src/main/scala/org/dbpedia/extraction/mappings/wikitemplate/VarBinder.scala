package org.dbpedia.extraction.mappings.wikitemplate

import org.dbpedia.extraction.wikiparser._
import util.control.Breaks._
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map, Queue, MutableList}
import xml.{XML, Node => XMLNode}

//some of my utilities
import MyStack._
import MyNode._
import Logging._
import MyLinkNode._
import MyNodeList._

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
  def parseNodesWithTemplate(tplIt : Stack[Node], pageIt : Stack[Node], varEndMarkers : List[Node] = Nil) : VarBindingsHierarchical = {
    printFuncDump("parseNodesWithTemplate", tplIt, pageIt, 4)
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
          //limit the queue as slinding window
          if(lastResults.size == windowSize){
            lastResults.dequeue()
          }
          //try to match node-by-node (here the recursion happens)
          bindings mergeWith parseNode(tplIt, pageIt, varEndMarkers)
          //add success value to the queue
          lastResults.enqueue(true)
        } catch {
          case e : WiktionaryException => {
            //the template does not match the page
            lastResults.enqueue(false)
            //check if we reached the failure rate threshold
            val failures = lastResults.count(!_)
            val correct = lastResults.size - failures
            printMsg("failures="+failures+" correct="+correct+" queue="+lastResults, 2)
            if(failures > maxFailures || correct < minCorrect){
              //too many errors
              printMsg("too many errors", 2)
              pageIt.clear
              pageIt.pushAll(pageItCopy.reverse)  // restore the page
              bindings mergeWith e.vars   // merge current bindings with previous and return them (by throwing a exception containing them)
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
  protected def parseNode(tplIt : Stack[Node], pageIt : Stack[Node], varEndMarkers : List[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseNode", tplIt, pageIt, 4)
    val bindings = new VarBindingsHierarchical
    val currNodeFromTemplate = tplIt.pop
    val currNodeFromPage = pageIt.head    // we dont consume the pagenode here - needs to be consumed after processing
    var pageItCopy = pageIt.clone

    //early detection of error or no action
    if(currNodeFromTemplate.equalsIgnoreLine(currNodeFromPage)){
      //println("simple match")
      //simple match
      pageIt.pop //consume page node
      return bindings
    } else //determine whether they CAN equal
    if(!currNodeFromTemplate.canMatchPageNode(currNodeFromPage)){
      //println("can not equal")
      //println("early mismatch: "+currNodeFromTemplate.dumpStrShort+" "+currNodeFromPage.dumpStrShort)
      throw new WiktionaryException("the template does not match the page - different type", bindings, Some(currNodeFromPage))
    }

    currNodeFromTemplate match {
      case tplNodeFromTpl : TemplateNode => {
        if(tplNodeFromTpl.title.decoded == "Extractiontpl"){
          val tplType = tplNodeFromTpl.property("1").get.children(0).asInstanceOf[TextNode].text
          tplType match {
            case "list-start" => {
              //val name =  tplNodeFromTpl.property("2").get.children(0).asInstanceOf[TextNode].text
              //take everything from tpl till list is closed
              val listTpl = tplIt.getList
              //take the node after the list as endmarker of this list
              val endMarkerNode = tplIt.findNextNonTplNode  //that will be the node that follows the list in the template
              val newVarEndMarkers = ListBuffer[Node]()
              newVarEndMarkers.appendAll(varEndMarkers)
              if(listTpl.findNextNonTplNode.isDefined){ //first node of tpl
                newVarEndMarkers.append(listTpl.findNextNonTplNode.get)
              }
              if(endMarkerNode.isDefined){
                newVarEndMarkers.append(endMarkerNode.get)
              }
              val listMode = tplNodeFromTpl.property("2").get.children(0).asInstanceOf[TextNode].text
              bindings addChild parseList(listTpl, pageIt, endMarkerNode, listMode, newVarEndMarkers.toList)
            }
            case "list-end" => printMsg("end list - you should not see this", 4)
            case "var" => {
              val endMarkerNode = tplIt.findNextNonTplNode
              val endMarkers = ListBuffer[Node]()
              if(endMarkerNode.isDefined){
                endMarkers.append(endMarkerNode.get)
              } else {
                //the var has an implicit end (e.g. "(a$x)*b" -> a or b may be endmarkers)
                endMarkers.appendAll(varEndMarkers)
              }
              val binding = recordVar(currNodeFromTemplate.asInstanceOf[TemplateNode], endMarkers.toList, pageIt)
              bindings.addBinding(binding._1, binding._2)
            }
            case "link" => {
              val expectedType = tplNodeFromTpl.property("2").get.children(0).asInstanceOf[TextNode].text
              currNodeFromPage match {
                case linkNodeFromPage : LinkNode => {
                  if(!expectedType.equals("any") && !linkNodeFromPage.getClass.getName.equals("org.dbpedia.extraction.wikiparser."+expectedType)){
                    //println("wrong link type. actual:"+linkNodeFromPage.getClass.getName+" expected: org.dbpedia.extraction.wikiparser."+expectedType+" ("+linkNodeFromPage.dumpStrShort+")")
                    throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
                  }
                  val destination = linkNodeFromPage.getDestination
                  val label = linkNodeFromPage.getLabel
                  //extract from the destination link
                  bindings mergeWith parseNodesWithTemplate(
                    tplNodeFromTpl.property("3").get.children.toStack,
                    new Stack[Node]() push new TextNode(label, 0)
                  )
                  if(tplNodeFromTpl.property("4").isDefined){
                    //extract from the label
                    bindings mergeWith parseNodesWithTemplate(
                    tplNodeFromTpl.property("4").get.children.toStack,
                      new Stack[Node]() push new TextNode(destination, 0)
                    )
                  }
                  pageIt.pop
                }
                case _ => throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
              }
            }
            case _ =>  { }
          }
        } else {
          //both are normal template nodes
          //parse template properties
          currNodeFromPage match {
            case tplNodeFromPage : TemplateNode => {
              //extract from template title
              bindings mergeWith parseNodesWithTemplate(
                  new Stack[Node]() pushAll tplNodeFromTpl.titleParsed.reverse,
                  new Stack[Node]() pushAll tplNodeFromPage.titleParsed.reverse
              ) 
              breakable {
                for(key <- tplNodeFromTpl.keySet){
                  if(tplNodeFromTpl.property(key).isDefined && tplNodeFromPage.property(key).isDefined){
                      bindings mergeWith parseNodesWithTemplate(
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
              printMsg("you should not see this: shouldve been detected earlier (node type does not match)", 4)
              throw new WiktionaryException("the template does not match the page: unmatched template property", bindings, Some(currNodeFromPage))
            }
          }
        }
      }
      case textNodeFromTpl : TextNode => {
        //variables can start recording in the middle of a textnode
        currNodeFromPage match {
           case textNodeFromPage : TextNode => {
              Logging.printMsg("two text nodes",4)
              if(textNodeFromPage.text.startsWith(textNodeFromTpl.text) && !textNodeFromPage.text.equals(textNodeFromTpl.text)){
                Logging.printMsg("consume shared prefix",4)
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
              printMsg("you should not see this: shouldve been detected earlier (node type does not match)", 4)
              throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
            }
        }
      }

      case _ => {
        if(currNodeFromPage.getClass != currNodeFromTemplate.getClass){
          restore(pageIt, pageItCopy) //still needed? dont think so
          printMsg("you should not see this: shouldve been detected earlier (node type does not match)", 4)
          throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
        } else {
          if(!(currNodeFromPage.isInstanceOf[SectionNode] && currNodeFromTemplate.isInstanceOf[SectionNode] &&
            currNodeFromPage.asInstanceOf[SectionNode].level != currNodeFromTemplate.asInstanceOf[SectionNode].level)){
            printMsg("same class but not equal. do recursion on children", 4)
            bindings mergeWith parseNodesWithTemplate(currNodeFromTemplate.children.toStack, pageIt.pop.children.toStack)
          } else {
            //sections with different level
            //TODO check canEqual
            printMsg("you should not see this: shouldve been detected earlier (section nodes with different level)", 4)
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
  protected def parseList(tplIt : Stack[Node], pageIt : Stack[Node], endMarkerNode : Option[Node], listMode : String, varEndMarkers : List[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseList ", tplIt, pageIt, 4)
    val bindings = new VarBindingsHierarchical
    val pageCopy = pageIt.clone
    var counter = 0
    try {
      breakable {
        while(pageIt.size > 0 ){
          /*if(endMarkerNode.isDefined &&
            (
              (pageIt.size > 0 && pageIt.head.equalsIgnoreLine(endMarkerNode.get)) ||   
              (pageIt.head.isInstanceOf[TextNode] && endMarkerNode.get.isInstanceOf[TextNode] &&
                pageIt.head.asInstanceOf[TextNode].text.startsWith(endMarkerNode.get.asInstanceOf[TextNode].text)
              )
            )
          ){    //TODO liststart = endmarker
            //printMsg("list ended by endmarker", 4)
            break
          }*/

          //try to match the list 
          //the parsing consumes the template so for multiple matches we need to duplicate it
          val copyOfTpl = tplIt.clone
          bindings addChild parseNodesWithTemplate(copyOfTpl, pageIt, varEndMarkers)
          counter += 1
        }
      }
    } catch {
      case e : WiktionaryException => printMsg("parseList caught an exception - list ended "+e, 4) // now we know the list was finished
      bindings addChild e.vars
    }
    printMsg("parseList matched "+counter+" times", 4)
    if((counter == 0 && listMode == "+")|| (counter > 1 && listMode == "?")){
        //println("list exception")
        restore(pageIt, pageCopy)
        throw new WiktionaryException("the list did not match", bindings, None)
    }
    bindings
  }

  /**
   * given a var-node and a page and an optional endmarker, bind the first n nodes to the var,
   * until the endmarker is seen or the page ends. bind means returning a tuple of varname and nodes
   */
  protected def recordVar(tplVarNode : TemplateNode, varEndMarkers : List[Node], pageIt : Stack[Node]) : (String, List[Node]) = {
    val varValue = new ListBuffer[Node]()
    printFuncDump("recordVar", new Stack[Node](), pageIt, 4)
    if(varEndMarkers.isEmpty){
      //when there is no end marker, we take everything we got
      varValue ++= pageIt
      pageIt.clear
      printMsg("no endmarker. taking all.", 4)
    } else {

      printMsg("endmarkers "+varEndMarkers.map(_.dumpStrShort), 4)

      //record from the page till we see the endmarker
      var usedEndMarker : Option[Node] = None
      var counter = 0
      breakable {
        while(pageIt.size > 0 ){
          val curNode = pageIt.pop
          //printMsg("curNode "+dumpStrShort(curNode), 4)

          varEndMarkers.foreach((endMarkerNode : Node) => {
          //check for occurence of endmarker (end of the var)
          if(endMarkerNode.equalsIgnoreLine(curNode)) {
            //printMsg("endmarker found (equal)", 4)
            usedEndMarker = Some(endMarkerNode)
            pageIt push curNode
            break
          } else if(curNode.isInstanceOf[TextNode] && endMarkerNode.isInstanceOf[TextNode]){
            //this should not happend
            if(curNode.asInstanceOf[TextNode].text.equals(endMarkerNode.asInstanceOf[TextNode].text)){
              //printMsg("endmarker found (string equal)", 4)
              usedEndMarker = Some(endMarkerNode)
              pageIt push curNode
              break
            }
          } 
          })
        
        val markerPositions = varEndMarkers.map((endMarkerNode : Node) => {
          if(curNode.isInstanceOf[TextNode] && endMarkerNode.isInstanceOf[TextNode]){
            val idx = curNode.asInstanceOf[TextNode].text.indexOf(endMarkerNode.asInstanceOf[TextNode].text)
            (idx -> endMarkerNode)
          }  else {
            (-1 -> endMarkerNode)
          }
        }).toMap
    
        val markerPositionsFiltered = markerPositions.filter(_._1 >= 0)
        if(markerPositionsFiltered.size > 0){
            //the curNode contains a endMarker
            val endMarkerNode = markerPositionsFiltered.minBy(_._1)._2
            //take the first occuring endmarker
            val idx = markerPositionsFiltered.minBy(_._1)._1
            usedEndMarker = Some(endMarkerNode)
            printMsg("endmarker found (substr)", 5)
            //everything until the endmarker is taken
            val part1 =  curNode.asInstanceOf[TextNode].text.substring(0, idx)  
            //part2 contains the endmarker followed by the remaining characters
            val part2 =  curNode.asInstanceOf[TextNode].text.substring(idx, curNode.asInstanceOf[TextNode].text.size)
              
            if(!part1.isEmpty){
              printMsg("var += "+part1, 4)
              varValue append new TextNode(part1, curNode.line)
            }
            //and put the rest back
            if(!part2.isEmpty){
              printMsg("putting back >"+part2+"<",4)
              pageIt.prependString(part2)
            } 
            break //stop recording
        }
        

          //count how many characters we recorded 
          counter += curNode.toWikiText.size
          if(counter > 1000){
            //limit
            throw new WiktionaryException("var too big", new VarBindingsHierarchical, None)
          }

          //recording
          varValue append curNode
          printMsg("var += "+curNode, 4)
        }
      }
      if(!usedEndMarker.isDefined){
        throw new WiktionaryException("endMarker of variable not found", new VarBindingsHierarchical, None)
      }
    }
    //return tuple consisting of var name and var value
    return (tplVarNode.property("2").get.children(0).asInstanceOf[TextNode].text, varValue.toList)
  }


  /**
   * silly helper function
   */
  def restore(st : Stack[Node], backup : Stack[Node]) : Unit = {
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
  val bindings = Map[String, List[Node]]()

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

  def getFlat(p : HashMap[String, List[Node]]) : VarBindings = {
    p ++= bindings //add my bindings
    val subPaths = new VarBindings
    //foreach child, open a new path
    children.map((c:VarBindingsHierarchical)=> {val cb = c.getFlat(p.clone); subPaths ++= cb})

    if(children.isEmpty){
      subPaths += p
    }
    subPaths
  }
  def getFlat() : VarBindings = getFlat(new HashMap[String, List[Node]]())
}

class VarBindings extends MutableList[HashMap[String, List[Node]]]


