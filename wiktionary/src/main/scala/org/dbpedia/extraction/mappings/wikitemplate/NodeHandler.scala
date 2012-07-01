package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.mappings.Cache
import collection.mutable.Stack
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.destinations.Quad
import xml.{XML, Node => XMLNode, NodeSeq}

trait NodeHandler {
    def process(n : Stack[Node], thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : Unit
   
    val vf = ValueFactoryImpl.getInstance
}

class SkipAny(config : NodeSeq) extends NodeHandler {
    def process(n : Stack[Node], thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : Unit = {
        n.pop //drop first
    }
}
