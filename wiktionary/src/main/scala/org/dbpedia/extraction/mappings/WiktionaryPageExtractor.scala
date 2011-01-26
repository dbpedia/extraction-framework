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
  )


  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    //for DEBUG: wait a random time (this makes threading useless, but i can read the console output better)
    //val r = new scala.util.Random
    //Thread sleep r.nextInt(10)*1000

    //parseNodesWithTemplate(extractionTemplate.children.toBuffer, page.children.toBuffer)
    /*   val testTemplate : PageNode = new SimpleWikiParser().apply(new WikiPage(new WikiTitle("test template"),0,0,
  "{{extractiontpl|list-start|langlist}}" +
      "{{extractiontpl|var|lang}}[[afterlang]]" +
      "{{extractiontpl|list-start|poslist}}" +
        "{{extractiontpl|var|pos}}[[afterpos]]" +
        "{{extractiontpl|list-start|varlist}}" +
          "{{extractiontpl|var|myvar}}\n" +
        "{{extractiontpl|list-end}}\n" +
      "{{extractiontpl|list-end}}[[endpos]]" +
    "{{extractiontpl|list-end}}[[endlang]]"))
val testPage : PageNode = new SimpleWikiParser().apply(new WikiPage(new WikiTitle("test page"),0,0,
  "DEUTSCH[[afterlang]]" +
    //"ADVERB\n\n" +
    "SUBSTANTIV[[afterpos]]" +
      "test1\n" +
      "test2\n\n" +
    "VERB[[afterpos]]" +
      "#test3[[aftervar]]" +
      "#test4[[aftervar]][[aftervarlist]]" +
    "[[endpos]]" +
    "[[endlang]]"))*/
    //    val testTemplate : PageNode = new SimpleWikiParser().apply(new WikiPage(new WikiTitle("test template"),0,0,
    //      "[1]{{extractiontpl|list-start|myvar}}{{extractiontpl|var|myvar}}[end]{{extractiontpl|list-end|myvar}}[2]"))
    //    val testPage : PageNode = new SimpleWikiParser().apply(new WikiPage(new WikiTitle("test page"),0,0,
    //      "[1]abc[end]xyz[end][2]"))

    val pageTemplate : PageNode = new SimpleWikiParser().apply(
      new WikiPage(
        new WikiTitle("test template"),0,0, "\n"+Source.fromFile(language+"-page.tpl").mkString
      )
    )


    measure {
      try {
        val tpl = new Stack[Node]().pushAll(pageTemplate.children.reverse).filterNewLines
        println(tpl.map(dumpStr(_)).mkString )
        //val bindings = parseNodesWithTemplate(tpl, new Stack[Node]().pushAll(page.children.reverse))
        //bindings.dump(0)
      } catch {
        case e : WiktionaryException => println("page could not be parsed. results so far:"); e.vars.dump(0)
      }

      // the hierarchical var bindings need to be normalized to be converted to rdf
      //flatLangPos(bindings)
      //bindings.dump(0)

      // for debugging
      /*val pageStr = "\n== one {{tpl}} ==\n"
      val testpage : PageNode = new SimpleWikiParser()(
        new WikiPage(
          new WikiTitle("test"),0,0, pageStr
        )
      )
      val stack = new Stack[Node] pushAll testpage.children.reverse

      println(stack.map(dumpStr(_)).mkString )*/
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
          val curNode = pageIt.fullTrimmedPop
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
    val currNodeFromTemplate = tplIt.fullTrimmedPop
    val currNodeFromPage = pageIt.fullTrimmedHead
    val pageItCopy = pageIt.clone

    if(equals(currNodeFromTemplate, currNodeFromPage)){
      pageIt.pop //consume page node
      return bindings
    }

    currNodeFromTemplate match {
      case tn : TemplateNode => {
        if(tn.title.decoded == "Extractiontpl"){
          val tplType = tn.property("1").get.children(0).asInstanceOf[TextNode].text
          tplType match {
            case "list-start" =>  {
              val name =  tn.property("2").get.children(0).asInstanceOf[TextNode].text
              val listTpl = tplIt.getList
              val endMarkerNode = tplIt.findNextNonTplNode  //that will be the node that follows the list in the template
              bindings addChild parseList(listTpl, pageIt, endMarkerNode, name)
            }
            case "list-end" =>    println("end list - you should not see this")
            case "contains" =>    {
              printMsg("include stuff")
              val templates = ListBuffer[Stack[Node]]()
              for(property <- currNodeFromTemplate.children){  // children == properties
                val name = property.asInstanceOf[PropertyNode].children(0).asInstanceOf[TextNode].text.trim
                if(! Set("contains", "unordered", "optional").contains(name)){
                  val filename = language+"-"+name+".tpl"
                  if(!subTemplateCache.contains(name)) {
                    try{
                      val tplPage = new SimpleWikiParser().apply(new WikiPage(new WikiTitle("extraction subtemplate "+name),0,0,Source.fromFile(filename, "utf-8").getLines.mkString))
                      subTemplateCache += (name -> (new Stack[Node]().pushAll(tplPage.children.reverse)))
                    } catch {
                      case e : FileNotFoundException =>  printMsg("referenced non existant sub template "+filename+". skipped")
                    }
                  }
                  templates append subTemplateCache(name)
                }
              }
              //apply these templates as often as possible
              var oneHadSuccess = true
              while(oneHadSuccess) {
                oneHadSuccess = false
                for(tpl <- templates){
                  try{
                    printMsg("try tpl")
                    bindings mergeWith parseNodesWithTemplate(tpl, pageIt)
                    oneHadSuccess = true
                  } catch {
                    case e : WiktionaryException =>  // try another template
                  }
                }
              }
            }
            case "var" => {
              val endMarkerNode = if(tplIt.size > 0) Some(tplIt.fullTrimmedPop) else None
              val binding = recordVar(currNodeFromTemplate.asInstanceOf[TemplateNode], endMarkerNode, pageIt)
              bindings.addBinding(binding._1, binding._2)
            }
            case _ =>  { }
          }
        } else {
          //parse template properties
          if(!currNodeFromPage.isInstanceOf[TemplateNode]){
            throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
          }
          printMsg("do recursion on template properties")
          breakable{
            for(key <- tn.keySet){
              println("propkey"+ key)
              if(tn.property(key).isDefined && currNodeFromPage.asInstanceOf[TemplateNode].property(key).isDefined){
                bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll tn.property(key).get.children.reverse, new Stack[Node]() pushAll currNodeFromPage.asInstanceOf[TemplateNode].property(key).get.children.reverse)
              } else {
                break
              }
            }
          }
          pageIt.pop
        }
      }
      case tn : TextNode => {
        //variables can start recording in the middle of a textnode
        if(currNodeFromPage.isInstanceOf[TextNode] && currNodeFromPage.asInstanceOf[TextNode].text.startsWith(tn.text) && !(currNodeFromPage.asInstanceOf[TextNode].text.equals(tn.text))){
          if(tplIt.size > 0 && tplIt.fullTrimmedHead.isInstanceOf[TemplateNode] && tplIt.fullTrimmedHead.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text == "var"){
            //consume the current node from page
            val curNodeFromPage2 = pageIt.fullTrimmedPop
            val varNode = tplIt.fullTrimmedPop
            //split it
            val remainder = curNodeFromPage2.asInstanceOf[TextNode].text.substring(currNodeFromTemplate.asInstanceOf[TextNode].text.size, curNodeFromPage2.asInstanceOf[TextNode].text.size)
            //record var
            val endMarkerNode = if(tplIt.size > 0) Some(tplIt.fullTrimmedPop) else None

            pageIt.prependString(remainder)

            val binding = recordVar(varNode.asInstanceOf[TemplateNode], endMarkerNode, pageIt)
            bindings.addBinding(binding._1, binding._2)
          }
        } else if(currNodeFromPage.isInstanceOf[TextNode] && currNodeFromPage.asInstanceOf[TextNode].text.equals(tn.text)){
          printMsg("matched text.")
          //bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll currNodeFromTemplate.children.reverse, new Stack[Node]() pushAll pageIt.fullTrimAwarePop.children.reverse)
        } else {
          restore(pageIt, pageItCopy)
          throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
        }
      }
      case _ => {
        if(currNodeFromPage.getClass != currNodeFromTemplate.getClass){
          restore(pageIt, pageItCopy)
          throw new WiktionaryException("the template does not match the page", bindings, Some(currNodeFromPage))
        } else {
          printMsg("same class but not equal. do recursion on children")
          bindings addChild parseNodesWithTemplate(new Stack[Node]() pushAll currNodeFromTemplate.children.reverse, new Stack[Node]() pushAll pageIt.fullTrimmedPop.children.reverse)
        }
      }
    }
    bindings
  }

  protected def restore(st : Stack[Node], backup : Stack[Node]) : Unit = {
    st.clear
    st pushAll backup.reverse
  }

  protected def parseList(tplIt : Stack[Node], pageIt : Stack[Node], endMarkerNode : Option[Node], name : String) : WiktionaryPageExtractor.VarBindingsHierarchical = {
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
            printMsg(dumpStrShort(pageIt.fullTrimmedHead))
          } */
          if(endMarkerNode.isDefined &&
            (
              (pageIt.size > 0 && equals(pageIt.fullTrimmedHead, endMarkerNode.get)) ||
              (pageIt.fullTrimmedHead.isInstanceOf[TextNode] && endMarkerNode.get.isInstanceOf[TextNode] &&
                pageIt.fullTrimmedHead.asInstanceOf[TextNode].text.startsWith(endMarkerNode.get.asInstanceOf[TextNode].text)
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
    if(node.isInstanceOf[TextNode]){
      str += prefix+"text : "+node.retrieveText +"\n"
    } else
    if(node.isInstanceOf[SectionNode]){
      str += prefix+"secname : "+node.asInstanceOf[SectionNode].name +"\n"
      str += prefix+"level : "+node.asInstanceOf[SectionNode].level +"\n"
    }  else
    if(node.isInstanceOf[TemplateNode]){
      str += prefix+"tplname : "+node.asInstanceOf[TemplateNode].title.decoded +"\n"
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
      case tn : TemplateNode => "<tpl>{{"+tn.children.map(dumpStrShort(_)+"|").mkString+"}}</tpl> "
      case tn : TextNode => "<text>"+tn.text + "</text> "
      case ln : LinkNode => "<link>[["+ln.children(0).asInstanceOf[TextNode].text+"]]</link> "
      case sn : SectionNode=> "<section>"+sn.children.map(dumpStrShort(_)+"").mkString + "</section> "
      case pn : PropertyNode =>  dumpStrShort(pn.children(0))
      case n : Node=> "<other "+node.getClass+">"+node.retrieveText.get + "</other> "
    }
  }
}