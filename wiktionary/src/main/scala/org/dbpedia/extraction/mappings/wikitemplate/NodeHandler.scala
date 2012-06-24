package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.mappings.Cache
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.destinations.Quad
import xml.{XML, Node => XMLNode, NodeSeq}

trait NodeHandler {
    def process(n : Node, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : (List[Quad], Boolean)
   
    val vf = ValueFactoryImpl.getInstance
}

class SkipAny(config : NodeSeq) extends NodeHandler {
    def process(n : Node, thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : (List[Quad], Boolean) = {
        (List[Quad](), true)
    }
}
