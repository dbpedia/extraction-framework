package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import org.dbpedia.extraction.destinations.{Graph, Quad, Dataset, IriRef, PlainLiteral}
import org.dbpedia.extraction.sources.WikiPage
import scala.io.Source
import util.control.Breaks._
import java.io.FileNotFoundException
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map}
import collection.SortedSet
import xml.{XML, Node => XMLNode}

//some of my utilities
import MyStack._
import MyNode._
import TimeMeasurement._
import MyStringTrimmer._
import WiktionaryPageExtractor2._ //shouldnt be the methods of the companion object available?
import WiktionaryLogging._

/**
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractor2 extends Extractor {
  //private val language = extractionContext.language.wikiCode

  private val possibleLanguages = Set("en", "de")
  require(possibleLanguages.contains(language))

  private val config = XML.loadFile("config-"+language+".xml")

  private val vars =  (config \ "vars" \ "uniqueVars" \ "var").map(_.attribute("name"))
  private val senseVars = (config \ "vars" \ "senseVars" \ "var").map(_.attribute("name"))

  private val sections = (config \ "templates" \ "sections" \ "section").map(_.attribute("name"))
  private val tpls = (config \ "templates" \ "sections" \ "section").map((n : XMLNode) => (n.attribute("name"), MyStack.fromString(n.text).filterNewLines)).toMap

  val ns = "http://wiktionary.org/" //TODO change all these uris to correct ones
  val usageProperty = ns + "hasUsage"
  val languageProperty = "http://purl.org/dc/elements/1.1/language"
  val posProperty = ns + "pos"
  val senseProperty = ns + "hasSense"
  val varToProperty = (config \ "vars" \\ "var").map((_.attribute("name"), (_ \ "property").text))

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    val r = new scala.util.Random
    Thread sleep r.nextInt(10)*1000
    val quads = new ListBuffer[Quad]()
    val word = subjectUri.split("/").last
    measure {
      var bindings : VarBindingsHierarchical = new VarBindingsHierarchical
      val pageStack =  new Stack[Node]().pushAll(page.children.reverse)
      //handle beginning (see also)
      try {
        val otherPages = MyStack.fromParsedFile(language+"-also.tpl").filterNewLines
        bindings addChild parseNodesWithTemplate(otherPages, pageStack)
      } catch {
        case e : WiktionaryException =>
      }

      //handle end (links to other languages)
      val rev = new Stack[Node] pushAll pageStack //reversed
      try {
        val otherLanguagesLinks = MyStack.fromParsedFile(language+"-otherLang.tpl").filterNewLines
        bindings addChild parseNodesWithTemplate(otherLanguagesLinks, rev)
      } catch {
        case e : WiktionaryException => bindings addChild e.vars
      }
      //apply consumed nodes to pageStack
      pageStack.clear
      pageStack pushAll rev

      //split by language-pos blocks
      val blocks : Map[Tuple2[List[Node], List[Node]], Stack[Node]] = new HashMap
      val langBlockStart = MyStack.fromParsedFile(language+"-lang-start.tpl").filterNewLines
      val posBlockStart = MyStack.fromParsedFile(language+"-pos-start.tpl").filterNewLines
      var curLang :Option[List[Node]] = None
      var curPos : Option[List[Node]] = None
      val curBlock = new Stack[Node]
      var started = false
      println("chunking page")
      while(pageStack.size > 0){
        //try recognizing language block starts
        //println()
        println(pageStack.head.toWikiText)
        try {
          //println("langBlock: "+langBlockStart.map(_.dumpStrShort).mkString)
          val langBlockBindings = parseNodesWithTemplate(langBlockStart.clone, pageStack)
          //success (no exception)
          val lang = langBlockBindings.getFirstBinding("language")
          if(lang.isDefined){
            //new lang block starts
            if(curPos.isDefined){
              if(started){
                 //but old block has not been ended yet -> end
                blocks += ((curLang.get, curPos.get) -> curBlock.clone )
                curBlock.clear
              }
              started = true
            }
            println("lang block started"+ lang.get)
            curLang = Some(lang.get)

          }
        } catch {
          case e : WiktionaryException => //did not match
        }
        //try recognizing pos block starts
        try {
          //println("posBlock: "+posBlockStart.map(_.dumpStrShort).mkString)
          val posBlockBindings = parseNodesWithTemplate(posBlockStart.clone, pageStack)
          //success (no exception)
          val pos = posBlockBindings.getFirstBinding("pos")
          if(pos.isDefined){
            //new lang block starts
            if(curLang.isDefined){
              if(started){
                 //but old block has not been ended yet -> end
                blocks += ((curLang.get, curPos.get) -> curBlock.clone )
                curBlock.clear
              }
              started = true
            }
            println("pos block started"+ pos.get)
            curPos = Some(pos.get)
          }
        } catch {
          case e : WiktionaryException => //did not match
        }

        if(started){
          curBlock push pageStack.pop // collect
        } else {
          pageStack.pop //ignore
        }
      }
      //println(blocks.keySet)

      val blockBindings : Map[Tuple2[List[Node], List[Node]], VarBindingsHierarchical] = new HashMap
      blocks.foreach({case((lang, pos),blockReverse) => {
        //println("lang="+lang+" pos="+pos)
        val block = new Stack[Node]() pushAll blockReverse
        val bindings = new VarBindingsHierarchical()
        while(block.size > 0){
          var success = false
          breakable{
            for(section <- sections){
              //println(section + " "+tpls(section).size)
              if(tpls(section).size > 0){
                try{
                  //println(tpls(section))
                  printFuncDump("trying section "+section, tpls(section), block)
                  bindings addChild parseNodesWithTemplate(tpls(section).clone, block)
                  success = true
                  break
                } catch {
                  case e : WiktionaryException => //ignore
                }
              }
            }
          }
          //println(success)
          if(!success){
            block.pop
          }
        }
        blockBindings += ((lang, pos) -> bindings)
      }})


      val wiktionaryDataset : Dataset = new Dataset("wiktionary")
      val tripleContext = new IriRef(ns)
      //generating triples
      blockBindings.foreach({case((lang, pos),bindings) => {
        println("lang="+lang+" pos="+pos)
        val posStr = nodesToString(pos)
        val langStr = nodesToString(lang)

        val usageIri = new IriRef(ns+word+"-"+langStr+"-"+posStr)
        quads += new Quad(wiktionaryDataset, new IriRef(subjectUri), new IriRef(usageProperty), usageIri, tripleContext)
        quads += new Quad(wiktionaryDataset, usageIri, new IriRef(languageProperty), new PlainLiteral(langStr), tripleContext)
        quads += new Quad(wiktionaryDataset, usageIri, new IriRef(posProperty), new PlainLiteral(posStr), tripleContext)

        println("bindings")
        //bindings.dump(0)
        val bindingsConverted = bindings.sortByVars
        //print
        println("normal properties:")
        bindingsConverted._1.foreach {case (varName, binding) => {
          println("\""+varName +"\" = "+binding)
          quads += new Quad(wiktionaryDataset, usageIri, new IriRef(varToProperty.apply(varName)), new PlainLiteral(nodesToString(binding)), tripleContext)
        }}
        println("has "+bindingsConverted._2.size +" meanings:")
        val meanings = bindingsConverted._2.keySet
        for(meaningLabel <- meanings){
          println("  sense (name="+meaningLabel+") has properties:")
          val meaningLabelStr = nodesToString(meaningLabel)
          val senseIri = new IriRef(usageIri.uri +"-"+ meaningLabelStr)
          quads += new Quad(wiktionaryDataset, usageIri, new IriRef(senseProperty), senseIri, tripleContext)
          bindingsConverted._2.apply(meaningLabel).foreach {case (varName, binding) => {
            val bindingStr =  nodesToString(binding)
            println("    \""+varName +"\" = "+bindingStr)
            quads += new Quad(wiktionaryDataset, senseIri, new IriRef(varToProperty.apply(varName)), new PlainLiteral(bindingStr), tripleContext)
          }}
        }
      }})
    } report {
      duration : Long => println("took "+ duration +"ms")
    }
    println(""+quads.size+" quads extracted for "+word)
    quads.foreach((q : Quad) => println(q.renderNTriple))
    new Graph(quads.toList)
  }
}
object WiktionaryPageExtractor2 {
  private val language = "de"
  private val subTemplateCache = scala.collection.mutable.Map[String, Stack[Node]]()

  protected def nodesToString(nodes : List[Node]) : String = nodes.map(_.retrieveText.getOrElse("")).mkString.trim

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
