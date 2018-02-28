package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.iri.UriUtils

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

class TemplatePropertyExpander(templateNode: TemplateNode) {

  private val redirectMap = templateNode.children.map(propertyNode =>{
    val childFunctions = propertyNode.children.map(c => redirectNodeFunction(c)_)
    //if all children are ampty text nodes, use the input template values
    if(propertyNode.children.isEmpty || ParserFunctions.testForEmptyNodeSeq(propertyNode.children))
      propertyNode.key -> ((t: TemplateNode) =>
        t.property(propertyNode.key).toList.flatMap(c => c.children))
    else //else resolve redirect
      propertyNode.key -> ((t: TemplateNode) =>
        childFunctions.flatMap(f => f(t)))
  }).toMap

  val templateTitle: WikiTitle = templateNode.title

  private def redirectNodeFunction(node: Node)(inputTemplate: TemplateNode): List[Node] ={
    node match{
      case pf: ParserFunctionNode => executeParserFunction(pf.title, pf.children.flatMap(redirectAndCleanParams(_)(inputTemplate)):_*)
      case tp: TemplateParameterNode => redirectTemplateParameter(tp)(inputTemplate)
      case ln: InternalLinkNode => List(expandInterLinkNode(ln)(inputTemplate))
      case ln: ExternalLinkNode => List(expandExternalLinkNode(ln)(inputTemplate))
      case no: Node  => List(no)
    }
  }

  def expandInterLinkNode(ln: InternalLinkNode)(inputTemplate: TemplateNode): LinkNode ={
    val destInationNodes = ln.destinationNodes.flatMap(n => redirectNodeFunction(n)(inputTemplate))
    val labelNodes = ln.children.flatMap(n => redirectNodeFunction(n)(inputTemplate))
    val newTitle = destInationNodes.map(_.toPlainText).mkString.trim
    val wt = WikiTitle.parse(newTitle, ln.destination.language)
    InternalLinkNode(wt, labelNodes, ln.line, destInationNodes)
  }

  def expandExternalLinkNode(ln: ExternalLinkNode)(inputTemplate: TemplateNode): LinkNode ={
    val destInationNodes = ln.destinationNodes.flatMap(n => redirectNodeFunction(n)(inputTemplate))
    val labelNodes = ln.children.flatMap(n => redirectNodeFunction(n)(inputTemplate))
    val newTitle = UriUtils.createURI(destInationNodes.map(_.toPlainText).mkString.trim) match{
      case Success(s) => s
      case Failure(f) => throw f
    }
    ExternalLinkNode(newTitle, labelNodes, ln.line, destInationNodes)
  }

  private def redirectAndCleanParams(n: Node)(inputTemplate: TemplateNode): Option[Node] ={
    val children = n match{
      case pfp: ParserFunctionParameterNode => List(ParserFunctionParameterNode(pfp.children.flatMap(redirectAndCleanParams(_)(inputTemplate)), pfp.line))
      case pn: PropertyNode => List(PropertyNode(pn.key, pn.children.flatMap(redirectAndCleanParams(_)(inputTemplate)), pn.line))
      case pf: ParserFunctionNode => executeParserFunction(pf.title, pf.children.flatMap(redirectAndCleanParams(_)(inputTemplate)):_*)
      case tp: TemplateParameterNode => redirectTemplateParameter(tp)(inputTemplate)
      case ln: InternalLinkNode => List(expandInterLinkNode(ln)(inputTemplate))
      case ln: ExternalLinkNode => List(expandExternalLinkNode(ln)(inputTemplate))
      case tn: TemplateNode => List(TemplateNode(tn.title,
        tn.children.flatMap(redirectAndCleanParams(_)(inputTemplate).map(x => x.asInstanceOf[PropertyNode])),
        tn.line, tn.titleParsed))
      case no: Node => if(no.children.isEmpty) List(no) else no.children
    }
    val list = children.flatMap(redirectNodeFunction(_)(inputTemplate))
    val res = filterEmptyPadNodes(list).headOption
    res
  }

  private def executeParserFunction(name: String, params: Node *): List[Node] ={
    name.trim match{
      //case "#expr" => params.headOption.map(p => ParserFunctions.exprFunc(p.toPlainText))
      case "#if" => ParserFunctions.ifFunc(params.head, params(1), if(params.size > 2) params(2) else null).toList
      //case "#invoke" => ParserFunctions.invokeFunc(params.head.toString, params(1).toString, params.drop(2):_*).toList
      case _ => List()
    }
  }

  private def redirectTemplateParameter(tp: TemplateParameterNode)(inputTemplate: TemplateNode): List[Node] ={
    //get children of redirected node, then add all alternatives
    val alternatives = List(inputTemplate.property(tp.parameter).toList.flatMap(_.children)) ++
      tp.children.map(redirectNodeFunction(_)(inputTemplate))
    alternatives.headOption.getOrElse(List())
  }

  private def filterEmptyPadNodes(list: List[Node]): List[Node] ={
    val buf = list.foldRight(new ListBuffer[Node])((n, l) => {
      var append = true
      if(l.isEmpty || ParserFunctions.testForEmptyNodeSeq(List(l.last)))
        if(ParserFunctions.testForEmptyNodeSeq(List(n)))
          append = false
      if(append)
        l.append(n)
      l
    })
    if(buf.isEmpty)
      buf.append(TextNode("\n", 0))
    buf.toList
  }

  def resolveProperty(property: String)(templateNode: TemplateNode): Option[PropertyNode] = {
    val templateChildren = templateNode.property(property) match{
      case Some(p) => p.children
      case None => List()
    }
    val children = filterEmptyPadNodes(
      redirectMap.get(property) match{
      case Some(pFunction) =>
        val zw = pFunction(templateNode)
        if(ParserFunctions.testForEmptyNodeSeq(zw))
          templateChildren
        else
          zw.reverse
      case None => templateChildren
    })
    templateNode.property(property) match{
      case Some(p) =>  Some(PropertyNode(p.key, children = children, p.line))
      case None if children.nonEmpty => Some(PropertyNode(property, children = children, 0))
      case None => None
    }
  }

  def resolveTemplate(template: TemplateNode): TemplateNode ={
    val propNames = (this.redirectMap.keys ++ template.children.map(c => c.key)).toList.distinct
    val properties = propNames.flatMap(p => this.resolveProperty(p)(template))
    TemplateNode(template.title, properties, template.line, template.titleParsed)
  }
}

object TemplatePropertyExpander{
  val defRed = new Redirects()
  def multilineTemplateFinder(validInfoboxes: Set[String]): (PageNode => Option[TemplateNode]) = (pn: PageNode) => {
      if(pn.title.namespace == Namespace.Template){
        pn match{
          case wp: WikiPage => SimpleWikiParser.apply(wp,defRed) match{
            case Some(p) =>
              // get all multiline templates
              val temps = p.containedTemplateNodes(validInfoboxes)
                .groupBy(t => t.children.map(c => c.line).max - t.line)
                .filter(x => x._1 > 0)
              if(temps.nonEmpty)
                temps.maxBy(x => x._1)._2.headOption
              else
                None
            case None => None
          }
          case _ => None
        }
      }
      else
        None
    }
}