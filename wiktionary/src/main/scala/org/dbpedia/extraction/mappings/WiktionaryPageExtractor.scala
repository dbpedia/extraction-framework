package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{Graph, Quad, Dataset, IriRef, PlainLiteral, GraphNode}
import util.control.Breaks._
import java.io.FileNotFoundException
import java.lang.StringBuffer
import xml.{XML, Node => XMLNode}
import collection.mutable.{HashMap, Stack, ListBuffer, Set}

//some of my utilities
import MyNodeList._
import MyNode._
import TimeMeasurement._
import VarBinder._
import WiktionaryLogging._

/**
 * parses (wiktionary) wiki pages
 * is meant to be configurable for multiple languages
 *
 * is even meant to be usable for non-wiktionary wikis -> arbitrary wikis, but where all pages follow a common schema
 * but in contrast to infobox-focused extraction, we *aim* to be more flexible:
 * dbpedia core is hardcoded extraction. here we try to use a meta-language describing the information to be extracted
 * this is done via xml containing wikisyntax snippets (called templates) containing placeholders (called variables), which are then bound
 *
 * we also extended this approach to match the wiktionary schema
 * a page can contain information about multiple entities (sequential blocks), each having multiple contexts/emittedBlockSenseConnections
 * other use cases (non-wiktionary), can be seen as a special case, having only one block (entity) and one sense
 *
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractor(val language : String, val debugging : Boolean) extends Extractor {
  private val possibleLanguages = Set("en", "de")
  require(possibleLanguages.contains(language))
  WiktionaryLogging.enabled = debugging
  //load config from xml
  private val config = XML.loadFile("config-"+language+".xml")

  private val templates = (config \ "templates" \ "sections" \ "template").map((n : XMLNode) =>  Tpl.fromNode(n))

  private val mappings : Map[String, String] = (config \ "mappings" \ "mapping").map(
      (n : XMLNode) =>
        ( (n \ "@from").text,
          if(n.attribute("toType").isDefined && (n \ "@toType").text.equals("uri")){(n \ "@to").text} else {(n \ "@to").text}
        )
      ).toMap

  val ns =            (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("ns") }).getOrElse(<propery value="http://undefined.com/"/>)) \ "@value").text
  val blockProperty = (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("blockProperty") }).getOrElse(<propery value="http://undefined.com/"/>)) \ "@value").text
  val senseProperty = (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("senseProperty") }).getOrElse(<propery value="http://undefined.com/"/>)) \ "@value").text
  val senseIdVarName = (((config \ "properties" \ "property").find( {n : XMLNode => (n \ "@name").text.equals("senseVarName") }).getOrElse(<propery value="meaning_id"/>)) \ "@value").text

  val wiktionaryDataset : Dataset = new Dataset("wiktionary")
  val tripleContext = new IriRef(ns)
  val senseIriRef = new IriRef(senseProperty)

  //to cache last used blockIris (from block name to its uri)
  val blockIris = new HashMap[String, IriRef]

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    // wait a random number of seconds. kills parallelism - otherwise debug output from different threads is mixed
    //TODO remove if in production
    val r = new scala.util.Random
    Thread sleep r.nextInt(10)*1000

    val quads = new ListBuffer[Quad]()
    val word = subjectUri.split("/").last
    val senses = HashMap[String, Set[String]]()

    measure {
      val pageConfig = Page.fromNode((config \ "page").head)
      blockIris("page") = new IriRef(ns + word) //this is also the base-url (all nested blocks will get uris with this as a prefix)

      val pageStack =  new Stack[Node]().pushAll(page.children.reverse)

      val proAndEpilogBindings : ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]] = new ListBuffer

      //handle prolog (beginning) (e.g. "see also") - not related to blocks, but to the main entity of the page
      for(prolog <- config \ "page" \ "prologs" \ "template"){
        val prologtpl = Tpl.fromNode(prolog)
         try {
          proAndEpilogBindings.append( (prologtpl, parseNodesWithTemplate(prologtpl.tpl, pageStack)) )
        } catch {
          case e : WiktionaryException => proAndEpilogBindings.append( (prologtpl, e.vars) )
        }
      }

      //handle epilog (ending) (e.g. "links to other languages") by parsing the page backwards
      val rev = new Stack[Node] pushAll pageStack //reversed
      for(epilog <- config \ "page" \ "epilogs" \ "template"){
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

      //handle the bindings from pro- and epilog
      proAndEpilogBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
         quads appendAll handleBlockBinding(pageConfig, tpl, tplBindings, senses)
      }})

      //keep track where we are in the page block hierarchy
      val curOpenBlocks = new ListBuffer[Block]()
      curOpenBlocks append pageConfig

      //keep track if we consumed at least one node in this while run - if not, drop one node at the end
      var consumed = false
      while(pageStack.size > 0){
        // try recognizing block starts of blocks. if recognized we go somewhere UP the hierarchy (the block ended) or one step DOWN (new sub block)
        // each block has a "indicator-template" (indTpl)
        // when it matches, the block starts. and from that template we get bindings that describe the block

        consumed = false
        val possibleBlocks = curOpenBlocks ++ (if(curOpenBlocks.last.blocks.isDefined){List[Block](curOpenBlocks.last.blocks.get)} else {List[Block]()})

        for(block <- possibleBlocks){
          if(block.indTpl == null){
            //continue - the "page" block has no indicator template, it starts implicitly with the page
          } else {
            //println(pageStack.take(1).map(_.dumpStrShort).mkString)
            try {
              //println("vs")
              //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
              val blockIndBindings =  parseNodesWithTemplate(block.indTpl.tpl.clone, pageStack)
              //no exception -> success -> stuff below here will be executed on success
              consumed = true

              //check where in the hierarchy the new opended block is
              if(!curOpenBlocks.exists((b:Block)=>if(b.indTpl == null || block.indTpl == null){false} else {b.indTpl.name == block.indTpl.name})){
                //the new block is not up in the hierarchy
                //one step down/deeper is the only possible alternative
               curOpenBlocks append curOpenBlocks.last.blocks.get
              } else {
                //the new block somewhere up the hierarchy
                val newOpen = new ListBuffer[Block]
                var seen = false
                curOpenBlocks.foreach((b : Block) => {
                  if(!seen){newOpen append b}
                  if(b.indTpl != null && b.indTpl.name == block.indTpl.name){seen = true}
                })
                curOpenBlocks.clear()
                curOpenBlocks.appendAll(newOpen) // up
              }

              //build a uri for the block
              val lastBlockName = if(curOpenBlocks.size > 2){curOpenBlocks(curOpenBlocks.size - 2).indTpl.name} else {"page"}
              val lastBlock = curOpenBlocks(curOpenBlocks.size - 2)

              val blockIdentifier = new StringBuffer(blockIris(lastBlockName).uri)

              block.indTpl.vars.foreach((varr : Var) => {
                //concatenate all binding values of the block indicator tpl to form a proper name for the block (sufficient?)
                val objStr = blockIndBindings.getFirstBinding(varr.name).getOrElse(List()).myToString
                val localBlockPropertyMapped = handleObjectMapping(varr, objStr, false) //this false forces a literal to be returned
                blockIdentifier append ("-" + localBlockPropertyMapped.asInstanceOf[PlainLiteral].value)
              })
              val blockIri = new IriRef(blockIdentifier.toString)
              blockIris(block.indTpl.name) = blockIri
              //generate triples that identify the block
              block.indTpl.vars.foreach((varr : Var) => {
                val objStr = blockIndBindings.getFirstBinding(varr.name).getOrElse(List()).myToString
                val obj = handleObjectMapping(varr, objStr)
                quads += new Quad(wiktionaryDataset, blockIri, new IriRef(varr.property), obj, tripleContext)
              })

              //generate a triple that connects the parent block to the new block
              quads += new Quad(wiktionaryDataset, blockIris(lastBlockName), new IriRef(lastBlock.blocks.get.property), blockIri, tripleContext)

            } catch {
              case e : WiktionaryException => //did not match
            }
          }
        }

        val curBlock = curOpenBlocks.last
        //try matching this blocks templates
        for(tpl <- curBlock.templates){
          //println(pageStack.take(1).map(_.dumpStrShort).mkString)
          try {
            //println("vs")
            //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
            val blockBindings =  parseNodesWithTemplate(tpl.tpl.clone, pageStack)
            //no exception -> success -> stuff below here will be executed on success
            consumed = true
            //generate triples
            //println(tpl.name +": "+ blockBindings.dump())
            quads appendAll handleBlockBinding(curBlock, tpl, blockBindings, senses)
          } catch {
            case e : WiktionaryException => //did not match
          }
        }

        if(!consumed){
          pageStack.pop
        }
      }

    } report {
      duration : Long => println("took "+ duration +"ms")
    }
    println(""+quads.size+" quads extracted for "+word)
    val quadsSortedImmutable = quads.sortWith((q1, q2)=> q1.subject.uri.compare(q2.subject.uri) < 0).toList
    quadsSortedImmutable.foreach((q : Quad) => println(q.renderNTriple))
    new Graph(quadsSortedImmutable)
  }

  def handleBlockBinding(block : Block, tpl : Tpl, blockBindings : VarBindingsHierarchical, emittedBlockSenseConnections : HashMap[String,Set[String]]) : List[Quad] = {
    val quads = new ListBuffer[Quad]

    val blockName = if(block.indTpl != null){block.indTpl.name} else {"page"}
    if(tpl.needsPostProcessing){
      //TODO does not work yet, implement the invocation of a static method that does a transformation of the bindings
      val clazz = ClassLoader.getSystemClassLoader().loadClass(tpl.ppClass.get)
      val method = clazz.getDeclaredMethod(tpl.ppMethod.get, null);
      val ret = method.invoke(blockBindings, null)
      quads ++= ret.asInstanceOf[List[Quad]]
    } else {
      //generate a triple for each var binding
      tpl.vars.foreach((varr : Var) => {
        if(varr.senseBound){
          //handle sense bound vars (e.g. meaning)
          val bindings = blockBindings.getAllSenseBoundVarBindings(varr.name, senseIdVarName)
          bindings.foreach({case (sense, senseBindings) =>
            senseBindings.foreach((binding)=>{
              //the sense identifier is mostly something like "[1]" - sense is then List(TextNode("1"))
              val objStr = binding.myToString

              //avoid useless triples
              if(!objStr.equals("")){
                //map object from (language-specific) literals to (universal) URIs if possible
                val obj = handleObjectMapping(varr, objStr)
                val senseUri = new IriRef(blockIris(blockName).uri + "-" + sense.myToString)

                quads += new Quad(wiktionaryDataset, senseUri, new IriRef(varr.property), obj, tripleContext)

                //connect emittedBlockSenseConnections to their blocks (collect for distinctness)
                var emitThis = false
                if(!emittedBlockSenseConnections.contains(blockIris(blockName).uri)  ){
                  emittedBlockSenseConnections(blockIris(blockName).uri) = Set(senseUri.uri)
                  emitThis = true
                } else if(!emittedBlockSenseConnections(blockIris(blockName).uri).contains(senseUri.uri)){
                  emittedBlockSenseConnections(blockIris(blockName).uri).add(senseUri.uri)
                  emitThis = true
                }
                if(emitThis){
                  println("saved senseUri "+senseUri.render+ " size"+emittedBlockSenseConnections.size)
                  quads += new Quad(wiktionaryDataset, blockIris(blockName), senseIriRef, senseUri, tripleContext)
                }
              }
            })
          })
        } else {
          //handle non-sense bound vars - they are related to the whole block/usage (e.g. hyphenation)
          val bindings = blockBindings.getAllBindings(varr.name)
          for(binding <- bindings){
            val objStr = binding.myToString
            if(!objStr.equals("")){
              val obj = handleObjectMapping(varr, objStr)
              quads += new Quad(wiktionaryDataset, blockIris(blockName), new IriRef(varr.property), obj, tripleContext)
            }
          }
        }
      })
    }

    /*//emit connections from block to its emittedBlockSenseConnections
    for((parentBlock:IriRef, sense:IriRef) <- emittedBlockSenseConnections)  {
      quads += new Quad(wiktionaryDataset, parentBlock, senseIriRef, sense, tripleContext)
    }*/
    quads.toList
  }

  def handleObjectMapping(varr: Var, objectStr: String, toUri : Boolean = true) : GraphNode = {
    printMsg("varr.name= "+varr.name+"varr.format="+varr.format)
    if(varr.doMapping){
      val mapped = mappings(objectStr)
      if(mappings.contains(objectStr)){
        if(toUri){
          if(!varr.format.equals("")){
            new IriRef(varr.format.format(mapped ) )
          } else {
            new IriRef(ns + mapped)
          }
        } else {
          new PlainLiteral(mapped)
        }
      } else {
        new PlainLiteral(objectStr)
      }
    } else {
      new PlainLiteral(objectStr)
    }
  }
}
