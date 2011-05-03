package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{Graph, Quad, Dataset, IriRef, PlainLiteral}
import util.control.Breaks._
import java.io.FileNotFoundException
import java.lang.StringBuffer
import collection.mutable.{Stack, ListBuffer, Set}
import xml.{XML, Node => XMLNode}

//some of my utilities
import MyStack._
import MyNodeList._
import TimeMeasurement._
import VarBinder._
import WiktionaryLogging._

/**
 * parses (wiktionary) wiki pages
 * is meant to be configurable for multiple languages
 *
 * is even meant to be usable for non-wiktionary pages -> arbitrary wiki pages, that follow a "overall page"-schema
 * but in contrast to infobox-focused extraction, we *aim* to be more flexible:
 * dbpedia core is hardcoded extraction. here we try to use a meta-language describing the information to be extracted
 * this is done via wikisyntax snippets (called templates) containing placeholders (called variables), which are then bound
 *
 * we also extended this approach to match the wiktionary schema
 * a page can contain information about multiple entities (sequential blocks), each having multiple contexts/senses
 * other use cases (non-wiktionary), can be seen as a special case, having only one block (entity) and one sense
 *
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractorNew(val language : String) extends Extractor {
  private val possibleLanguages = Set("en", "de")
  require(possibleLanguages.contains(language))

  //load config from xml
  private val config = XML.loadFile("config-"+language+".xml")

  private val templates = (config \ "templates" \ "sections" \ "template").map((n : XMLNode) =>  Tpl.fromNode(n))

  private val mappings = (config \ "mappings" \ "mapping").map(
      (n : XMLNode) =>
        ( (n \ "@from").text,
          if(n.attribute("toType").isDefined && (n \ "@toType").text.equals("uri")){new IriRef((n \ "@to").text)} else {new PlainLiteral((n \ "@to").text)}
        )
      ).toMap

  val ns =            (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("ns") }).getOrElse(<propery uri="http://undefined.com/"/>)) \ "@uri").text
  val blockProperty = (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("blockProperty") }).getOrElse(<propery uri="http://undefined.com/"/>)) \ "@uri").text
  val senseProperty = (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("senseProperty") }).getOrElse(<propery uri="http://undefined.com/"/>)) \ "@uri").text

  //these classes represent configuration from the xml
  class Var (val name : String, val property : String, val senseBound : Boolean, val bindsToUri : Boolean, val doMapping : Boolean)
  object Var {
    def fromNode(n:XMLNode) = new Var(
        (n \ "@name").text,
        (n \ "@property").text,
        n.attribute("senseBound").isDefined && (n \ "@senseBound").text.equals("true"),
        n.attribute("type").isDefined && (n \ "@type").text.equals("resource"),
        n.attribute("doMapping").isDefined && (n \ "@doMapping").text.equals("true")
    )
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
    // wait a random number of seconds. kills parallelism - otherwise debug output from different threads is mixed
    //TODO remove if in production
    val r = new scala.util.Random
    Thread sleep r.nextInt(10)*1000

    val quads = new ListBuffer[Quad]()
    val word = subjectUri.split("/").last
    measure {
      val proAndEpilogBindings : ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]] = new ListBuffer
      val pageStack =  new Stack[Node]().pushAll(page.children.reverse)

      //handle prolog (beginning) (e.g. "see also") - not related to blocks, but to the main entity of the page
      for(prolog <- config \ "templates" \ "prologs" \ "template"){
        val prologtpl = Tpl.fromNode(prolog)
         try {
          proAndEpilogBindings.append( (prologtpl, parseNodesWithTemplate(prologtpl.tpl, pageStack)) )
        } catch {
          case e : WiktionaryException => proAndEpilogBindings.append( (prologtpl, e.vars) )
        }
      }

      //handle epilog (ending) (e.g. "links to other languages") by parsing the page backwards
      val rev = new Stack[Node] pushAll pageStack //reversed
      for(epilog <- config \ "templates" \ "epilogs" \ "template"){
        val epilogtpl = Tpl.fromNode(epilog)
        try {
          proAndEpilogBindings.append( (epilogtpl, parseNodesWithTemplate(epilogtpl.tpl, rev)) )
        } catch {
          case e : WiktionaryException => proAndEpilogBindings.append( (epilogtpl, e.vars) )
        }
      }
      //apply consumed nodes (from the reversed page) to pageStack  (unreversed)
      pageStack.clear
      pageStack pushAll rev

      //TODO handle the bindings from pro- and epilog

      //split by blocks (in wiktionary a block is a usage - "English, Verb")
      val curBlockNodes = new Stack[Node]
      var curBlock : Block = null
      var started = false
      //println("chunking page")
      val configuredBlocks = (config \\ "block").map((n:XMLNode) => Block.fromNode(n))
      val pageBlocks = new ListBuffer[Block]()
      while(pageStack.size > 0){
        // try recognizing block starts: each block has a "indicator-template" (indTpl) -
        // when it matches, the block starts. and from that template we get bindings that describe the block

        //debug: print the page (the next 2 nodes only)
        //println(pageStack.take(2).map(_.dumpStrShort).mkString)
        for(block <- configuredBlocks){
          try {
            //println("vs")
            //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
            val blockIndBindings =  parseNodesWithTemplate(block.indTpl.tpl.clone, pageStack)
            //no exception -> success -> stuff below here will be executed on success
            println("success")
            curBlock = block.clone
            curBlock.indBindings.append( (block.indTpl, blockIndBindings)  )
            curBlock.indBindings.foreach({case(tpl,bindings)=>bindings.dump()})
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

        //if ALL nested blocks have started (so were in the innermost block), start recording it as ONE block
        //TODO this is not sufficient. vars need to be bound in outer blocks. hint: in xml: move all the templates into block definitions, react there
        if(configuredBlocks.foldLeft(true)((rest, block) => rest && block.opened)){
          started = true
        }

        if(started){
          curBlockNodes push pageStack.pop // collect
        } else {
          pageStack.pop //ignore
        }
      }
      //close last block implicitly on page end
      if(curBlock != null){
        curBlock.nodes = Some(curBlockNodes.clone)
        pageBlocks append curBlock
      }

      //get bindings for each block
      pageBlocks.foreach((block : Block) => {
        val blockSt = new Stack[Node]() pushAll block.nodes.getOrElse(List()) //the nodes were reversed while collecting, now we reverse them again
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
                  block.bindings.append( (template, parseNodesWithTemplate(template.tpl.clone, blockSt)) )  //consumes multiple nodes
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
            blockSt.pop //we just skip nodes that are not the start of a template
          }
        }
      })

      //generate triples from blocks
      val wiktionaryDataset : Dataset = new Dataset("wiktionary")
      val tripleContext = new IriRef(ns)
      pageBlocks.foreach((block : Block) => {

        //build a uri for the block
        val blockIdentifier = new StringBuffer()
        block.indBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          tpl.vars.foreach((varr : Var) => {
            //concatenate all binding values of the block indicator tpl (?)
            blockIdentifier append tplBindings.getFirstBinding(varr.name).getOrElse(List()).myToString
          })
        }})
        val blockIri = new IriRef(ns + word+"-"+blockIdentifier.toString)

        //generate triples that indentify the block
        block.indBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          tpl.vars.foreach((varr : Var) => {
            val objStr = tplBindings.getFirstBinding(varr.name).getOrElse(List()).myToString
            val obj = if(varr.doMapping){mappings.getOrElse(objStr,new PlainLiteral(objStr))} else {new PlainLiteral(objStr)}
            quads += new Quad(wiktionaryDataset, blockIri, new IriRef(varr.property), obj, tripleContext)
          })
        }})
        //generate a triple to connect the block to the word
        quads += new Quad(wiktionaryDataset, new IriRef(ns+word), new IriRef(blockProperty), blockIri, tripleContext)

        //generate triples that describe the content of the block
        block.bindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
          //println(tpl.name +": "+ tplBindings.dump())
          if(tpl.needsPostProcessing){
            //TODO does not work yet, implement the invocation of a static method that does a transformation of the bindings
            val clazz = ClassLoader.getSystemClassLoader().loadClass(tpl.ppClass.get)
            val method = clazz.getDeclaredMethod(tpl.ppMethod.get, null);
            val ret = method.invoke(tplBindings, null)
            quads ++= ret.asInstanceOf[List[Quad]]
          } else {
            //generate a triple for each var binding
            tpl.vars.foreach((varr : Var) => {
              if(varr.senseBound){
                //handle sense bound vars (e.g. meaning)
                //TODO use getAllSenseBoundVarBindings function
                val bindings = tplBindings.getSenseBoundVarBinding(varr.name)
                bindings.foreach({case (sense : List[Node], binding : List[Node]) =>
                  //the sense identifier is mostly something like "[1]" - sense is then List(TextNode("1"))
                  val objStr = binding.myToString
                  val obj = if(varr.doMapping){mappings.getOrElse(objStr,new PlainLiteral(objStr))} else {new PlainLiteral(objStr)}
                  quads += new Quad(wiktionaryDataset, new IriRef(blockIri.uri + "-"+sense.myToString), new IriRef(varr.property), obj, tripleContext)
                  //TODO triples to connect blocks to its senses (maybe collect all senses here, make distinct, then build triples after normal bindingtriples)
                })
              } else {
                //handle non-sense bound vars - they are related to the whole block/usage (e.g. hyphenation)
                val bindings = tplBindings.getAllBindings(varr.name)
                for(binding <- bindings){
                  val objStr = binding.myToString
                  val obj = if(varr.doMapping){mappings.getOrElse(objStr,new PlainLiteral(objStr))} else {new PlainLiteral(objStr)}
                  quads += new Quad(wiktionaryDataset, blockIri, new IriRef(varr.property), obj, tripleContext)
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

