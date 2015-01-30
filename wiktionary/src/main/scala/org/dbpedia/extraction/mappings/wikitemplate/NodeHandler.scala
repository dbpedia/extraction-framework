package org.dbpedia.extraction.mappings.wikitemplate

import org.openrdf.model.impl.ValueFactoryImpl
import org.openrdf.model.Statement
import org.dbpedia.extraction.mappings.{WiktionaryPageExtractor, Cache}
import collection.mutable.Stack
import org.dbpedia.extraction.wikiparser.{TemplateNode, TextNode, Node}
import collection.mutable.{ListBuffer}
import org.dbpedia.extraction.destinations.Quad
import xml.{XML, Node => XMLNode, NodeSeq}
import org.dbpedia.extraction.mappings.wikitemplate.MyNodeList._
import collection.mutable
import scala.language.reflectiveCalls
import scala.language.postfixOps



trait NodeHandler {
    def process(n : Stack[Node], thisBlockUri : String, cache : Cache, parameters : Map[String, String], block : Block) : NodeHandlerResult
   
    val vf = WiktionaryPageExtractor.vf
}

class InfoBoxMapper(config : NodeSeq) extends NodeHandler {
  val nodeName = (config \ "@tplname").text
  val mapping : Map[String, String] = (config \ "mapping" ).map(node => ((node \ "@label").text, (node \ "@uri").text ) ) toMap

  val templates = (config \ "resultTemplates" \ "resultTemplate").map(
    rtn => new ResultTemplate(
      (rtn \ "triples" \ "triple").map(
        t => new TripleTemplate(t)
      )
    ))

  def process(n : Stack[Node], thisBlockUri : String, cache : Cache, parameters : Map[String, String], block : Block) : NodeHandlerResult = {
    val node = n.pop
    if (!(node.isInstanceOf[TemplateNode] && node.asInstanceOf[TemplateNode].title.decoded == nodeName)){
      n.push(node)
      return new NodeHandlerNoResult
    } else {

      val mapped = node.asInstanceOf[TemplateNode].children.filter(prop => mapping.contains(prop.key)).map(prop => (mapping(prop.key), prop.children))

      val varBindings = new VarBindings
      for( (name, value) <- mapped) {
        val map = new mutable.HashMap[String, List[Node]]
        map += "predicate"  -> List(new TextNode(name, 0))
        map += "value" -> value
        varBindings += map
      }

      var tpl = new Tpl("nodehandlerdummy", new Stack(), None, templates)
      try{
      val quads = WiktionaryPageExtractor.handleFlatBindings(varBindings, block, tpl, cache, thisBlockUri)
      return new NodeHandlerTriplesResult(quads.toList)
      } catch {
        case e : Exception => return new NodeHandlerNoResult
      }
    }
  }
}

class NodeHandlerResult

class NodeHandlerNoResult extends NodeHandlerResult

class NodeHandlerTriplesResult(val triples : List[Statement]) extends NodeHandlerResult { }
