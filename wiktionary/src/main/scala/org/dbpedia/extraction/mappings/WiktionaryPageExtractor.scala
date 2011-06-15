package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.sources.WikiPage
import scala.io.Source
import util.control.Breaks._
import java.io.FileNotFoundException
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map}

//some of my utilities
import MyStack._
import MyNode._
import TimeMeasurement._
import MyStringTrimmer._
import WiktionaryPageExtractor._ //shouldnt be the methods of the companion object available?
import WiktionaryLogging._

/**
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractor extends Extractor {
  //private val language = extractionContext.language.wikiCode

  private val possibleLanguages = Set("en", "de")
  require(possibleLanguages.contains(language))

  //maybe we need this later, needs to be put in the tranformator class
  private val translatePOS = Map(
    "noun" -> Map(
      "de" -> "Substantiv",
      "en" -> "noun",
      "uri"-> "http://purl.org/linguistics/gold/Noun"
    ),
    "verb" -> Map(
      "de" -> "Verb",
      "en" -> "Verb",
      "uri"-> "http://purl.org/linguistics/gold/Verbal"    //TODO check
    ),
    "adverb" -> Map(
      "de" -> "Adverb",
      "en" -> "Adverb",
      "uri"-> "http://purl.org/linguistics/gold/Adverbial"
    ),
    "adjective" -> Map(
      "de" -> "Adjektiv",
      "en" -> "Adjective",
      "uri"-> "http://purl.org/linguistics/gold/Adjectival"
    ),
    "pronoun" -> Map(
      "de" -> "Pronom",
      "en" -> "Pronoun",
      "uri"-> "http://purl.org/linguistics/gold/Pronominal"
    ),
    "cardinalNumber" -> Map(
      "de" -> "Kardinalzahl",
      "en" -> "Cardinal number",
      "uri"-> "http://purl.org/linguistics/gold/CardinalNumeral"
    )   //TODO complete
  )

  private val vars = Set("hyphenation-singular", "hyphenation-plural", "pronunciation-singular", "pronunciation-plural",
    "audio-singular", "audio-plural")
  private val senseVars = Set("meaning")

  //private val translatePOSback = Map() ++ (translatePOS map {case (lang,kv) => (lang, kv map {case (k, v) => (v, k)})})
  //TODO do this on a single line :)
  private var translatePOSback : scala.collection.mutable.Map[String, scala.collection.mutable.Map[String,String]] =  scala.collection.mutable.Map()
  for(lang <- possibleLanguages){
    translatePOSback += lang -> scala.collection.mutable.Map()
  }
  translatePOSback += "uri" -> scala.collection.mutable.Map()
  for(key <- translatePOS.keySet){
    for(lang <- possibleLanguages){
      translatePOSback.apply(lang) += translatePOS.apply(key).apply(lang) ->  key
      translatePOSback.apply("uri") += translatePOS.apply(key).apply("uri") ->  key
    }
  }

  private val varPropMapping = Map (
    "pos" -> "http://example.com/pos",
    "lang" -> "http://example.com/lang",
    "word" -> "http://example.com/word",
    "example" -> "http://example.com/example"
  ) //TODO complete


  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    //for DEBUG: wait a random time (this makes threading useless, but i can read the console output better)
    //val r = new scala.util.Random
    //Thread sleep r.nextInt(10)*1000


    measure {
      val tpl = MyStack.fromParsedFile(language+"-page.tpl").filterNewLines.filterTrimmed
      val pageSt =  new Stack[Node]().pushAll(page.children.reverse).filterTrimmed
      //println("dumping page")
      //page.children.foreach(_.dump(0))
      var bindings : VarBindingsHierarchical = null
      try {
        bindings = parseNodesWithTemplate(tpl, pageSt)
      } catch {
        case e : WiktionaryException => {
          bindings = e.vars
          println("page could not be parsed. reason:")
          println(e.s)
          if(e.unexpectedNode.isDefined){
            println("unexpected node:")
            println(e.unexpectedNode.get)
          }
        }
      }
      println("results")
      println("before")
      bindings.dump(0)
      // the hierarchical var bindings need to be normalized to be converted to rdf
      val converted = bindings.flatLangPos
      val converted2 : Map[Tuple2[String,String], Tuple2[Map[String, List[Node]], Map[List[Node],Map[String, List[Node]]]]] = new HashMap()
      for (wlp <- converted){
        val normalBindings : Map[String, List[Node]]= new HashMap()
        //get normal var bindings out (these that are unique to a (word,lang,pos) combination)
        for(varName <- vars){
          val currBindings = wlp._2.getFirstBinding(varName)
          if(currBindings.isDefined){
            normalBindings += (varName -> currBindings.get)
          }
        }
        val senseBindingsConverted : Map[List[Node],Map[String, List[Node]]] = new HashMap()
        for(varName <- senseVars){
          val senseBindings = wlp._2.getSenseBoundVarBinding(varName)
          val senses = senseBindings.keySet
          for(sense <- senses){
             if(!senseBindingsConverted.contains(sense)){
                senseBindingsConverted += (sense -> new HashMap())
             }
             senseBindingsConverted(sense) += (varName -> senseBindings(sense))
          }
        }
        converted2 += (wlp._1 -> (normalBindings, senseBindingsConverted))
      }
      println("after")
      //println(converted2)
      println(subjectUri)
      val usages = converted2.keySet
      for(usage <- usages){
        println("usage in language: "+usage._1+" as part of speech: "+usage._2)
        println("normal properties:")
        converted2(usage)._1.foreach {case (varName, binding) => println("\""+varName +"\" = "+binding)}
        println("has "+converted2(usage)._2.size +" meanings:")
        val meanings = converted2(usage)._2.keySet
        for(meaning <- meanings){
          println("  sense (name="+meaning+") has properties:")
          converted2(usage)._2.apply(meaning).foreach {case (varName, binding) => println("    \""+varName +"\" = "+binding)}
        }
      }

      // for debugging
      /*println()
      println("parse an example page... result:")
      //val pageStr = "[[{{tpl|var|uri}}|uriLabel]]"  //ok das hier ist wahrscheinlich ein größeres problem
      val pageStr = "[{{tpl|var|uri}} uriLabel]"
      val testpage : PageNode = new SimpleWikiParser()(
        new WikiPage(
          new WikiTitle("test"),0,0, pageStr
        )
      )
      testpage.children.foreach(node => println(dumpStr(node)))
      assert(testpage.children(0).asInstanceOf[InternalLinkNode].destinationNodes(0).isInstanceOf[TemplateNode])
      */
    } report {
      duration : Long => println("took "+ duration +"ms")
    }

    //TODO build triples from bindings

    new Graph()
  }
}

object WiktionaryPageExtractor {
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
          if(equals(endMarkerNode, curNode)) {
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
                 printMsg("var += "+curNode)
                varValue append new TextNode(part1, curNode.line)
              }
              //and put the rest back
              if(!part2.isEmpty){
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

  //why are line numbers stored - this messes up the equals method,
  //we fix this using the scala 2.8 named constructor arguments aware copy method of case classes
  protected def equals(n1 : Node, n2 : Node) : Boolean = {
    if(n1.getClass != n2.getClass){
      return false
    }
    //the copy method is not available in the implementation of the abstract node-class
    //so we cast to all subclasses
    n1 match {
      case tn : TemplateNode => n2.asInstanceOf[TemplateNode].copy(line=0).equals(tn.copy(line=0))
      case tn : TextNode => n2.asInstanceOf[TextNode].copy(line=0).equals(tn.copy(line=0))
      case tn : SectionNode => n2.asInstanceOf[SectionNode].copy(line=0).equals(tn.copy(line=0))
      case tn : ExternalLinkNode => n2.asInstanceOf[ExternalLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : InternalLinkNode => n2.asInstanceOf[InternalLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : InterWikiLinkNode => n2.asInstanceOf[InterWikiLinkNode].copy(line=0).equals(tn.copy(line=0))
      case tn : PropertyNode => n2.asInstanceOf[PropertyNode].copy(line=0).equals(tn.copy(line=0))
      case tn : TableNode => n2.asInstanceOf[TableNode].copy(line=0).equals(tn.copy(line=0))
    }
  }

  //extract does one step e.g. parse a var, or a list etc and then returns
  protected def parseNode(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
    printFuncDump("parseNode", tplIt, pageIt)
    val bindings = new VarBindingsHierarchical
    val currNodeFromTemplate = tplIt.pop
    val currNodeFromPage = pageIt.head    // we dont consume the pagenode here - needs to be consumed after processing
    var pageItCopy = pageIt.clone

    //early detection of error or no action
    if(equals(currNodeFromTemplate, currNodeFromPage)){
      //simple match
      pageIt.pop //consume page node
      return bindings
    } else //determine whether they CAN equal
    if(!  //NOT
      (
        (currNodeFromTemplate.isInstanceOf[TemplateNode]
          && currNodeFromTemplate.asInstanceOf[TemplateNode].title.decoded.equals( "Extractiontpl")
        )
        || currNodeFromPage.getClass == currNodeFromTemplate.getClass
      )
    ){
      println("early mismatch")
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
            case "contains" =>    {
              printMsg("include stuff")
              val templates = ListBuffer[String]()
              val markers  = Set[Node]()
              for(property <- currNodeFromTemplate.children){  // children == properties
                val name = property.asInstanceOf[PropertyNode].children(0).asInstanceOf[TextNode].text.trim
                if(! Set("contains", "unordered", "optional").contains(name)){
                  val filename = language+"-"+name+".tpl"
                  if(!subTemplateCache.contains(name)){
                    try{
                      val tplSt = MyStack.fromParsedFile(language+"-"+name+".tpl")
                      tplSt.filterNewLines.filterTrimmed
                      if(tplSt.size != 0){
                        subTemplateCache += (name -> tplSt)
                        markers.add(tplSt.head)
                      }
                    } catch {
                      case e : FileNotFoundException =>  printMsg("referenced non existant sub template "+filename+". skipped")
                    }
                  }
                  templates append name
                }
              }
              //apply these templates as often as possible
              var oneHadSuccess = true
              while(oneHadSuccess) {
                oneHadSuccess = false
                for(tpl <- templates){
                  pageItCopy = pageIt.clone
                  try{
                    bindings addChild parseNodesWithTemplate(subTemplateCache(tpl).clone, pageIt)
                    oneHadSuccess = true
                  } catch {
                    case e : WiktionaryException =>  {
                      restore(pageIt, pageItCopy)
                      bindings addChild e.vars
                    }// try another template
                  }
                }
              }
            }
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
          printMsg("same class but not equal. do recursion on children")
          bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll currNodeFromTemplate.children.reverse, new Stack[Node]() pushAll pageIt.pop.children.reverse)
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
          //printFuncDump("parseList "+name+" item", tplIt, pageIt)

          /*if(endMarkerNode.isDefined && pageIt.size > 0){
            printMsg("check if continue list. endmarker:")
            printMsg(dumpStrShort(endMarkerNode.get))
            printMsg("head")
            printMsg(dumpStrShort(pageIt.head))
          } */
          if(endMarkerNode.isDefined &&
            (
              (pageIt.size > 0 && equals(pageIt.head, endMarkerNode.get)) ||
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
  protected def parseNodesWithTemplate(tplIt : Stack[Node], pageIt : Stack[Node]) : VarBindingsHierarchical = {
    //printFuncDump("parseNodesWithTemplate", tplIt, pageIt)
    val bindings = new VarBindingsHierarchical
    while(tplIt.size > 0 && pageIt.size > 0){
        try {
          bindings addChild parseNode(tplIt, pageIt)
        } catch {
          case e : WiktionaryException => {
            //try dropping n nodes from the page
            if(false){

            } else {
              bindings addChild e.vars
              throw e.copy(vars=bindings)
            }
          }
        }
    }
    bindings.dump(0)
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
 }
