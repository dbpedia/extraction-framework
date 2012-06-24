package org.dbpedia.extraction.wikiparser.impl.sweble

import java.io.StringWriter
import java.net.URI
import java.util.ArrayList

import scala.collection.JavaConversions._
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

import de.fau.cs.osr.ptk.common.ast.AstNode
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
		
    def apply(page : WikiPage) : PageNode =
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
        //p.go(cp.getPage())
		//println(w.toString())
    
        //TODO dont transform, refactor all usages instead
        transformAST(page, false, false, parsed)
    }

    def parse(pageId : PageId, wikitext : String) : Page = {
        // Compile the retrieved page
		compiler.postprocess(pageId, wikitext, null).getPage
    }

    def transformAST(page: WikiPage, isRedirect : Boolean, isDisambiguation : Boolean, swebleTree : Page) : PageNode = {
        //merge fragmented Text nodes
        new AstCompressor().go(swebleTree)
        //transform sweble nodes to DBpedia nodes
        val nodes = transformNodes(swebleTree.getContent)
        //merge fragmented TextNodes (again... this time the DBpedia nodes)
        val nodesClean = mergeConsecutiveTextNodes(nodes)
        new PageNode(page.title, page.id, page.revision, isRedirect, isDisambiguation, nodesClean)
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
                nl append new SectionNode(s.getTitle, s.getLevel, transformNodes(s.getTitle), line)
                // add the children of this section
                // makes it flat
                nl appendAll transformNodes(s.iterator.toList.tail) //first NodeList is the section title again
                nl.toList
            }
            case p : Paragraph => transformNodes(p.getContent)
            case t : Template => {
                var i = -1
                val properties = t.getArgs.iterator.toList.map( (n:AstNode) => {
                    i = i+1
                    n match {
                        case ta : TemplateArgument => new PropertyNode(if(ta.getHasName){ta.getName} else {i.toString}, transformNodes(parse(pageId, PreprocessorToParserTransformer.transform(new LazyPreprocessedPage(ta.getValue, new ArrayList[Warning]()), new EntityMap()).getWikitext()).getContent), line) 
                        case _ => throw new Exception("expected TemplateArgument as template child")
                    }
                })
                val name : String = t.getName //implicit conversion
                val title = new WikiTitle(WikiUtil.cleanSpace(name), WikiTitle.Namespace.Template)
                List(new TemplateNode(title, properties, line, transformNodes(t.getName)))
            }
            case tn : Text => List(new TextNode(tn.getContent, line))
            case ws : Whitespace => List(new TextNode(ws.getContent.get(0).asInstanceOf[Text].getContent, line)) //as text
            case il : InternalLink => {
                val destinationURL = WikiTitle.parse(il.getTarget(), language)
                val destinationNodes = List[Node](new TextNode(il.getTarget, line)) //parsing of target not yet supported
                val titleNodes = transformNodes(il.getTitle.getContent) 
                if (destinationURL.language == language) {
                    List(new InternalLinkNode(destinationURL, titleNodes, line, destinationNodes))
                } else {
                    List(new InterWikiLinkNode(destinationURL, titleNodes, line, destinationNodes))
                }
            }
            case el : ExternalLink => {
                val destinationURL = new URI(el.getTarget)
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
                    case item : ItemizationItem => transformNodes(item.getContent)
                    case _ => throw new Exception("expected ItemizationItem as Itemization child")
                }
            }).foldLeft(ListBuffer[Node]())( (lb : ListBuffer[Node], nodes : List[Node]) => {lb add new TextNode("*", line); lb addAll nodes; lb add new TextNode("\n", line); lb} ).toList
            case items : Enumeration => items.getContent.iterator.toList.map( (n:AstNode) => {
                n match {   
                    case item : EnumerationItem => transformNodes(item.getContent)
                    case _ => throw new Exception("expected EnumerationItem as Enumeration child")
                }
            }).foldLeft(ListBuffer[Node]())( (lb : ListBuffer[Node], nodes : List[Node]) => {lb add new TextNode("#", line); lb addAll nodes; lb add new TextNode("\n", line); lb} ).toList
            case b : Bold => transformNodes(b.getContent) // ignore style
            case i : Italics => transformNodes(i.getContent) // ignore style
            case tplParam : TemplateParameter => List(new TemplateParameterNode(tplParam.getName, tplParam.getDefaultValue != null, line))
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
                        val cells = tr.getBody.iterator.toList.map((rowElement:AstNode) => {
                            new TableCellNode(transformNodes(rowElement.iterator.toList), line)
                        })
                        new TableRowNode(cells, line)
                    })
                List(new TableNode(caption, rows, line))
            }
            //else
            case _ => List(new TextNode("else:"+node.getClass.getName+": "+node.toString, line))
        }
    }

    implicit def nodeList2humanString(nl:NodeList) : String = {
        transformNodes(nl).map( (n:Node)=>
            n match {
                case sn : TextNode => sn.text 
                case _ => ""
            }
        ).mkString
    }

    implicit def url2string(url:Url) : String = {
        url.getProtocol +":"+ url.getPath
    }

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
}


