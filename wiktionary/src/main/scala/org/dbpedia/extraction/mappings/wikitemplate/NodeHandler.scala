package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.dbpedia.extraction.mappings.{WiktionaryPageExtractor, Cache}
import collection.mutable.Stack
import org.dbpedia.extraction.wikiparser.{TemplateNode, Node}
import collection.mutable.{ListBuffer}
import org.dbpedia.extraction.destinations.Quad
import xml.{XML, Node => XMLNode, NodeSeq}
import org.dbpedia.extraction.mappings.wikitemplate.MyNodeList._


trait NodeHandler {
    def process(n : Stack[Node], thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : NodeHandlerResult
   
    val vf = ValueFactoryImpl.getInstance
}

class InfoBoxMapper(config : NodeSeq) extends NodeHandler {
  val nodeName = (config \ "@tplname").text
  val mapping : Map[String, String] = (config \ "mapping" ).map(node => ((node \ "@label").text, (node \ "@uri").text ) ) toMap

  //val oTpl : ResultTemplate = Tpl.fromNode(n \ "oTemplate")

  def process(n : Stack[Node], thisBlockURI : String, cache : Cache, parameters : Map[String, String]) : NodeHandlerResult = {
    val node = n.pop
    if (!(node.isInstanceOf[TemplateNode] && node.asInstanceOf[TemplateNode].title.decoded == nodeName)){
      n.push(node)
      return new NodeHandlerNoResult
    } else {
      Console.println("found node")
      val mapped = node.asInstanceOf[TemplateNode].children.filter(prop => mapping.contains(prop.key)).map(prop => (mapping(prop.key), prop.children))
      val quads = new ListBuffer[Quad]()

      for( (name, value) <- mapped) {
        val obj = vf.createLiteral(value.toReadableString)
        val quad = new Quad(WiktionaryPageExtractor.langObj, WiktionaryPageExtractor.datasetURI, vf.createURI(thisBlockURI), vf.createURI(name), obj, WiktionaryPageExtractor.tripleContext)
        Console.println("->quad "+quad)
        quads += quad
      }
      return new NodeHandlerTriplesResult(quads.toList)
    }
  }
}

class NodeHandlerResult

class NodeHandlerNoResult extends NodeHandlerResult

class NodeHandlerVarBindingsResult(val varBindings : VarBindingsHierarchical) extends NodeHandlerResult {}
class NodeHandlerTriplesResult(val triples : List[Quad]) extends NodeHandlerResult { }
