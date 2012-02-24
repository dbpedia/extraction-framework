package org.dbpedia.extraction.mappings

//import org.dbpedia.extraction.config.mappings.WiktionaryPageExtractorConfig
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import java.util.Locale
import org.dbpedia.extraction.destinations.{Graph, Quad, Dataset}
import org.openrdf.model.{Literal, URI, Resource, Value}
import org.openrdf.model.impl.ValueFactoryImpl
import util.control.Breaks._
import java.io.FileNotFoundException
import java.io.FileWriter
import java.net.URLEncoder
import java.lang.StringBuffer
import xml.{XML, Node => XMLNode}
import scala.util.matching.Regex
import collection.mutable.{HashMap, Stack, ListBuffer, Set}

//some of my utilities
import MyNodeList._
import MyNode._
import MyStack._
import TimeMeasurement._
import VarBinder._
import Logging._

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
 * a page can contain information about multiple entities (sequential blocks)
 *
 * @author Jonas Brekle <jonas.brekle@gmail.com>
 * @author Sebastian Hellmann <hellmann@informatik.uni-leipzig.de>
 */

class WiktionaryPageExtractor( context : {} ) extends Extractor {

  //load config from xml
  private val config = XML.loadFile("config.xml")

  val properties : Map[String, String] = (config \ "properties" \ "property").map(
      (n : XMLNode) =>
        ( (n \ "@name").text,
          (n \ "@value").text
        )
      ).toMap

  val language = properties("language")
  val logLevel = properties("logLevel").toInt

  val langObj = new Language(language, new Locale(language))

  val vf = ValueFactoryImpl.getInstance
  Logging.level = logLevel
  Logging.printMsg("wiktionary loglevel = "+logLevel,0)

  val ns =            properties.get("ns").getOrElse("http://undefined.com/")
  val blockProperty = properties.get("blockProperty").getOrElse("http://undefined.com/block")
  val labelProperty = properties.get("labelProperty").getOrElse("http://undefined.com/label")
  val varPattern = new Regex("\\$[a-zA-Z0-9]+")

  private val languageConfig = XML.loadFile("config-"+language+".xml")

  private val mappings : Map[String, String] = (languageConfig \\ "mapping").map(
      (n : XMLNode) => //language specific mappings (from language ("Spanisch") to global vocabulary ("spanish"))
        ( (n \ "@from").text,
          (n \ "@to").text
        )
      ).toMap ++ (config \ "mappings"\ "mapping").map(
      (n : XMLNode) => // general mappings (from language codes ("es") to global vocabulary ("spanish"))
        ( (n \ "@from").text,
          (n \ "@to").text
        )
      ).toMap

  val ignoreStart = (languageConfig \ "ignore" \ "page").filter(_.attribute("startsWith").isDefined).map((n : XMLNode) => (n \ "@startsWith").text)
  val ignoreEnd = (languageConfig \ "ignore" \ "page").filter(_.attribute("endsWith").isDefined).map((n : XMLNode) => (n \ "@endsWith").text)

  val pageConfig = new Page((languageConfig \ "page")(0))

  val datasetURI : Dataset = new Dataset("wiktionary.dbpedia.org")
  val tripleContext = vf.createURI(ns.replace("http://","http://"+language+"."))

  val missingMappings = Set[String]()
  val usedMappings = Set[String]()
  var counter = 0

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    // wait a random number of seconds. kills parallelism - otherwise debug output from different threads is mixed
    if(logLevel > 0){
      val r = new scala.util.Random
      Thread sleep r.nextInt(10)*1000
    }
    val missingMappingsT = Set[String]()
    missingMappingsT ++= missingMappings
    missingMappings.empty
    counter += 1 

    val quads = new ListBuffer[Quad]()
    val entityId = subjectUri.split("/").last

    //skip some useless pages    
    for(start <- ignoreStart){
        if(entityId.startsWith(start)){
            return new Graph(quads.toList)
        }
    }
    for(end <- ignoreEnd){
        if(entityId.endsWith(end)){
            return new Graph(quads.toList)
        }
    }

    Logging.printMsg("processing "+entityId, 1)

    //to cache last used blockIris (from block name to its uri)
    val blockIris = new HashMap[String, URI]
    measure {
      
      blockIris("page") = vf.createURI(ns + URLEncoder.encode(entityId, "UTF-8")) //this is also the base-url (all nested blocks will get uris with this as a prefix)

      quads append new Quad(langObj, datasetURI, blockIris("page"), vf.createURI(labelProperty), vf.createLiteral(entityId), tripleContext)

      val pageStack =  new Stack[Node]().pushAll(page.children.reverse).filterSpaces
      val proAndEpilogBindings : ListBuffer[Tuple2[Tpl, VarBindingsHierarchical]] = new ListBuffer
      //handle prolog (beginning) (e.g. "see also") - not related to blocks, but to the main entity of the page
      for(prolog <- languageConfig \ "page" \ "prologs" \ "template"){
        val prologtpl = Tpl.fromNode(prolog)
        Logging.printMsg("try "+prologtpl.name, 2)
         try {
          proAndEpilogBindings.append( (prologtpl, parseNodesWithTemplate(prologtpl.wiki, pageStack)) )
        } catch {
          case e : WiktionaryException => proAndEpilogBindings.append( (prologtpl, e.vars) )
        }
      }
      Logging.printMsg(proAndEpilogBindings.size+ " prologs configured", 2)

      //handle epilog (ending) (e.g. "links to other languages") by parsing the page backwards
      val rev = new Stack[Node] pushAll pageStack //reversed
      for(epilog <- languageConfig \ "page" \ "epilogs" \ "template"){
        val epilogtpl = Tpl.fromNode(epilog)
        try {
          proAndEpilogBindings.append( (epilogtpl, parseNodesWithTemplate(epilogtpl.wiki, rev)) )
        } catch {
          case e : WiktionaryException => proAndEpilogBindings.append( (epilogtpl, e.vars) )
        }
      }
      Logging.printMsg(proAndEpilogBindings.size+ " prologs and epilogs found", 2)

      //apply consumed nodes (from the reversed page) to pageStack  (unreversed)
      pageStack.clear
      pageStack pushAll rev

      //handle the bindings from pro- and epilog
      proAndEpilogBindings.foreach({case (tpl : Tpl, tplBindings : VarBindingsHierarchical) => {
         quads appendAll handleFlatBindings(tplBindings.getFlat(), pageConfig, tpl, blockIris, blockIris("page").stringValue)
      }})
      Logging.printMsg("pro- and epilog bindings handled", 2)

      //keep track where we are in the page block hierarchy
      val curOpenBlocks = new ListBuffer[Block]()
      curOpenBlocks append pageConfig
      var curBlock : Block = pageConfig

      //keep track if we consumed at least one node in this while run - if not, drop one node at the end
      var consumed = false
      while(pageStack.size > 0){
        Logging.printMsg("page node: "+pageStack.head.toWikiText(), 2)
        consumed = false

        val possibleBlocks = curOpenBlocks.map(_.blocks).foldLeft(List[Block]()){ _ ::: _ } //:: pageConfig
        //val possibleTemplates = curOpenBlocks.foldLeft(List[Tpl]()){(all,cur)=>all ::: cur.templates} 
        //println("possibleTemplates="+ possibleTemplates.map(t=>t.name) )
        //println("possibleBlocks="+ possibleBlocks.map(_.name) )

        //try matching this blocks templates
        for(tpl <- curBlock.templates){
        //for(tpl <- possibleTemplates){
          Logging.printMsg("trying template "+tpl.name, 2)

          //println(pageStack.take(1).map(_.dumpStrShort).mkString)
          try {
            //println("vs")
            //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
            val blockBindings =  parseNodesWithTemplate(tpl.wiki.clone, pageStack)
            //no exception -> success -> stuff below here will be executed on success
            consumed = true
            Logging.printMsg("finished template "+tpl.name+" successfully", 2)

            //generate triples
            //println(tpl.name +": "+ blockBindings.dump())
            quads appendAll handleFlatBindings(blockBindings.getFlat(), curBlock, tpl, blockIris, blockIris(curBlock.name).stringValue)
          } catch {
            case e : WiktionaryException => //did not match
          }
        }

        if(pageStack.size > 0){
          // try recognizing block starts of blocks. if recognized we go somewhere UP the hierarchy (the block ended) or one step DOWN (new sub block)
          // each block has a "indicator-template" (indTpl)
          // when it matches, the block starts. and from that template we get bindings that describe the block

          Logging.printMsg("trying block indicator templates. page node: "+pageStack.head.toWikiText, 2)
          breakable {
            for(block <- possibleBlocks){
              if(block.indTpl == null){
                //continue - the "page" block has no indicator template, it starts implicitly with the page
              } else {
                //println(pageStack.take(1).map(_.dumpStrShort).mkString)
                try {
                  //println("vs")
                  //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
                  val blockIndBindings = parseNodesWithTemplate(block.indTpl.wiki.clone, pageStack)
                  //no exception -> success -> stuff below here will be executed on success
                  consumed = true
                  var oldBlockUri = blockIris(curBlock.name).stringValue
                  curBlock = block //switch to new block
                  Logging.printMsg("block indicator template "+block.indTpl.name+" matched", 2)
                  
                  //check where in the hierarchy the new opended block is
                  if(!curOpenBlocks.contains(block)){
                    // the new block is not up in the hierarchy
                    // go one step down/deeper 
                   curOpenBlocks append block
                  } else {
                    //the new block somewhere up the hierarchy
                    val newOpen = curOpenBlocks.takeWhile(_ != block) 
                    oldBlockUri = blockIris(newOpen.last.name).stringValue
                    curOpenBlocks.clear()
                    curOpenBlocks.appendAll(newOpen) 
                    curOpenBlocks.append(block)// up
                  }
                  
                  quads appendAll handleFlatBindings(blockIndBindings.getFlat(), block, block.indTpl, blockIris, oldBlockUri)
                  
                  break; //dont match another block indicator template right away (continue with this blocks templates)
                } catch {
                  case e : WiktionaryException => //did not match
                }
              }
            }
          }
        }
        if(!consumed){
          Logging.printMsg("skipping unconsumable node ", 2)
          pageStack.pop
        }
      }

    } report {
      duration : Long => Logging.printMsg("took "+ duration +"ms", 1)
    }
    
    if(missingMappings.diff(missingMappingsT).size > 0){
      Logging.printMsg("missing mapping: "+missingMappings.diff(missingMappingsT).toString,1)
      missingMappings ++= missingMappings.diff(missingMappingsT)
    }
    missingMappings ++= missingMappingsT

    if(counter == 100000){
       Logging.printMsg("unused mapping: "+mappings.keySet.--(usedMappings),1)
    }

    Logging.printMsg(""+quads.size+" quads extracted for "+entityId, 1)
    val quadsSortedDistinct = quads.groupBy(_.renderNTriple).map(_._2.head).toList.sortWith((q1, q2)=> q1.renderNTriple.compareTo(q2.renderNTriple) > 0)
    quadsSortedDistinct.foreach( q => { Logging.printMsg(q.renderNTriple, 1) } )
    new Graph(quadsSortedDistinct)
  }

  def handleFlatBindings(bindings : VarBindings, block : Block, tpl : Tpl, blockIris : HashMap[String, URI], thisBlockIri : String) : List[Quad] = {
    val quads = new ListBuffer[Quad]
    var curBlockIri = thisBlockIri

    if(tpl.pp.isDefined){
        val clazz = Class.forName("org.dbpedia.extraction.mappings."+tpl.pp.get.clazz).newInstance.asInstanceOf[PostProcessor]
        return clazz.process(bindings, block, tpl, blockIris, thisBlockIri, langObj, datasetURI, tripleContext, ns, tpl.pp.get.parameters, mappings)
    }
    
    bindings.foreach( (binding : HashMap[String, List[Node]]) => {
      Logging.printMsg("bindings "+binding, 2)
      tpl.resultTemplates.foreach( (rt : ResultTemplate) => {
        try {
          val thisTplQuads = new ListBuffer[Quad]
          rt.triples.foreach( (tt : TripleTemplate) => {
            val s = varPattern.replaceAllIn( tt.s, 
              (m) => {
                val varName = m.matched.replace("$","");
                if(!varName.equals("block")){
                  if(!binding.contains(varName)){throw new Exception("missing binding for "+varName)};
                  binding(varName).myToString
                } else {curBlockIri}
              }
            )

            val p = varPattern.replaceAllIn( tt.p, 
              (m) => {
                val varName = m.matched.replace("$","");
                if(!varName.equals("block")){
                  if(!binding.contains(varName)){throw new Exception("missing binding for "+varName)};
                  binding(varName).myToString
                } else {curBlockIri}
              }
            )

            val o = varPattern.replaceAllIn( tt.o, 
              (m) => {
                val varName = m.matched.replace("$","");
                if(!varName.equals("block")){
                  if(!binding.contains(varName)){throw new Exception("missing binding for "+varName)};
                  if(tt.oMapping){
                    val b = binding(varName).myToString
                    mappings.getOrElse(b, b)
                  } else {
                    binding(varName).myToString
                  }
                } else {curBlockIri}
              }
            )
            Logging.printMsg("emmiting triple "+s+" "+p+" "+o, 2)
            val oObj = if(tt.oType == "URI"){
                val blockIri = vf.createURI(o)
                if(tt.oNewBlock){
                  blockIris(block.name) = blockIri //save for later
                  Logging.printMsg("entering "+blockIri, 2)
                  curBlockIri = o
                }
                blockIri
              } else {
                vf.createLiteral(o)
              }
            thisTplQuads += new Quad(langObj, datasetURI, vf.createURI(s), vf.createURI(p), oObj, tripleContext)
          })
          quads appendAll thisTplQuads //the triples are added at once after they all were created without a exception
        } catch {
          case e1:java.util.NoSuchElementException => //e1.printStackTrace
          case e => //println(e)
        }
      })
    })
    quads.toList
  }
}

trait PostProcessor {
    def expandSense(s:String):List[String] = {
        val senses = new ListBuffer[String]()
        s.replace(" ", "").split(",").foreach(block=>{
            if(block.contains("-")){
                val ab = block.split("-")
                ab(0).toInt.until(ab(1).toInt).foreach(
                    num => senses += num.toString
                )
            } else {
                senses += block
            }
        })
        senses.toList
    }
    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String]) : List[Quad]
}

class TranslationHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String]) : List[Quad] = {
        val quads = ListBuffer[Quad]()
        val translateProperty = vf.createURI(ns+"hasTranslation")
        i.foreach(binding=>{
            try {
            val lRaw = binding("lang")(0).asInstanceOf[TemplateNode].title.decoded.toLowerCase
            val language = mappings.getOrElse(lRaw, lRaw)
            val line = binding("line")
            var curSense = "1"
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[LinkNode]){
                   curSense = node.toWikiText.substring(1, node.toWikiText.length-1)
                } else if(node.isInstanceOf[TemplateNode]){
                    val tplType = node.asInstanceOf[TemplateNode].title.decoded
                    if(tplType == "Ü" || tplType == "Üxx"){
                        val translationTargetWord = node.asInstanceOf[TemplateNode].property("2").get.children(0).asInstanceOf[TextNode].text
                        printMsg("translationTargetWord: "+translationTargetWord, 4)
                        expandSense(curSense).foreach(sense =>{
                            val translationSourceWord = if(sense.equals("?")){ 
                                vf.createURI(tBI)
                            } else {
                                vf.createURI(tBI+"-"+sense)
                            }
                            quads += new Quad(langObj, datasetURI, translationSourceWord, translateProperty, vf.createURI(ns+URLEncoder.encode(translationTargetWord.trim, "UTF-8")+"-"+language), tripleContext)
                        })
                    }
                }
                } catch {
                   case e:Exception=> printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> printMsg("error processing translation line: "+e.getMessage, 4)//ignore
            }
        })        
        quads.toList
    }
}

/**
* a generic parser for something like 
*[1] [house], [boat]
*[2] [tree]
*
*/
class LinkListHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String]) : List[Quad] = {
        val quads = ListBuffer[Quad]()
        val linkProperty = vf.createURI(parameters("linkProperty"))
        i.foreach(binding=>{
            try {
            val senses = binding("sense")(0).toWikiText.substring(1, binding("sense")(0).toWikiText.length-1)
            var line = binding("line")
            
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[LinkNode]){
                    expandSense(senses).foreach(sense =>{
                        val sourceWord = vf.createURI(tBI+"-"+sense)
                        quads += new Quad(langObj, datasetURI, sourceWord, linkProperty, vf.createURI(ns+URLEncoder.encode(node.asInstanceOf[LinkNode].children(0).asInstanceOf[TextNode].text.trim, "UTF-8")), tripleContext)
                    })
                } 
                } catch {
                   case e:Exception=> printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> printMsg("error processing translation line: "+e.getMessage, 4)//ignore
            }
        })        
        quads.toList
    }
}
