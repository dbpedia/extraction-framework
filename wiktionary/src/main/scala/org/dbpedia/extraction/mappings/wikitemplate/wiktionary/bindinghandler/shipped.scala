package org.dbpedia.extraction.mappings.wikitemplate.wiktionary.bindinghandler

import org.openrdf.model._
import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language
import scala.util.matching.Regex
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.mappings.WiktionaryPageExtractor
import org.dbpedia.extraction.mappings.wikitemplate._
import org.dbpedia.extraction.mappings.Cache
import collection.mutable.ListBuffer
import org.dbpedia.extraction.wikiparser.TextNode
import scala.Some
import org.dbpedia.extraction.wikiparser.TemplateNode

//implicit conversion magic
import MyNodeList._
import MyNode._
import MyStack._
import Logging._
import MyLinkNode._

trait TranslationHelper extends BindingHandler {
    val translateCleanPattern = new Regex("\\([^\\)]*\\)")
    def getCleanWord(dirty:String) = translateCleanPattern.replaceAllIn(dirty.split(",").head, "").trim
    
    def getTranslateTriples(source : Resource, property : URI, word : String, language : String, llangLowerShort : String, thisBlockURI : String) : List[Statement] = {
        val quads = new ListBuffer[Statement]()
        val wordObj = vf.createURI(WiktionaryPageExtractor.resourceNS+WiktionaryPageExtractor.urify(word))
        val wordLangObj = vf.createURI(WiktionaryPageExtractor.resourceNS+WiktionaryPageExtractor.urify(word)+"-"+WiktionaryPageExtractor.urify(language))
        //main translate triple
        quads += vf.createStatement(source, property, wordLangObj)
        //the triple inversed
        //quads += new Quad(WiktionaryPageExtractor.langObj, WiktionaryPageExtractor.datasetURI, wordLangObj, property, source, WiktionaryPageExtractor.tripleContext) 
        //a triple about the target word
        quads += vf.createStatement(wordObj, vf.createURI("http://wiktionary.dbpedia.org/terms/hasLangUsage"), wordLangObj)
        //a label for the target word
        quads += vf.createStatement(wordObj, vf.createURI("http://www.w3.org/2000/01/rdf-schema#label"), vf.createLiteral(word))
        quads += vf.createStatement(wordLangObj, vf.createURI("http://www.w3.org/2000/01/rdf-schema#label"), vf.createLiteral(word, llangLowerShort))
        quads.toList
    }
}

class GermanTranslationHelper extends TranslationHelper {
    val sensePattern = "\\[(\\d+)\\]".r
    def process(i:VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement] = {
        val quads = ListBuffer[Statement]()
        val translateProperty = vf.createURI(WiktionaryPageExtractor.termsNS+"hasTranslation")
        i.foreach(binding=>{
            try {
            val lRaw = binding("lang")(0).asInstanceOf[TemplateNode].title.encoded
            val llangLowerShort = lRaw.toLowerCase
            val language = WiktionaryPageExtractor.map(llangLowerShort)
            val line = binding("line")
            var curSense = "1"
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[TextNode]){
                    val matchOption = sensePattern.findFirstIn(node.asInstanceOf[TextNode].text)
                    if(matchOption.isDefined){
                        curSense = matchOption.get.drop(1).dropRight(1) // drop the brackets
                    }
                } else if(node.isInstanceOf[TemplateNode]){
                    val tplType = node.asInstanceOf[TemplateNode].title.decoded
                    if(tplType == "Ü" || tplType == "Üxx"){
                        val translationTargetWord = getCleanWord(node.asInstanceOf[TemplateNode].property("2").get.children(0).asInstanceOf[TextNode].text)
                        if(translationTargetWord.isEmpty)
                          throw new Exception("translationTargetWord.isEmpty")
                        Logging.printMsg("translationTargetWord: "+translationTargetWord, 4)
                        expandSense(curSense).foreach(sense =>{
                            val translationSourceWord = if(sense.forall(_.isDigit)){ 
                                vf.createURI(thisBlockURI+"-"+WiktionaryPageExtractor.urify(sense+WiktionaryPageExtractor.language))//if the found sense is numeric
                            } else {
                                vf.createURI(thisBlockURI)
                            }
                            quads.appendAll(getTranslateTriples(translationSourceWord, translateProperty, translationTargetWord, language, llangLowerShort, thisBlockURI))
                        })
                    }
                }
                } catch {
                   case e:Exception=> Logging.printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> Logging.printMsg("error processing translation line: "+e.getMessage, 4)//ignore
            }
        })        
        quads.toList
    }
}


class EnglishTranslationHelper extends TranslationHelper {
    //todo make configurabel. problem: multiple values for one property
    val targetTemplates = List("t", "t+","t-", "tø", "trad+", "trad-")
    def process(i:VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement] = {
        val quads = ListBuffer[Statement]()
        val translateProperty = vf.createURI(WiktionaryPageExtractor.termsNS+"hasTranslation")
        
        i.foreach( binding => {
            try {
            val langRaw = binding("lang").toReadableString.trim
            val language = WiktionaryPageExtractor.map(langRaw)
            val line = binding("line")
            val senseGloss = binding("sense").toReadableString.trim
            val sense = cache.matcher.getIdOption(senseGloss)

            val translationSourceWord = vf.createURI(thisBlockURI+(if(sense.isDefined){"-"+WiktionaryPageExtractor.urify(sense.get+WiktionaryPageExtractor.language)} else {""}))
            line.foreach(node=>{
                try{
                  if(node.isInstanceOf[TemplateNode]){
                    val tplType = node.asInstanceOf[TemplateNode].title.decoded
                    if(targetTemplates.contains(tplType)){
                        val translationTargetLanguage = node.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text
                        val translationTargetWord = getCleanWord(node.asInstanceOf[TemplateNode].property("2").get.children(0).asInstanceOf[TextNode].text)
                        Logging.printMsg("translationTargetWord: "+translationTargetWord, 4)
                        quads.appendAll(getTranslateTriples(translationSourceWord, translateProperty, translationTargetWord, language, translationTargetLanguage, thisBlockURI))
                    }
                  }
                } catch {
                   case e:Exception=> Logging.printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> Logging.printMsg("error processing translation line: "+e.getMessage, 4)//ignore
            }
        })        
        quads.toList
    }
}

/**
* 
*
*/
class LinkListHelper extends BindingHandler {
    def process(i:VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement] = {
        val quads = ListBuffer[Statement]()
        val linkProperty = vf.createURI(parameters("linkProperty"))
        val sourceWord = vf.createURI(thisBlockURI)
        i.foreach(binding=>{
            try {
            var line = binding("line")
            
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[LinkNode]){
                    val destination = node.asInstanceOf[LinkNode].getFullDestination(WiktionaryPageExtractor.resourceNS)
                    quads += vf.createStatement(sourceWord, linkProperty, vf.createURI(destination))

                }
                } catch {
                   case e:Exception=> Logging.printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> Logging.printMsg("error processing translation line: "+e.getMessage, 4)//ignore
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
class ExplicitSenseLinkListHelper extends BindingHandler {
    def process(i:VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement] = {
        val quads = ListBuffer[Statement]()
        val linkProperty = vf.createURI(parameters("linkProperty"))
        i.foreach(binding=>{
            try {
            val senses = binding("sense")(0).toWikiText.replace("\n","").replace(":", "").substring(1, binding("sense")(0).toWikiText.length-1)
            var line = binding("line")
            
            line.foreach(node=>{
                try{
                if(node.isInstanceOf[LinkNode]){
                    expandSense(senses).foreach(sense =>{
                        val sourceWord = if(sense.forall(_.isDigit)){
                          vf.createURI(thisBlockURI+"-"+WiktionaryPageExtractor.urify(sense)+WiktionaryPageExtractor.language) //if the found sense is numeric
                        } else {
                          vf.createURI(thisBlockURI)
                        }
                        val destination = node.asInstanceOf[LinkNode].getFullDestination(WiktionaryPageExtractor.resourceNS)
                        quads += vf.createStatement(sourceWord, linkProperty, vf.createURI(destination))
                    })
                } 
                } catch {
                   case e:Exception=> Logging.printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> Logging.printMsg("error processing translation line: "+e.getMessage, 4)//ignore
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
class MatchedSenseLinkListHelper extends BindingHandler {
    def process(i:VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement] = {
        val quads = ListBuffer[Statement]()
        val linkProperty = vf.createURI(parameters("linkProperty"))
        i.foreach(binding=>{
            try {
            val senseRawOption = binding("line").find( (n : Node) => {n.isInstanceOf[TemplateNode] && n.asInstanceOf[TemplateNode].title.decoded.equals("sense")})
            val senseOption = if(binding.contains("sense")){
                Some(cache.matcher.getId(binding("sense").toReadableString))
            } else if(senseRawOption.isDefined){
                Some(cache.matcher.getId(senseRawOption.get.asInstanceOf[TemplateNode].property("1").get.children(0).asInstanceOf[TextNode].text))
            } else {None}
            var line = binding("line")
            
            line.foreach(node=>{
                try{
                    if(node.isInstanceOf[LinkNode]){
                        val sourceWord = if(senseOption.isDefined){
                          vf.createURI(thisBlockURI+"-"+senseOption.get+WiktionaryPageExtractor.language)
                        } else {
                          vf.createURI(thisBlockURI)
                        }
                        val destination = node.asInstanceOf[LinkNode].getFullDestination(WiktionaryPageExtractor.resourceNS)
                        quads += vf.createStatement(sourceWord, linkProperty, vf.createURI(destination))
                    } 
                } catch {
                   case e:Exception=> Logging.printMsg("error processing translation item: "+e.getMessage, 4)//ignore
                }
            })
            } catch {
               case e:Exception=> Logging.printMsg("error processing translation line: "+e.getMessage, 4)//ignore
            }
        })        
        quads.toList
    }
}

