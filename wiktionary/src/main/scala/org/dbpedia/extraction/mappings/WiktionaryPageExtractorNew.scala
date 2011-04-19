package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import impl.simple.SimpleWikiParser
import org.dbpedia.extraction.destinations.{Graph, Quad, Dataset, IriRef, PlainLiteral}
import org.dbpedia.extraction.sources.WikiPage
import scala.io.Source
import util.control.Breaks._
import java.io.FileNotFoundException
import java.lang.StringBuffer
import collection.mutable.{Stack, ListBuffer, HashMap, Set, Map, Seq}
import collection.SortedSet
import xml.{XML, Node => XMLNode}
import reflect.Class

//some of my utilities
import MyStack._
import MyNode._
import MyNodeList._
import TimeMeasurement._
import VarBinder._
import WiktionaryLogging._

/**
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractorNew(val language : String) extends Extractor {
  //private val language = extractionContext.language.wikiCode

  private val possibleLanguages = Set("en", "de")
  require(possibleLanguages.contains(language))

  private val config = XML.loadFile("config-"+language+".xml")

  private val templates = (config \ "templates" \ "sections" \ "template").map((n : XMLNode) =>  Tpl.fromNode(n))

  val ns = "http://wiktionary.org/" //TODO change all these uris to correct ones
  val usageProperty = ns + "hasUsage"
  val languageProperty = "http://purl.org/dc/elements/1.1/language"
  val posProperty = ns + "pos"
  val senseProperty = ns + "hasSense"
  //val varToProperty = (config \ "vars" \\ "var").map((_.attribute("name"), (_ \ "property").text))

  //these classes represent configuration from the xml
  class Var (val name : String, val property : String, val senseBound : Boolean, val resource : Boolean)
  object Var {
    def fromNode(n:XMLNode) = new Var((n \ "@name").text, (n \ "@property").text, n.attribute("senseBound").isDefined && (n \ "@senseBound").text.equals("true"), n.attribute("type").isDefined && (n \ "@type").text.equals("resource"))
  }
  class Tpl (val name : String, val tpl : Stack[Node], val vars : scala.collection.immutable.Seq[Var], var needsPostProcessing : Boolean, var ppClass : Option[String], var ppMethod : Option[String])
  object Tpl {
    def fromNode(n:XMLNode) = {
      val pp = n.attribute("needsPostProcessing").isDefined && (n \ "@needsPostProcessing").text.equals("true")
      val ppClass = if(pp){
        Some((n \ "@ppClass").text)
      } else {
        None
      }
      val ppMethod = if(pp){
        Some((n \ "@ppMethod").text)
      } else {
        None
      }
      new Tpl((n \ "@name").text, MyStack.fromString((n \ "wikiSyntax").text).filterNewLines, (n \ "vars" \ "var").map(Var.fromNode(_)), pp, ppClass, ppMethod)
    }
  }
  class Block (val indTpl : Tpl, val indBindings : ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]], var opened : Boolean, var nodes : Option[Stack[Node]], val bindings : ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]]){
    override def clone = new Block(indTpl, indBindings, opened, nodes, bindings)
  }
  object Block {
    def fromNode(n:XMLNode) = new Block(Tpl.fromNode((n \ "template").head), new ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]](), false, None, new ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]]())
  }
  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    val r = new scala.util.Random
    Thread sleep r.nextInt(10)*1000

    val quads = new ListBuffer[Quad]()
    val word = subjectUri.split("/").last
    measure {
      val bindings : VarBindingsHierarchical = new VarBindingsHierarchical
      val pageStack =  new Stack[Node]().pushAll(page.children.reverse)
      //handle beginning (see also)
      for(prolog <- config \ "templates" \ "prologs" \ "template"){
        try {
          val prologtpl = Tpl.fromNode(prolog)
          bindings addChild parseNodesWithTemplate(prologtpl.tpl, pageStack)
        } catch {
          case e : WiktionaryException =>
        }
      }

      //handle end (links to other languages)
      val rev = new Stack[Node] pushAll pageStack //reversed
      for(epilog <- config \ "templates" \ "epilogs" \ "template"){
        try {
          val epilogtpl = Tpl.fromNode(epilog)
          bindings addChild parseNodesWithTemplate(epilogtpl.tpl, rev)
        } catch {
          case e : WiktionaryException => bindings addChild e.vars
        }
      }
      //apply consumed nodes to pageStack
      pageStack.clear
      pageStack pushAll rev
      //TODO handle the bindings from pro- and epilog

      //split by blocks
      val curBlockNodes = new Stack[Node]
      var curBlock : Block = null
      var started = false
      println("chunking page")
      val configuredBlocks = (config \\ "block").map((n:XMLNode) => Block.fromNode(n))
      val pageBlocks = new ListBuffer[Block]()
      while(pageStack.size > 0){
        //try recognizing language block starts
        //print current node
        //println(pageStack.head.dumpStrShort)
        for(block <- configuredBlocks){
          try {
            //println("vs")
            //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
            val blockIndBindings =  parseNodesWithTemplate(block.indTpl.tpl.clone, pageStack)
            //success (no exception)
            println("success")
            blockIndBindings.dump(0)
            curBlock = block.clone
            curBlock.indBindings.append( (block.indTpl, blockIndBindings)  )

            if(started){
              //finish a previously opened block
              curBlock.nodes = Some(curBlockNodes.clone)
              pageBlocks append curBlock
              curBlockNodes clear
            } else {
              block.opened = true
            }
          } catch {
            case e : WiktionaryException => //did not match
          }
        }

        if(configuredBlocks.foldLeft(true)((rest, block) => rest && block.opened)){
          started = true
        }

        if(started){
          curBlockNodes push pageStack.pop // collect
        } else {
          pageStack.pop //ignore
        }
      }
      //close last block
      if(curBlock != null){
        curBlock.nodes = Some(curBlockNodes.clone)
        pageBlocks append curBlock
      }
      //println(blocks.keySet)

      //get bindings for each block
      pageBlocks.foreach((block : Block) => {
        val blockSt = new Stack[Node]() pushAll block.nodes.getOrElse(List()) //the nodes reversed, now we reverse them again
        val blockBindings = new VarBindingsHierarchical()
        while(blockSt.size > 0){
          var success = false
          breakable{
            for(template <- templates){
              //try matching the block to all templates
              if(template.tpl.size > 0){
                try{
                  //println(tpls(section))
                  printFuncDump("trying subtemplate "+template.name, template.tpl, blockSt)
                  block.bindings.append( (template, parseNodesWithTemplate(template.tpl.clone, blockSt)) )
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
            blockSt.pop
          }
        }
      })

      //generating triples
      val wiktionaryDataset : Dataset = new Dataset("wiktionary")
      val tripleContext = new IriRef(ns)
      pageBlocks.foreach((block : Block) => {

        //build a uri for the block
        val blockIdentifier = new StringBuffer()
        block.indBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          tpl.vars.foreach((varr : Var) => {
            println("blockindentifier "+tpl.name+" "+tplBindings.dump(0))
            blockIdentifier append tplBindings.getFirstBinding(varr.name).getOrElse(List()).myToString //concatenate all binding values (?)
          })
        }})
        val usageIri = new IriRef(ns + word+"-"+blockIdentifier.toString)

        //generate triples that indentify the block
        block.indBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          tpl.vars.foreach((varr : Var) => {
            quads += new Quad(wiktionaryDataset, usageIri, new IriRef(varr.property), new PlainLiteral(tplBindings.getFirstBinding(varr.name).getOrElse(List()).myToString), tripleContext)
          })
        }})
        //generate a triple to connect the block to the word
        quads += new Quad(wiktionaryDataset, new IriRef(ns+word), new IriRef(usageProperty), usageIri, tripleContext)


        //generate triples that describe the content of the block
        println("bindings")
        block.bindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          if(tpl.needsPostProcessing){
            //TODO fix this, stub
            val clazz = ClassLoader.getSystemClassLoader().loadClass(tpl.ppClass.get)
            val method = clazz.getDeclaredMethod(tpl.ppMethod.get, null);
            val ret = method.invoke(null, null)
            quads ++= ret.asInstanceOf[List[Quad]]
          } else {
            tpl.vars.foreach((varr : Var) => {
              if(varr.senseBound){
                val bindings = tplBindings.getSenseBoundVarBinding(varr.name)
                bindings.foreach({case (sense : List[Node], binding : List[Node]) =>
                  //TODO triples to connect usages to its senses
                  quads += new Quad(wiktionaryDataset, new IriRef(usageIri + "-"+sense.myToString), new IriRef(varr.property), new PlainLiteral(binding.myToString), tripleContext)
                })
              } else {
                val binding = tplBindings.getFirstBinding(varr.name)
                if(binding.isDefined){
                  quads += new Quad(wiktionaryDataset, usageIri, new IriRef(varr.property), new PlainLiteral(binding.get.myToString), tripleContext)
                }
              }
            })
          }
        }})

      })
    } report {
      duration : Long => println("took "+ duration +"ms")
    }
    println(""+quads.size+" quads extracted for "+word)
    quads.foreach((q : Quad) => println(q.renderNTriple))
    new Graph(quads.toList)
  }
}

