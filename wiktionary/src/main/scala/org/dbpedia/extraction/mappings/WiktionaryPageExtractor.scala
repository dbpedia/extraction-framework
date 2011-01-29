package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.sources.WikiPage
import scala.io.Source
import util.control.Breaks._
import java.io.FileNotFoundException
import collection.mutable.{Stack, ListBuffer, HashMap}
import MyStack._
import TimeMeasurement._
import WiktionaryPageExtractor._

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
      /*val tpl = MyStack.fromParsedFile(language+"-page.tpl").filterNewLines.filterTrimmed
      val pageSt =  new Stack[Node]().pushAll(page.children.reverse).filterTrimmed
      //println(tpl.map(dumpStr(_)).mkString )
      try {
        val bindings = parseNodesWithTemplate(tpl, pageSt)
        bindings.dump(0)
      } catch {
        case e : WiktionaryException => println("page could not be parsed. results so far:"); e.vars.dump(0)
      }
       */
      // the hierarchical var bindings need to be normalized to be converted to rdf
      //flatLangPos(bindings)
      //bindings.dump(0)

      // for debugging
      val pageStr = " [[1]] [[{{tpl}}]] [1] [{{tpl}}] "
      val testpage : PageNode = new SimpleWikiParser()(
        new WikiPage(
          new WikiTitle("test"),0,0, pageStr
        )
      )
      testpage.children.foreach(node => println(dumpStr(node)))

    } report {
      duration : Long => println("took "+ duration +"ms")
    }

    println()
    println()

    //TODO build triples from bindings

    new Graph()
  }
}

class WiktionaryException(s: String, val vars : VarBindingsHierarchical, val unexpectedNode : Option[Node]) extends  Exception(s) {}
class VarException extends  WiktionaryException("no endmarker found", new VarBindingsHierarchical(), None) {}

object WiktionaryPageExtractor {
  private val language = "de"
  private val subTemplateCache = scala.collection.mutable.Map[String, Stack[Node]]()

  class VarBindingsHierarchical (){
    val children  = new ListBuffer[VarBindingsHierarchical]()
    val bindings = new HashMap[String, List[Node]]()
    def addBinding(name : String, value : List[Node]) : Unit = {
      bindings += (name -> value)
    }
    def addChild(sub : VarBindingsHierarchical) : Unit = {
      children += sub
    }
    def mergeWith(other : VarBindingsHierarchical) = {
      children ++= other.children
      bindings ++= other.bindings
    }
    def dump(depth : Int = 0){
      val prefix = " "*depth
      for(key <- bindings.keySet){
        println(prefix+key+" -> "+bindings.apply(key))
      }
      for(child <- children){
        child.dump(depth + 2)
      }
    }
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
  }

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
      printMsg("endmarker "+dumpStrShort(endMarkerNode))

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
          printMsg("var += "+curNode)

          varValue append curNode
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
    //the copy method is only available in the implementation of the abstract node-class
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
  protected def parseNode(tplIt : Stack[Node], pageIt : Stack[Node]) : WiktionaryPageExtractor.VarBindingsHierarchical = {
    printFuncDump("parseNode", tplIt, pageIt)
    val bindings = new WiktionaryPageExtractor.VarBindingsHierarchical
    val currNodeFromTemplate = tplIt.pop
    val currNodeFromPage = pageIt.head    // we dont consume the pagenode here - needs to be consumed after processing
    var pageItCopy = pageIt.clone

    //early detection of error or no action
    if(equals(currNodeFromTemplate, currNodeFromPage)){
      //simple match
      pageIt.pop //consume page node
      return bindings
    } else //determine whether they CAN equal
    if(!
      (
        (currNodeFromTemplate.isInstanceOf[TemplateNode]
          && currNodeFromTemplate.asInstanceOf[TemplateNode].title.decoded.equals( "Extractiontpl")
        )
        || currNodeFromPage.getClass == currNodeFromTemplate.getClass
      )
    ){
      throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
    }

    currNodeFromTemplate match {
      case tplNodeFromTpl : TemplateNode => {
        if(tplNodeFromTpl.title.decoded == "Extractiontpl"){
          val tplType = tplNodeFromTpl.property("1").get.children(0).asInstanceOf[TextNode].text
          tplType match {
            case "list-start" =>  {
              //val name =  tplNodeFromTpl.property("2").get.children(0).asInstanceOf[TextNode].text
              val listTpl = tplIt.getList
              val endMarkerNode = tplIt.findNextNonTplNode  //that will be the node that follows the list in the template
              bindings addChild parseList(listTpl, pageIt, endMarkerNode)
            }
            case "list-end" =>    println("end list - you should not see this")
            case "contains" =>    {
              printMsg("include stuff")
              val templates = ListBuffer[String]()
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
                    bindings mergeWith parseNodesWithTemplate(subTemplateCache(tpl).clone, pageIt)
                    oneHadSuccess = true
                  } catch {
                    case e : WiktionaryException =>  {
                      restore(pageIt, pageItCopy)
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
            case _ =>  { }
          }
        } else {
          //both are normal template nodes
          //parse template properties
          currNodeFromPage match {
            case tplNodeFromPage : TemplateNode => {
              breakable{
                for(key <- tplNodeFromTpl.keySet){
                  println("propkey"+ key)
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
              throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
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
      case linkNodeFromTpl : LinkNode => {
        linkNodeFromTpl match {
          case ilnFromTpl : InternalLinkNode => {
            val ilnFromPage = currNodeFromPage.asInstanceOf[InternalLinkNode]
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll ilnFromTpl.destinationNodes.reverse,
              new Stack[Node]() pushAll ilnFromPage.destinationNodes.reverse
            )
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll ilnFromTpl.children.reverse,
              new Stack[Node]() pushAll ilnFromPage.children.reverse
            )
          }
          case interWikiLinkFromTpl : InterWikiLinkNode => {
            val iwlnFromPage = currNodeFromPage.asInstanceOf[InterWikiLinkNode]
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll interWikiLinkFromTpl.destinationNodes.reverse,
              new Stack[Node]() pushAll iwlnFromPage.destinationNodes.reverse
            )
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll interWikiLinkFromTpl.children.reverse,
              new Stack[Node]() pushAll iwlnFromPage.children.reverse
            )
          }
          case externalLinkFromTpl : ExternalLinkNode => {
            val elnFromPage = currNodeFromPage.asInstanceOf[ExternalLinkNode]
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll externalLinkFromTpl.destinationNodes.reverse,
              new Stack[Node]() pushAll elnFromPage.destinationNodes.reverse
            )
            bindings addChild parseNodesWithTemplate(
              new Stack[Node]() pushAll externalLinkFromTpl.children.reverse,
              new Stack[Node]() pushAll elnFromPage.children.reverse
            )
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

  protected def parseList(tplIt : Stack[Node], pageIt : Stack[Node], endMarkerNode : Option[Node]) : WiktionaryPageExtractor.VarBindingsHierarchical = {
    //printFuncDump("parseList "+name, tplIt, pageIt)
    val bindings = new WiktionaryPageExtractor.VarBindingsHierarchical

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
    }
    bindings
  }

  // parses the complete buffer
  protected def parseNodesWithTemplate(tplIt : Stack[Node], pageIt : Stack[Node]) : WiktionaryPageExtractor.VarBindingsHierarchical = {
    //printFuncDump("parseNodesWithTemplate", tplIt, pageIt)
    val bindings = new WiktionaryPageExtractor.VarBindingsHierarchical
    while(tplIt.size > 0 && pageIt.size > 0){
      bindings mergeWith parseNode(tplIt, pageIt)
    }
    bindings
  }

  protected def printFuncDump(name : String, tplIt : Stack[Node], pageIt : Stack[Node]) : Unit = {
    val st_depth = new Exception("").getStackTrace.length  - 7 //6 is the stack depth on the extract call. +1 for this func
    val prefix =  " " * st_depth
    println(prefix + "------------")
    println(prefix + "<entering " + name +">")
    println(prefix + "<template (next 5)>")
    println(prefix + tplIt.take(5).map(dumpStrShort(_)).mkString)
    println(prefix + "<page (next 5)>")
    println(prefix + pageIt.take(5).map(dumpStrShort(_)).mkString)
    println(prefix + "------------\n\n")
  }

  protected def printMsg(str : String) : Unit = {
    val st_depth = new Exception("").getStackTrace.length  - 7
    val prefix =  " " * st_depth
    println(prefix + str)
  }

  protected def flatLangPos(vb : WiktionaryPageExtractor.VarBindingsHierarchical) : Unit = {
    def getLang(vbi : WiktionaryPageExtractor.VarBindingsHierarchical) : Option[WiktionaryPageExtractor.VarBindingsHierarchical] = {
      if(vbi.bindings.contains("lang")){
        return Some(vbi)
      } else {
        for(child <- vbi.children){
          val possibleLang = getLang(child)
          if(possibleLang.isDefined){
            return possibleLang
          }
        }
        return None
      }
    }
    def getPos(vbi : WiktionaryPageExtractor.VarBindingsHierarchical) : Option[WiktionaryPageExtractor.VarBindingsHierarchical] = {
      if(vbi.bindings.contains("pos")){
        return Some(vbi)
      } else {
        for(child <- vbi.children){
          val possiblePos = getPos(child)
          if(possiblePos.isDefined){
            return possiblePos
          }
        }
        return None
      }
    }
    val lang = getLang(vb)
    if(lang.isDefined){
      val pos = getPos(lang.get)
      if(pos.isDefined){
        val children = pos.get.children.clone
        pos.get.children.clear
        for(child <- children){
          pos.get.children prepend child.flat
        }
      } else println("no pos")
    } else println("no lang")
  }

  protected def dump(node : Node, depth : Int = 0) : Unit =
  {
    println(dumpStr(node, depth))
  }

   protected def dumpStr(node : Node, depth : Int = 0) : String =
  {
    var str = ""
    //indent by depth
    val prefix = " "*depth

    //print class
    str += prefix + node.getClass +"\n"

    //dump node properties according to its type
    node match {
      case tn : TextNode =>  str += prefix+"text : "+tn.text +"\n"
      case tn : SectionNode => {
        str += prefix+"secname : "+tn.name +"\n"
        str += prefix+"level : "+tn.level +"\n"
      }
      case tn : TemplateNode =>  str += prefix+"tplname : "+tn.title.decoded +"\n"
      case tn : ExternalLinkNode =>  str += prefix+"destNodes : "+tn.destinationNodes +"\n"
      case tn : InternalLinkNode =>  str += prefix+"destNodes : "+tn.destinationNodes +"\n"
    }

    //dump children
    str += prefix+"children:  {" +"\n"
    node.children.foreach(
      child => str += dumpStr(child, depth + 2)
    )
    str += prefix+"}" +"\n"
    str
  }

  protected  def dumpStrShort(node : Node) : String =
  {
    node match {
      case tn : TemplateNode => "<tpl>"+tn.toWikiText+"</tpl> "
      case tn : TextNode => "<text>"+tn.toWikiText+"</text> "
      case ln : LinkNode => "<link>"+ln.toWikiText+"</link> "
      case sn : SectionNode=> "<section>"+sn.toWikiText+"</section> "
      case n : Node=> "<other "+node.getClass+">"+node.retrieveText.get + "</other> "
    }
  }
}