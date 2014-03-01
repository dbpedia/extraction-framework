package org.dbpedia.extraction.wikiparser.impl.sweble

import java.io.StringWriter
import java.net.URI
import java.util.ArrayList

import scala.collection.JavaConversions._
import scala.language.implicitConversions
import collection.mutable.{ListBuffer}

import org.sweble.wikitext.engine.CompiledPage
import org.sweble.wikitext.engine.Compiler
import org.sweble.wikitext.engine.CompilerException
import org.sweble.wikitext.engine.Page
import org.sweble.wikitext.engine.PageId
import org.sweble.wikitext.engine.PageTitle
import org.sweble.wikitext.engine.utils.SimpleWikiConfiguration
import org.sweble.wikitext.`lazy`.utils.AstPrinter
import org.sweble.wikitext.`lazy`.parser._
import org.sweble.wikitext.`lazy`.preprocessor._
import org.sweble.wikitext.`lazy`.postprocessor.AstCompressor
import org.sweble.wikitext.`lazy`.encval.IllegalCodePoint
import org.sweble.wikitext.`lazy`.AstNodeTypes
import org.sweble.wikitext.`lazy`.utils.XmlAttribute

import de.fau.cs.osr.ptk.common.ast.ContentNode
import de.fau.cs.osr.ptk.common.ast.AstNode
import de.fau.cs.osr.ptk.common.AstVisitor
import de.fau.cs.osr.ptk.common.ast.NodeList
import de.fau.cs.osr.ptk.common.ast.Text
import de.fau.cs.osr.ptk.common.ast.StringContentNode
import de.fau.cs.osr.ptk.common.Warning
import de.fau.cs.osr.ptk.common.EntityMap
//import de.fau.cs.osr.ptk.nodegen.parser._


import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.Language


/**
 * this class transforms a Sweble AST to DBpedia AST.
 * the implementation is dirty
 */
final class SwebleWrapper extends WikiParser
{
    var lastLine = 0
    var language : Language = null
    var pageId : PageId = null

    // Set-up a simple wiki configuration
	val config = new SimpleWikiConfiguration(
		"classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml"
    )
	
	// Instantiate a compiler for wiki pages
	val compiler = new Compiler(config)
    var entityMap : EntityMap = new EntityMap()

    def apply(page : WikiPage) : Option[PageNode] =
    {
        //TODO refactor, not safe
        language = page.title.language
        
		// Retrieve a page
		val pageTitle = PageTitle.make(config, page.title.decodedWithNamespace)
		
		pageId = new PageId(pageTitle, page.id)
		
		val wikitext : String = page.source
		
		val parsed = parse(pageId, wikitext)

        //print sweble AST for debugging
        //val w = new StringWriter()
		//val p = new AstPrinter(w)
        //p.go(parsed)
		//println(w.toString())
    
        //TODO dont transform, refactor all extractors instead
        Some(transformAST(page, false, false, parsed))
    }

    def parse(pageId : PageId, wikitext : String) : Page = {
        // Compile the retrieved page
		val cp = compiler.postprocess(pageId, wikitext, null)
        if(cp.getEntityMap != null)
            entityMap = cp.getEntityMap
        //println("after parsing "+entityMap)
        cp.getPage
    }


    def transformAST(page: WikiPage, isRedirect : Boolean, isDisambiguation : Boolean, swebleTree : Page) : PageNode = {
        //merge fragmented Text nodes
        new AstCompressor().go(swebleTree)
        //parse template arguments
         new ParameterToDefaultValueResolver(pageId).go(swebleTree)
        //transform sweble nodes to DBpedia nodes
        val nodes = transformNodes(swebleTree.getContent)
        //merge fragmented TextNodes (again... this time the DBpedia nodes)
        val nodesClean = mergeConsecutiveTextNodes(nodes)
        //println(nodesClean)
        new PageNode(page.title, page.id, page.revision, page.timestamp,
                page.contributorID,  page.contributorName, isRedirect, isDisambiguation, nodesClean)
    }

    def transformNodes(nl : NodeList) : List[Node] = {
        transformNodes(nl.iterator.toList)
    }

    def transformNodes(nodes : List[AstNode]) : List[Node] = {
        nodes.map( (node : AstNode) => 
            node match {
                case nl : NodeList => transformNodes(nl.listIterator.toList)
                case n : AstNode =>  transformNode(n)
            }
        ).foldLeft(ListBuffer[Node]())( (lb : ListBuffer[Node], nodes : List[Node]) => {lb addAll nodes; lb} ).toList
    }

    def transformNode(node : AstNode) : List[Node] = {
        val line = if(node.getNativeLocation == null){lastLine} else {node.getNativeLocation.getLine}
        lastLine = line
        node match {
            case s : Section => {
                val nl = ListBuffer[Node]()
                nl append new SectionNode(s.getTitle, s.getLevel, transformNodes(new NodeList(s.iterator.toList.head)), line)
                // add the children of this section
                // makes it flat
                nl appendAll transformNodes(s.iterator.toList.tail) //first NodeList is the section title again
                nl.toList
            }
            case p : Paragraph => transformNodes(p.getContent)
            case t : Template => {
                var i = 0
                val properties = t.getArgs.iterator.toList.map( (n:AstNode) => {
                    i = i+1
                    n match {
                        case ta : TemplateArgument => new PropertyNode(if(ta.getHasName){ta.getName} else {i.toString}, transformNodes(ta.getValue), line) 
                        case _ => throw new Exception("expected TemplateArgument as template child")
                    }
                })
                val name : String = nodeList2string(t.getName) 
                val nameClean = WikiUtil.cleanSpace(name)
                val title = new WikiTitle(nameClean, Namespace.Template, language)
                val tpl = TemplateNode.transform(new TemplateNode(title, properties, line, transformNodes(t.getName)))
                tpl
            }
            case tn : Text => List(new TextNode(tn.getContent, line))
            case ws : Whitespace => List(new TextNode(ws.getContent.get(0).asInstanceOf[Text].getContent, line)) //as text
            case il : InternalLink => {
                val target = il.getTarget()
                val target2 = if(target != null && !target.equals("")){ if(target.startsWith("#")) target.substring(1) else target} else "none"
                val destinationURL = WikiTitle.parse(target2, language)

                val destinationNodes = List[Node](new TextNode(il.getTarget, line)) //parsing of target not yet supported
                val titleNodes = if(!il.getTitle.getContent.isEmpty){transformNodes(il.getTitle.getContent)} else {List(new TextNode(il.getTarget, line))}

                val postfix : List[Node] = if(il.getPostfix != null && il.getPostfix != ""){
                    List(new TextNode(il.getPostfix, line))
                } else List()

                if (destinationURL.language == language) {
                    List(new InternalLinkNode(destinationURL, titleNodes, line, destinationNodes)) ++ postfix
                } else {
                    List(new InterWikiLinkNode(destinationURL, titleNodes, line, destinationNodes)) ++ postfix
                }
            }
            case el : ExternalLink => {
              var destinationURL : URI = null
              try {
                destinationURL = new URI(el.getTarget)
              } catch {
                case e : Exception => destinationURL = new URI("http://example.org")
              }
              val destinationNodes = List[Node](new TextNode(el.getTarget, line)) //parsing of target not yet supported
              val titleNodes = transformNodes(el.getTitle)
              List(new ExternalLinkNode(destinationURL, titleNodes, line, destinationNodes))
            }
            case url : Url => {
                val destinationURL = new URI(url)
                val destinationNodes = List[Node](new TextNode(url, line)) //parsing of target not yet supported
                List(new ExternalLinkNode(destinationURL, destinationNodes, line, destinationNodes))
            }
            case items : Itemization => items.getContent.iterator.toList.map( (n:AstNode) => {
                n match {   
                    case item : ItemizationItem => wrap(item, line)
                    case item : Text => List(new TextNode(item.getContent, line))
                    case _ => throw new Exception("expected ItemizationItem as Itemization child")
                }
            }).flatten
            case items : Enumeration => {
                var i = 0
                items.getContent.iterator.toList.map( (n:AstNode) => {
                //TODO a EnumerationItem can contain Itemization - needs to be emitted as #* 
                n match {   
                    case item : EnumerationItem => wrap(item, line)
                    case _ => throw new Exception("expected EnumerationItem as Enumeration child. found "+n.getClass.getName+" instead")
                }
            }).foldLeft(ListBuffer[Node]())( (lb : ListBuffer[Node], nodes : List[Node]) => {
                i = i+1; 
                lb add nodes.head; //the # symbol hopefully :)
                lb add new TemplateNode(
                    new WikiTitle("enum-expanded", Namespace.Template, language), 
                    List(new PropertyNode("1", List(new TextNode(i.toString, line)), line)), line, List(new TextNode("enum-expanded", line)));
                lb addAll nodes.tail; 
                lb} ).toList
            }
            case definitions : DefinitionList => definitions.getContent.iterator.toList.map( (n:AstNode) => {
                n match {   
                    case item : DefinitionDefinition => wrap(item, line)
                    case item : DefinitionTerm => wrap(item, line)
                    case a : AstNode =>  transformNodes(List(a))//throw new Exception("expected DefinitionDefinition as DefinitionList child. found "+n.getClass.getName+" instead")
                }
            }).flatten
            case b : Bold => transformNodes(b.getContent) // ignore style
            case i : Italics => transformNodes(i.getContent) // ignore style
            case tplParam : TemplateParameter => {
              if(tplParam != null && tplParam.getDefaultValue != null)
                List(new TemplateParameterNode(tplParam.getName, transformNodes(tplParam.getDefaultValue.getValue()), line))
              else List[Node]()
            }
            /*case pf : ParserFunctionBase => {
                val title = new WikiTitle(pf.getName + ":", WikiTitle.Namespace.Template, language)
                val children = List[Node]() //not implemented yet
                List(new ParserFunctionNode(title, children, line))
            }*/
            case t : Table => {
                val captionNodeOption = t.getBody.iterator.toList.find( (n:AstNode) => n.isInstanceOf[TableCaption])
                val caption : Option[String] = if(captionNodeOption.isDefined){Some(captionNodeOption.get.asInstanceOf[TableCaption].getBody)} else {None}
                val rows = t.getBody.iterator.toList.
                    filter((n:AstNode) => n.isInstanceOf[TableRow]).
                    map((n:AstNode) => {
                        val tr = n.asInstanceOf[TableRow]
                        val cells = tr.getBody.iterator.toList.map((re:AstNode) => { 
                            re match { 
                                case tableCell:TableCell => {
                                    Some(new TableCellNode(transformNodes(tableCell.iterator.toList), line, getXMLAttribute(tableCell, "rowspan").getOrElse("1").toInt, getXMLAttribute(tableCell, "colspan").getOrElse("1").toInt))
                                }
                                case _ => None
                            }
}).filter(_.isDefined).map(_.get)
                        new TableRowNode(cells, line)
                    })
                List(new TableNode(caption, rows, line))
            }
            case icp : IllegalCodePoint => List(new TextNode(icp.getCodePoint(), line))
            case xml : XmlElement => List() //drop xml nodes - are they needed?
            case cn : ContentNode => transformNodes(cn.getContent)
            //else
            case _ => List(new TextNode(/*"else:"+node.getClass.getName+": "+*/nodeList2string(new NodeList(node)), line))
        }
    }

    implicit def nodeList2prepocessedPage(nl:NodeList) : PreprocessedWikitext = {
        //println(entityMap)
        val t = PreprocessorToParserTransformer.transform(new LazyPreprocessedPage(nl, new ArrayList[Warning]()), entityMap)
        t
    }

    implicit def preprocessedPage2string(p:PreprocessedWikitext) : String = {
        val t = p.getWikitext()
        t
    }

    implicit def nodeList2string(nl:NodeList) : String = {
        preprocessedPage2string(nodeList2prepocessedPage(nl))
    }

    implicit def url2string(url:Url) : String = {
        url.getProtocol +":"+ url.getPath
    }

    implicit def nodes2nodeList(l : List[AstNode]) : NodeList = new NodeList(l.toSeq)
    implicit def nodeList2nodes(l : NodeList) : List[AstNode] = l.iterator.toList

    def mergeConsecutiveTextNodes(nodes:List[Node]):List[Node] = {
        val ret = new ListBuffer[Node]()
        nodes.indices.foreach( (i:Int) => {
            if(nodes(i).isInstanceOf[TextNode] && !ret.isEmpty && ret.last.isInstanceOf[TextNode]){
                val last = ret.remove(ret.size - 1)
                ret.append(last.asInstanceOf[TextNode].copy(text=last.asInstanceOf[TextNode].text+nodes(i).asInstanceOf[TextNode].text))
            } else {
                //recurse
                if(nodes(i).children.size > 0){
                    val newChildren = mergeConsecutiveTextNodes(nodes(i).children)
                    nodes(i) match {
                        case t : TemplateNode => ret.append(t.copy(children=newChildren.asInstanceOf[List[PropertyNode]]))
                        case s : SectionNode => ret.append(s.copy(children=newChildren))
                        case t : TableNode => ret.append(t.copy(children=newChildren.asInstanceOf[List[TableRowNode]]))
                        case iln : InternalLinkNode => ret.append(iln.copy(children=newChildren))
                        case eln : ExternalLinkNode => ret.append(eln.copy(children=newChildren))
                        case iwln : InterWikiLinkNode => ret.append(iwln.copy(children=newChildren))
                        case p : PropertyNode => ret.append(p.copy(children=newChildren))
                        case n : Node =>  ret.append(n) //unmodified
                    }

                } else {
                    ret.append(nodes(i))
                }
            }
        })
        ret.toList
    }

    class ParameterToDefaultValueResolver(pageId : PageId) extends AstVisitor {

		var entityMap : EntityMap = new EntityMap()
		
		var warnings : java.util.List[Warning] = new ArrayList[Warning]()
		
		def visit(n : AstNode) : AstNode =
		{
			mapInPlace(n)
			return n
		}
		
		def  visit(n : CompiledPage) : AstNode =
		{
			this.warnings = n.getWarnings()
			this.entityMap = n.getEntityMap()
			mapInPlace(n)
			return n
		}
		
		def visit(n : TemplateParameter) : AstNode = 
		{
			val defValArg = n.getDefaultValue()
			if (defValArg == null)
				return n
			
			val defVal = defValArg.getValue();
			
			// Shortcut for all those empty default values
			if (defVal.isEmpty())
				return defValArg
			
			val pprAst = new LazyPreprocessedPage(
					defVal, warnings, entityMap
            )
			
			val parsed = compiler.postprocessPpOrExpAst(pageId, pprAst);
			
			val content = parsed.getPage().getContent()
			
			// The parser of course thinks that the given wikitext is a 
			// individual page and will wrap even single line text into a 
			// paragraph node. We try to catch at least simple cases to improve
			// the resulting AST
			val contentClean = if (content.size() == 1 && content.get(0).getNodeType() == AstNodeTypes.NT_PARAGRAPH)    
				content.get(0).asInstanceOf[Paragraph].getContent()
            else content
			
			content
		}
		def visit(n : TemplateArgument) : AstNode = 
		{
			val value = n.getValue()
			if (value == null)
				return n
		
			
			// Shortcut for all those empty default values
			if (value.isEmpty())
				return n
			
			val pprAst = new LazyPreprocessedPage(
					value, warnings, entityMap
            )
			
			val parsed = compiler.postprocessPpOrExpAst(pageId, pprAst);
			
			val content = parsed.getPage().getContent()
			
			// The parser of course thinks that the given wikitext is a 
			// individual page and will wrap even single line text into a 
			// paragraph node. We try to catch at least simple cases to improve
			// the resulting AST
			val contentClean = if (content.size() == 1 && content.get(0).getNodeType() == AstNodeTypes.NT_PARAGRAPH)    
				content.get(0).asInstanceOf[Paragraph].getContent()
            else content
			
			n.setValue(contentClean)
            n
		}
	}

    def getXMLAttribute(node : TableCell, name : String) : Option[String] = {
        val xmlAttrs = node.getXmlAttributes()
        xmlAttrs.foreach((an:AstNode) => { 
            an match { 
                case (xmlAttr : XmlAttribute) => {
                    if(xmlAttr.getName == name && xmlAttr.getHasValue()){
                        return Some(nodeList2string(xmlAttr.getValue))
                    }
                }
                case _ =>
            }
        })
        None
    }

    def getWrap(node : AstNode) : Tuple2[String, String]= {
        val rtd = node.getAttribute("RTD").asInstanceOf[RtData]

        val start = if(rtd != null && rtd.getRts() != null){
                        val rts = rtd.getRts()
                        if(rts.length > 0  && rts(0) != null && rts(0).length > 0){
                            rts(0)(0).asInstanceOf[String]
                        } else defaultStart(node)
                    } else defaultStart(node)


        val end = if(rtd != null && rtd.getRts() != null){
                        val rts = rtd.getRts()
                        if(rts.length > 1 && rts(1) != null && rts(1).length > 0){
                            rtd.getRts()(1)(0).asInstanceOf[String]
                        } else ""
                    } else ""

        (start, end)
    }

    def defaultStart(node : AstNode) : String = node match {
        case i : ItemizationItem => "*"
        case i : EnumerationItem => "#"
        case i : DefinitionDefinition => ":"
        case i : DefinitionTerm => ":"
        case _ => ""
    }

    def wrap(node : ContentNode, line : Int) : List[Node] = {
        val w = getWrap(node)
        var hasExtraNL = false
        val c = node.getContent.map((a:AstNode)=>{if(a.isInstanceOf[Enumeration] || a.isInstanceOf[Itemization] || a.isInstanceOf[DefinitionList]){hasExtraNL = true; List(new Text("\n"), a)} else {List(a)}}).flatten
        List(new TextNode(w._1, line)) ++ transformNodes(c) ++ (if(!hasExtraNL){List(new TextNode(w._2, line))} else {List()})
    }
}


