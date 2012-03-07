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
import WiktionaryPageExtractor._ //companion

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

  val ns = properties.get("ns").getOrElse("http://undefined.com/")
  val resourceNS = ns +"resource/"
  val termsNS = ns +"terms/"
  val referenceProperty = properties.get("referenceProperty").getOrElse("http://undefined.com/see")
  val labelProperty = properties.get("labelProperty").getOrElse("http://undefined.com/label")
  val varPattern = new Regex("\\$[a-zA-Z0-9]+")  
  val mapPattern = new Regex("map\\([^)]*\\)")
  val uriPattern = new Regex("uri\\([^)]*\\)")
  val splitPattern = new Regex("\\W")
  val xmlPattern = new Regex("</?[^>]+/?>")

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

  override def extract(page: PageNode, subjectUri: String, pageContext: PageContext): Graph =
  {
    Logging.printMsg("start "+subjectUri+" threadID="+Thread.currentThread().getId(),1)
    // wait a random number of seconds. kills parallelism - otherwise debug output from different threads is mixed
    if(false && logLevel > 0){
      val r = new scala.util.Random
      Thread sleep r.nextInt(10)*1000
    }

    val quads = new ListBuffer[Quad]()
    val entityId = page.title.decoded

    //skip some useless pages    
    for(start <- ignoreStart){
        if(entityId.startsWith(start)){
            Logging.printMsg("ignored "+entityId,1)
            return new Graph(quads.toList)
        }
    }
    for(end <- ignoreEnd){
        if(entityId.endsWith(end)){
            Logging.printMsg("ignored "+entityId,1)
            return new Graph(quads.toList)
        }
    }

    Logging.printMsg("processing "+entityId, 1)

    //to cache last used blockIris (from block name to its uri)
    val blockIris = new HashMap[String, URI]
    measure {
      
      blockIris("page") = vf.createURI(resourceNS + urify(entityId)) //this is also the base-url (all nested blocks will get uris with this as a prefix)

      quads append new Quad(langObj, datasetURI, blockIris("page"), vf.createURI(labelProperty), vf.createLiteral(entityId), tripleContext)
      quads append new Quad(langObj, datasetURI, blockIris("page"), vf.createURI(referenceProperty), vf.createURI("http://"+language+".wiktionary.org/wiki/"+ urify(entityId)), tripleContext)

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
        try {
         quads appendAll handleFlatBindings(tplBindings.getFlat(), entityId, pageConfig, tpl, blockIris, blockIris("page").stringValue)
        } catch { case _ => } //returned bindings are wrong
      }})
      Logging.printMsg("pro- and epilog bindings handled", 2)

      //keep track where we are in the page block hierarchy
      val curOpenBlocks = new ListBuffer[Block]()
      curOpenBlocks append pageConfig
      var curBlock : Block = pageConfig
      var counter = 0
      //keep track if we consumed at least one node in this while run - if not, drop one node at the end
      var consumed = false
      while(pageStack.size > 0){
        counter += 1
        if(counter % 100 == 0){
            Logging.printMsg(""+counter+"/"+page.children.size+" nodes inspected "+entityId,1)
        }
        Logging.printMsg("page node: "+pageStack.head.toWikiText(), 2)
        consumed = false

        val possibleBlocks = curOpenBlocks.map(_.blocks).foldLeft(List[Block]()){ _ ::: _ } //:: pageConfig
        //val possibleTemplates = curOpenBlocks.foldLeft(List[Tpl]()){(all,cur)=>all ::: cur.templates} 
        //println("possibleTemplates="+ possibleTemplates.map(t=>t.name) )
        //println("possibleBlocks="+ possibleBlocks.map(_.name) )

        //try matching this blocks templates
        var triedTemplatesCounter = 0
        while(triedTemplatesCounter < curBlock.templates.size && pageStack.size > 0){
        for(tpl <- curBlock.templates){
          triedTemplatesCounter += 1
          //println(""+triedTemplatesCounter+"/"+curBlock.templates.size)
          //for(tpl <- possibleTemplates){
          Logging.printMsg("trying template "+tpl.name, 3)
          val pageCopy = pageStack.clone 
          //println(pageStack.take(1).map(_.dumpStrShort).mkString)
          try {
            //println("vs")
            //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )

            val blockBindings =  parseNodesWithTemplate(tpl.wiki.clone, pageStack)

            //generate triples
            //println(tpl.name +": "+ blockBindings.dump())
            quads appendAll handleFlatBindings(blockBindings.getFlat(), entityId, curBlock, tpl, blockIris, blockIris(curBlock.name).stringValue)

            //no exception -> success -> stuff below here will be executed on success
            consumed = true
            //reset counter, so all templates need to be tried again
            triedTemplatesCounter = 0
            Logging.printMsg("finished template "+tpl.name+" successfully", 3)
          } catch {
            case e : WiktionaryException => restore(pageStack, pageCopy) //did not match
          }
        }
        }

        if(pageStack.size > 0){
          // try recognizing block starts of blocks. if recognized we go somewhere UP the hierarchy (the block ended) or one step DOWN (new sub block)
          // each block has a "indicator-template" (indTpl)
          // when it matches, the block starts. and from that template we get bindings that describe the block

          Logging.printMsg("trying block indicator templates. page node: "+pageStack.head.toWikiText, 3)
          breakable {
            for(block <- possibleBlocks){
              if(block.indTpl == null){
                //continue - the "page" block has no indicator template, it starts implicitly with the page
              } else {
                //println(pageStack.take(1).map(_.dumpStrShort).mkString)
                val pageCopy = pageStack.clone      
                try {
                  //println("vs")
                  //println(block.indTpl.tpl.map(_.dumpStrShort).mkString )
                  val blockIndBindings = parseNodesWithTemplate(block.indTpl.wiki.clone, pageStack)

                  val oldBlockUri = if(!curOpenBlocks.contains(block)){
                    blockIris(curBlock.name).stringValue
                  } else {
                    blockIris(block.parent.name).stringValue
                  }

                  quads appendAll handleFlatBindings(blockIndBindings.getFlat(), entityId, block, block.indTpl, blockIris, oldBlockUri)

                  //no exception -> success -> stuff below here will be executed on success
                  consumed = true

                  curBlock = block //switch to new block
                  Logging.printMsg("block indicator template "+block.indTpl.name+" matched", 2)
                  
                  //check where in the hierarchy the new opended block is
                  if(!curOpenBlocks.contains(block)){
                    // the new block is not up in the hierarchy
                    // go one step down/deeper 
                    //println("new block detected: "+block.name+" in curOpenBlocks= "+curOpenBlocks.map(_.name))
                    curOpenBlocks append block
                  } else {
                    //the new block somewhere up the hierarchy
                    //go to the parent of that
                    //println("parent block detected: "+block.name+" in curOpenBlocks= "+curOpenBlocks.map(_.name))
                    val newOpen = curOpenBlocks.takeWhile(_ != block)
                    curOpenBlocks.clear()
                    curOpenBlocks.appendAll(newOpen) 
                    //take a new turn
                    curOpenBlocks.append(block)// up
                  }
                  
                  break; //dont match another block indicator template right away (continue with this blocks templates)
                } catch {
                  case e : WiktionaryException => restore(pageStack, pageCopy) //did not match
                }
              }
            }
          }
        }
        if(!consumed){
          Logging.printMsg("skipping unconsumable node ", 2)
          val unconsumeableNode = pageStack.pop
          unconsumeableNode match {
            case tn : TextNode => if(tn.text.startsWith(" ") || tn.text.startsWith("\n")){
                pageStack.push(tn.copy(text=tn.text.substring(1)))
            }
            case _ =>
          }
        }
      }

    } report {
      duration : Long => Logging.printMsg("took "+ duration +"ms", 1)
    }
    
    Logging.printMsg(""+quads.size+" quads extracted for "+entityId, 1)
    val quadsSortedDistinct = quads.groupBy(_.renderNTriple).map(_._2.head).toList.sortWith((q1, q2)=> q1.renderNTriple.compareTo(q2.renderNTriple) < 0)
    quadsSortedDistinct.foreach( q => { Logging.printMsg(q.renderNTriple, 1) } )
    Logging.printMsg("end "+subjectUri+" threadID="+Thread.currentThread().getId(),1)
    new Graph(quadsSortedDistinct)
  }

  /**
   * silly helper function
   */
  protected def restore(st : Stack[Node], backup : Stack[Node]) : Unit = {
    st.clear
    st pushAll backup.reverse
  }

  def handleFlatBindings(bindings : VarBindings, entityId : String, block : Block, tpl : Tpl, blockIris : HashMap[String, URI], thisBlockIri : String) : List[Quad] = {
    val quads = new ListBuffer[Quad]
    var curBlockIri = thisBlockIri

    if(tpl.pp.isDefined){
        val clazz = Class.forName("org.dbpedia.extraction.mappings."+tpl.pp.get.clazz).newInstance.asInstanceOf[PostProcessor]
        return clazz.process(bindings, block, tpl, blockIris, thisBlockIri, langObj, datasetURI, tripleContext, ns, tpl.pp.get.parameters, mappings, resourceNS)
    }
    var bindingsMatchedAnyResultTemplate = false
    bindings.foreach( (binding : HashMap[String, List[Node]]) => {
      Logging.printMsg("bindings "+binding, 2)
      tpl.resultTemplates.foreach( (rt : ResultTemplate) => {
        try {
          val thisTplQuads = new ListBuffer[Quad]
          rt.triples.foreach( (tt : TripleTemplate) => {
            //apply the same placeholder-processing to s, p and o => DRY:
            val in = Map("s"->tt.s, "p"->tt.p, "o"->tt.o)
            val out = in.map( kv => {
                //println("before:"+kv._2)
                val replacedVars = varPattern.replaceAllIn( kv._2, (m) => {
                        val varName = m.matched.substring(1);//exclude the $ symbol
                        val replacement = if(varName.equals("block")){
                            curBlockIri
                        } else if(varName.equals("entityId")){
                            entityId
                        } else {
                            if(!binding.contains(varName)){throw new Exception("missing binding for "+varName)};
                            binding(varName).myToString
                        }
                        replacement.replace(")", "?~*#?") //to prevent, that recorded ) symbols mess up the regex of map and uri
                })
                val mapped = mapPattern.replaceAllIn(replacedVars, (m) => {
                    val found = m.matched
                    val b = found.substring(4, found.length-1)
                    mappings.getOrElse(b, b)
                })
                val encoded = uriPattern.replaceAllIn(mapped, (m) => {
                    val found = m.matched
                    val b = found.substring(4, found.length-1)
                    urify(b)
                })
                //println("after:"+mapped)
                (kv._1, encoded.replace("?~*#?", ")"))
            })
            
            val s = out("s")
            val p = out("p")
            val o = out("o")
            Logging.printMsg("emmiting triple "+s+" "+p+" "+o, 2)
            val oObj = if(tt.oType == "URI"){
                val oURI = vf.createURI(o)
                if(tt.oNewBlock){
                  blockIris(block.name) = oURI //save for later
                  //println(blockIris)
                  Logging.printMsg("entering "+oURI, 2)
                  curBlockIri = o
                }
                oURI
              } else {
                vf.createLiteral(o)
              }
            thisTplQuads += new Quad(langObj, datasetURI, vf.createURI(s), vf.createURI(p), oObj, tripleContext)
          })
          quads appendAll thisTplQuads //the triples are added at once after they all were created without a exception
          bindingsMatchedAnyResultTemplate = true
        } catch {
          case e1:java.util.NoSuchElementException => //e1.printStackTrace
          case e => Logging.printMsg("exception while processing bindings: "+e, 3)
        }
      })
    })
    if(!bindingsMatchedAnyResultTemplate){
        throw new WiktionaryException("result templates were not satisfiable", new VarBindingsHierarchical, None)    
    }
    quads.toList
  }
}
object WiktionaryPageExtractor {
  def urify(in:String):String = in.replace(" ", "_")//URLEncoder.encode(in.trim, "UTF-8")
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
    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String], resourceNS : String) : List[Quad]
 
    def getDestination(ln : Node) : String = {
        ln match {
            case eln : ExternalLinkNode => eln.destination.toString
            case iln : InternalLinkNode => iln.destination.encoded
            case iwln : InterWikiLinkNode => iwln.destination.encoded
            case _ => ln.children.myToString
        }
    }
}

class GermanTranslationHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String], resourceNS : String) : List[Quad] = {
        val quads = ListBuffer[Quad]()
        val translateProperty = vf.createURI(ns+"terms/hasTranslation")
        i.foreach(binding=>{
            try {
            val lRaw = binding("lang")(0).asInstanceOf[TemplateNode].title.decoded.capitalize
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
                                vf.createURI(tBI+"-"+WiktionaryPageExtractor.urify(sense))
                            }
                            quads += new Quad(langObj, datasetURI, translationSourceWord, translateProperty, vf.createURI(resourceNS+WiktionaryPageExtractor.urify(translationTargetWord)+"-"+language), tripleContext)
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


class EnglishTranslationHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String], resourceNS : String) : List[Quad] = {
        val quads = ListBuffer[Quad]()
        val translateProperty = vf.createURI(ns+"terms/hasTranslation")
        val translationSourceWord = vf.createURI(tBI)
        i.foreach(binding=>{
            try {
            val langFull = binding("lang").myToString.replace("[^a-zA-Z]","")
            val line = binding("line")
            line.foreach(node=>{
                try{
                  if(node.isInstanceOf[TemplateNode]){
                    val tplType = node.asInstanceOf[TemplateNode].title.decoded
                    if(tplType == "t+" || tplType == "t-" || tplType == "tø" || tplType == "t"){
                        val translationTargetLanguage = node.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text
                        val translationTargetWord = node.asInstanceOf[TemplateNode].property("2").get.children(0).asInstanceOf[TextNode].text
                        printMsg("translationTargetWord: "+translationTargetWord, 4)
                       quads += new Quad(langObj, datasetURI, translationSourceWord, translateProperty, vf.createURI(resourceNS+WiktionaryPageExtractor.urify(translationTargetWord)+"-"+mappings.getOrElse(translationTargetLanguage,translationTargetLanguage)), tripleContext)
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
class SenseLinkListHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String], resourceNS : String) : List[Quad] = {
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
                        quads += new Quad(langObj, datasetURI, sourceWord, linkProperty, vf.createURI(resourceNS+WiktionaryPageExtractor.urify(getDestination(node))), tripleContext)
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
class LinkListHelper extends PostProcessor{
    val vf = ValueFactoryImpl.getInstance

    def process(i:VarBindings, b : Block, t : Tpl, bI : HashMap[String, URI], tBI : String, langObj : Language, datasetURI : Dataset, tripleContext : Resource, ns : String, parameters : Map[String, String], mappings : Map[String, String], resourceNS : String) : List[Quad] = {
        val quads = ListBuffer[Quad]()
        val linkProperty = vf.createURI(parameters("linkProperty"))
        val sourceWord = vf.createURI(tBI)
        i.foreach(binding=>{
            try {
            var line = binding("line")
            
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[LinkNode]){
                    quads += new Quad(langObj, datasetURI, sourceWord, linkProperty, vf.createURI(resourceNS+WiktionaryPageExtractor.urify(getDestination(node))), tripleContext)

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
