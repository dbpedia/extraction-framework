package org.dbpedia.extraction.mappings.wikitemplate.wiktionary.postprocessor

import org.dbpedia.extraction.mappings.WiktionaryPageExtractor
import org.dbpedia.extraction.mappings.wikitemplate.PostProcessor
import xml.{Node => XMLNode, NodeSeq}
import org.dbpedia.extraction.destinations.{Quad, Dataset}
import org.dbpedia.extraction.util.Language

import org.openrdf.model.{Statement, Value, Resource, Literal}
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

  def process(i : List[Statement], subject: String) : List[Statement] = {
    val mm = MemoryModel.fromQuads(i)
    if(i.size > 0){
        val subjectURI = vf.createURI(subject)
        val blocks = Collector.collect(mm, subject, MMap(), this)
        val newQuads = new ListBuffer[Statement]()
        newQuads append vf.createStatement(subjectURI, instanceURI, outputStartClass)
        for(block <- blocks){
          val blockId = vf.createURI(block(blockIdProperty).head.toString)
          newQuads append vf.createStatement(subjectURI, blockProperty, blockId)
          newQuads append vf.createStatement(blockId, instanceURI, outputAggregatedClass)
          for(property <- block.keySet){
            val propertyURI = vf.createURI(property)
            for(value <- block(property)){
              if(collectProperties.contains(property)){
                newQuads append vf.createStatement(blockId, propertyURI, value)
              }
            }
          }
        }
        newQuads.appendAll(i) //keep original quads -> overlay
        newQuads.toList
    } else i
  }
}

object Collector  {
   
    def myContains(haystack:ListBuffer[Value], needle:String) : Boolean = {
        for(v <- haystack){
            if(v.toString.equals(needle)){
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
  val vf = WiktionaryPageExtractor.vf

  def getQuads() : List[Statement] = {
    val quads = new ListBuffer[Statement]()

    for(s <- d.keySet){
        for(p <- d(s).keySet){
            for(o <- d(s)(p)){
                quads.append(vf.createStatement(vf.createURI(s), vf.createURI(p), o))
            }
        }
    }
    quads.toList
  }
}

object MemoryModel {

  def fromQuads(g:List[Statement]) : MemoryModel = {
    val d = MMap[String, MMap[String, ListBuffer[Value]]]()
    for(q <- g){
      if(d.contains(q.getSubject.toString())){
        val s = d(q.getSubject.toString())
        if(s.contains(q.getPredicate.getURI())){
          s(q.getPredicate.getURI()).append(q.getObject)
        } else {
          val values = new ListBuffer[Value]()
          values.append(q.getObject)
          s(q.getPredicate.getURI()) = values
        }
      } else {
        val values = new ListBuffer[Value]()
        values.append(q.getObject)
        d(q.getSubject.toString()) = MMap(q.getPredicate.getURI() -> values)
      }
    }
    new MemoryModel( d)
  }
}
