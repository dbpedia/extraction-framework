package org.dbpedia.extraction.mappings.wikitemplate.wiktionary.postprocessor

import org.dbpedia.extraction.mappings.WiktionaryPageExtractor
import org.dbpedia.extraction.mappings.wikitemplate.PostProcessor
import xml.{Node => XMLNode, NodeSeq}
import org.dbpedia.extraction.destinations.{Quad, Dataset}
import org.dbpedia.extraction.util.Language

import org.openrdf.model.{Value, Resource, Literal}
import org.openrdf.model.impl.ValueFactoryImpl
import collection.mutable.{ListBuffer, Map=>MMap}

class LemonOverlay (config : NodeSeq) extends PostProcessor {
    val blockProperty = vf.createURI((config \ "blockProperty" \ "@uri").text)
    val inputTargetClass = (config \ "inputTargetClass" \ "@uri").text
    val outputStartClass = vf.createURI((config \ "outputStartClass" \ "@uri").text)
    val outputAggregatedClass = vf.createURI((config \ "outputAggregatedClass" \ "@uri").text)

    val followProperties = (config \ "followProperties" \ "property").map(
          (n : XMLNode) =>
            (n \ "@uri").text
          )
    val collectProperties = (config \ "collectProperties" \ "property").map(
      (n : XMLNode) =>
        (n \ "@uri").text
      )

    val blockIdProperty = "http://thisSubject"
    val instanceRelation = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
    val instanceURI = vf.createURI(instanceRelation)

  def process(i : List[Quad], subject: String) : List[Quad] = {
    val mm = MemoryModel.fromQuads(i)
    if(i.size > 0){    
        val language = WiktionaryPageExtractor.langObj
        val dataset = WiktionaryPageExtractor.datasetURI
        val context = WiktionaryPageExtractor.tripleContext
        val subjectURI = vf.createURI(subject)
        val blocks = Collector.collect(mm, subject, MMap(), this)
        val newQuads = new ListBuffer[Quad]()
        newQuads append new Quad(language, dataset, subjectURI, instanceURI, outputStartClass, context)
        for(block <- blocks){
          val blockId = vf.createURI(block(blockIdProperty).head.asInstanceOf[Literal].stringValue)
          newQuads append new Quad(language, dataset, subjectURI, blockProperty, blockId, context)
          newQuads append new Quad(language, dataset, blockId, instanceURI, outputAggregatedClass, context)
          for(property <- block.keySet){
            val propertyURI = vf.createURI(property)
            for(value <- block(property)){
              if(collectProperties.contains(property)){
                newQuads append new Quad(language, dataset, blockId, propertyURI, value, context)
              }
            }
          }
        }
        newQuads.appendAll(i) //keep orignal quads -> overlay
        newQuads.toList
    } else i
  }
}

object Collector  {
   
    def myContains(haystack:ListBuffer[Value], needle:String) : Boolean = {
        for(v <- haystack){
            if(v.stringValue.equals(needle)){
                return true
            }
        }
        false
    }
    
    def collect(mm : MemoryModel, cur : String, collected : MMap[String, ListBuffer[Value]], config : LemonOverlay) : ListBuffer[MMap[String, ListBuffer[Value]]] = {
        //println("inspect "+cur)
        val ret = new ListBuffer[MMap[String, ListBuffer[Value]]]()
        if(!mm.hasS(cur)){
            //println("no data")
            return ret
        }

        val properties = mm.getPO(cur)
        for(p <- properties.keySet){
            if(config.collectProperties.contains(p)){
                //println("collect property "+p)
                for(v <- properties(p)){
                    if(collected.contains(p)){
                        //println("value "+v)
                        collected(p).append(v)               
                    } else {
                        val values = new ListBuffer[Value]()
                        values.append(v)
                        //println("value "+v)
                        collected(p) = values
                    }
                }
            }
        }
        /*
        for(p <- properties.keySet){
            println("property "+p)
            for(v <- properties(p)){
                println("value "+v)
            }
        }*/

        if(properties.contains(config.instanceRelation) && myContains(properties(config.instanceRelation), config.inputTargetClass)){
            //end condition for recursion
            //println("end condition reached")
            val values = new ListBuffer[Value]()
            values.append(config.vf.createLiteral(cur))
            collected(config.blockIdProperty) = values
            ret.append(collected)
            return ret
        } else {
            val thisFollowProperties = properties.keySet.intersect(config.followProperties.toSet)
            if(thisFollowProperties.size == 0) {
                //nothing to follow here -> make this a Sense too, even if its a stub 
                val values = new ListBuffer[Value]()
                values.append(config.vf.createLiteral(cur))
                collected(config.blockIdProperty) = values
                ret.append(collected)
            } else {
                for(p <- thisFollowProperties){
                    for(v <- properties(p)){
                        if(v.isInstanceOf[Resource]){
                            //recursion
                            ret.appendAll(collect(mm, v.toString, collected.clone, config))
                        }
                    }
                }
            }
            return ret
        }
    }
}

class MemoryModel (val d : MMap[String, MMap[String, ListBuffer[Value]]]) {
  def hasS(s:String) = d.contains(s)
  def getPO(s:String) = d(s)
  def getO(s:String, p:String) = d(s)(p)
  def getQuads(langObj:Language, datasetURI:Dataset, tripleContext:Resource) : List[Quad] = {
    val quads = new ListBuffer[Quad]()
println(d.keySet)
    for(s <- d.keySet){
        for(p <- d(s).keySet){
            for(o <- d(s)(p)){
                quads.append(new Quad(langObj, datasetURI, ValueFactoryImpl.getInstance.createURI(s), ValueFactoryImpl.getInstance.createURI(p), o, tripleContext))
            }
        }
    }
    quads.toList
  }
}

object MemoryModel {
  def getValue(q:Quad) : Value = if(q.datatype == null){ValueFactoryImpl.getInstance.createURI(q.value)} else {ValueFactoryImpl.getInstance.createLiteral(q.value)}
  
  def fromQuads(g:List[Quad]) : MemoryModel = {
    val d = MMap[String, MMap[String, ListBuffer[Value]]]()
    for(q <- g){
      if(d.contains(q.subject)){
        val s = d(q.subject)
        if(s.contains(q.predicate)){
          s(q.predicate).append(MemoryModel.getValue(q))
        } else {
          val values = new ListBuffer[Value]()
          values.append(MemoryModel.getValue(q))
          s(q.predicate) = values
        }
      } else {
        val values = new ListBuffer[Value]()
        values.append(MemoryModel.getValue(q))
        d(q.subject) = MMap(q.predicate -> values)
      }
    }
    new MemoryModel( d)
  }
}
