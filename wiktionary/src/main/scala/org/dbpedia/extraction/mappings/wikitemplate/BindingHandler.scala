package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.mappings.{WiktionaryPageExtractor, Cache}
import collection.mutable.ListBuffer
import org.dbpedia.extraction.destinations.Quad
import org.openrdf.model.Statement

trait BindingHandler {
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
    def process(i : VarBindings, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : List[Statement]

    val vf = WiktionaryPageExtractor.vf
}
