package org.dbpedia.extraction.wikiparser.impl.sweble

import java.util.ArrayList

import de.fau.cs.osr.ptk.common.ast.RtData
import de.fau.cs.osr.ptk.common.{AstVisitor, Warning}
import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.iri.UriUtils
import org.sweble.wikitext.engine._
import org.sweble.wikitext.engine.config.WikiConfigImpl
import org.sweble.wikitext.engine.nodes.{EngPage, EngProcessedPage}
import org.sweble.wikitext.engine.utils.DefaultConfigEnWp
import org.sweble.wikitext.parser.nodes.WtNodeList.WtNodeListImpl
import org.sweble.wikitext.parser.nodes._
import org.sweble.wikitext.parser.parser.PreprocessorToParserTransformer
import org.sweble.wikitext.parser.preprocessor.PreprocessedWikitext
import org.sweble.wikitext.parser.{WtEntityMap, WtEntityMapImpl}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.language.implicitConversions
import scala.util.{Failure, Success}


import org.dbpedia.extraction.util.{Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._


/**
  * this class transforms a Sweble AST to DBpedia AST.
  * the implementation is dirty
  */
final class SwebleWrapper extends WikiParser
{
    var lastLine = 0
    var language : Language = _
    var pageId : PageId = _

    // Set-up a simple wiki configuration
    var config: WikiConfigImpl = DefaultConfigEnWp.generate()
    /*var config1 = new SimpleWikiConfiguration(
        "classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml");
    var configFile = new File("classpath:/org/sweble/wikitext/engine/SimpleWikiConfiguration.xml")
    var wikiConfig: WikiConfigImpl =  WikiConfigImpl.load(configFile)
*/

    // Instantiate a compiler for wiki pages
    var engine = new WtEngineImpl(config)
    var wikiNodeFactory = engine.getWikiConfig.getParserConfig.getNodeFactory
    var entityMap : WtEntityMap = new WtEntityMapImpl()

    def apply(page : WikiPage, templateRedirects: Redirects = new Redirects(Map())) : Option[PageNode] =
    {
        //TODO refactor, not safe
        language = page.title.language
        lastLine = 0
        // Retrieve a page
        val pageTitle = PageTitle.make(config, page.title.decodedWithNamespace)

        pageId = new PageId(pageTitle, page.id)

        val wikitext : String = page.source

        val parsed = parse(pageId, wikitext)


        //TODO dont transform, refactor all extractors instead
        Some(transformAST(page, parsed))
    }

    def parse(pageId : PageId, wikitext : String) : EngPage = {
        // Compile the retrieved page
        var cp: EngProcessedPage = engine.postprocess(pageId, wikitext, null);
        if(cp.getEntityMap != null)
            entityMap = cp.getEntityMap
        cp.getPage
    }

    // The start_line param is for cases when I am rerunning the parser on a subtext and hence need
    // to adjust for the line numbers
    def transformAST(page: WikiPage, swebleTree : EngPage, start_line : Int = 0) : PageNode = {
        //parse template arguments

       // new ParameterToDefaultValueResolver(pageId).go(swebleTree)
        //transform sweble nodes to DBpedia nodes
        val nodes = transformNodes(swebleTree, start_line)
        //merge fragmented TextNodes (again... this time the DBpedia nodes)
        val nodesClean = mergeConsecutiveTextNodes(nodes)
        //println(nodesClean)
        new PageNode(page.title, page.id, page.revision, page.timestamp,
            page.contributorID,  page.contributorName, page.source, nodesClean)
    }

    def transformNodes(nl : WtNodeListImpl, start_line : Int) : List[Node] = {
        transformNodes(nl.iterator.toList, start_line)
    }

    // Extract Italic and Bold nodes and convert them to wikiformat
    // Also handles nested cases
    def transformFormattingNodes(nodes : List[WtNode]) : List[WtNode] = {
        nodes.map( (node : WtNode) =>
            node match {
                case n: WtItalics => {
                    if( n.get(0).isInstanceOf[WtText]){
                        var content = ""
                        for (i <- 0 to n.iterator().toList.length - 1) {
                            content = content + n.get(i).asInstanceOf[WtText].getContent
                        }
                        wikiNodeFactory.text("''" + content + "''")
                    } else if(n.get(0).isInstanceOf[WtBold]){
                        var content = ""
                        for (i <- 0 to n.get(0).asInstanceOf[WtBold].iterator().toList.length - 1 ) {
                            content = content + n.get(0).asInstanceOf[WtBold].get(i).asInstanceOf[WtText].getContent
                        }
                        wikiNodeFactory.text("'''''" + content + "'''''")

                    } else {
                        n
                    }
                }
                case n: WtBold => {
                    if( n.get(0).isInstanceOf[WtText]){
                        var content = ""
                        for (i <- 0 to n.iterator().toList.length - 1) {
                            if(n.get(i).isInstanceOf[WtText]) {
                                content = content + n.get(i).asInstanceOf[WtText].getContent
                            }
                        }
                        wikiNodeFactory.text("'''" + content + "'''")
                    } else if(n.get(0).isInstanceOf[WtItalics]){
                        var content = ""
                        for (i <- 0 to n.get(0).asInstanceOf[WtItalics].iterator().toList.length - 1) {
                            content = content + n.get(0).asInstanceOf[WtItalics].get(i).asInstanceOf[WtText].getContent
                        }
                        wikiNodeFactory.text("'''''" + content + "'''''")
                    } else{
                        n
                    }
                }
                case _ => node
            })
    }
    def transformNodes(nodes : List[WtNode], start_line : Int) : List[Node] = {

        val nodesTransformed = transformFormattingNodes(nodes)

        nodesTransformed.map( (node : WtNode) =>
            node match {
                case nl : WtNodeListImpl => transformNodes(nl.listIterator.toList, start_line)
                case n : WtNode => {
                    transformNode(n, start_line)
                }
            }
        ).foldLeft(ListBuffer[Node]())( (lb : ListBuffer[Node], nodes : List[Node]) => {lb addAll nodes; lb} ).toList
    }

    def getProperties( startingKey : Int , t :WtTemplate, line :Int , start_line : Int): List[PropertyNode] ={
        var i = startingKey
        t.getArgs.iterator.toList.map((n:WtNode) => {
            i = i+1

            n match {
                case ta : WtTemplateArgument => new PropertyNode(if(ta.hasName){ta.getName.trim} else {i.toString}, transformNodes(ta.getValue, start_line), start_line + line)
                case _ => throw new Exception("expected TemplateArgument as template child")
            }
        })
    }

    def transformNode(node : WtNode, start_line : Int) : List[Node] = {
        val line = if(node.getNativeLocation == null){lastLine} else {node.getNativeLocation.getLine}
        lastLine = line
        node match {
            case s : WtSection => {
                val nl = ListBuffer[Node]()
                nl append new SectionNode(s.getHeading.toString, s.getLevel, transformNodes(wikiNodeFactory.list(s.iterator.toList.head), start_line), start_line + line)
                // add the children of this section
                // makes it flat
                nl appendAll transformNodes(s.iterator.toList.tail, start_line) //first NodeList is the section title again
                nl.toList
            }
            case p : WtParagraph => transformNodes(p, start_line)
            case t : WtTemplate => {

                val name : String = nodeList2string(t.getName)

                var nameClean = WikiUtil.cleanSpace(name)

                var tpl = List[Node]()

                // Parser Function Node
                if(name.charAt(0) == '#'){
                    var str : String = ""
                    val title = name.substring(0, name.indexOf(':'))
                    for ( node  <-  transformNode(t.getName.asInstanceOf[WtNode], start_line).iterator.toList){
                        str = str + node.toWikiText
                    }
                    nameClean = WikiUtil.cleanSpace(str)

                    //Extract the body of the first argument
                    var arg1 = str.substring(name.indexOf(':')+1)

                    //Create an AST from the first argument
                    val parsed = parse(pageId, arg1)
                    val nodeList =transformNodes(parsed, line)
                    val prop = new PropertyNode("1",nodeList.filter(p => p.toWikiText.length > 1),start_line + line  )

                    tpl = List(new ParserFunctionNode(title, prop :: getProperties(1,t, line, start_line) , start_line + line))
                } else {

                    val title = new WikiTitle(nameClean, Namespace.Template, language)
                    tpl =  TemplateNode.transform(new TemplateNode(title, getProperties(0,t, line, start_line), start_line + line, transformNodes(t.getName, start_line)))
                }

                tpl
            }
            case tn : WtText => List(new TextNode(tn.getContent, start_line + line))
            case ws : WtWhitespace => List(new TextNode(ws, start_line + line)) //as text
            case img : WtImageLink =>  {
                val target = img.getTarget()
                val destinationURL = WikiTitle.parse(target.asInstanceOf[WtPageName].get(0).asInstanceOf[WtText].getContent, language)
                var options : String = ""
                for( a: WtNode <- img.getOptions){
                    if(a.isInstanceOf[WtLinkOptionKeyword]){
                        options = options + "|" + a.asInstanceOf[WtLinkOptionKeyword].getKeyword.toString
                    }
                }
                if(options != "") {
                    options = options.substring(1)
                }
                val destinationNodes = List[Node](new TextNode(img.getTarget, start_line + line)) //parsing of target not yet supported
                val titleNodes = List(new TextNode(options + "|" + img.getTitle.get(0).asInstanceOf[WtText].getContent, start_line + line))

                if (destinationURL.language == language) {
                    List(new InternalLinkNode(destinationURL, titleNodes, start_line + line, destinationNodes))
                } else {
                    List(new InterWikiLinkNode(destinationURL, titleNodes, start_line + line, destinationNodes))
                }
            }
            case il : WtInternalLink => {
                val target = il.getTarget()
                val target2 = if(target != null && !target.equals("")){ if(target.startsWith("#")) target.substring(1) else target} else "none"
                val destinationURL = WikiTitle.parse(target2.asInstanceOf[WtPageName].get(0).asInstanceOf[WtText].getContent, language)

                val destinationNodes = List[Node](TextNode(il.getTarget, start_line + line)) //parsing of target not yet supported
                val titleNodes = if(!il.getTitle.isEmpty){transformNodes(il.getTitle, start_line)} else {List(TextNode(il.getTarget, start_line + line))}

                val postfix : List[Node] = if(il.getPostfix != null && il.getPostfix != ""){
                    List(TextNode(il.getPostfix, start_line + line))
                } else List()

                if (destinationURL.language == language) {
                    List(InternalLinkNode(destinationURL, titleNodes, start_line + line, destinationNodes)) ++ postfix
                } else {
                    List(InterWikiLinkNode(destinationURL, titleNodes, start_line + line, destinationNodes)) ++ postfix
                }
            }
            case el : WtExternalLink => {
                val destinationURL = UriUtils.createURI(el.getTarget) match{
                    case Success(u) => u
                    case Failure(f) => UriUtils.createURI("http://example.org").get
                }
                val destinationNodes = List[Node](TextNode(el.getTarget.toString, start_line + line)) //parsing of target not yet supported
                val titleNodes = transformNodes(el.getTitle, start_line)
                List(ExternalLinkNode(destinationURL, titleNodes, start_line + line, destinationNodes))
            }
            case url : WtUrl => {
                val destinationURL = UriUtils.createURI(url).get
                val destinationNodes = List[Node](TextNode(url, start_line + line)) //parsing of target not yet supported
                List(ExternalLinkNode(destinationURL, destinationNodes, start_line + line, destinationNodes))
            }
            case items : WtUnorderedList => items.iterator.toList.flatMap((n: WtNode) => {
                n match {
                    case item: WtListItem => wrap(item, line, start_line)
                    case item: WtText => List(TextNode(item.getContent, start_line + line))
                    case _ => throw new Exception("expected ItemizationItem as Itemization child")
                }
            })
            case items : WtOrderedList => {
                var i = 0
                items.iterator.toList.map( (n:WtNode) => {
                    //TODO a EnumerationItem can contain Itemization - needs to be emitted as #*
                    n match {
                        case item : WtListItem => wrap(item, line, start_line)
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
            case definitions : WtDefinitionList => definitions.iterator.toList.map( (n:WtNode) => {
                n match {
                    case item : WtDefinitionListDef => wrap(item, line, start_line)
                    case item : WtDefinitionListTerm => wrap(item, line, start_line)
                    case a : WtNode =>  transformNodes(List(a), start_line)//throw new Exception("expected DefinitionDefinition as DefinitionList child. found "+n.getClass.getName+" instead")
                }
            }).flatten
            case tplParam : WtTemplateParameter => {
                if(tplParam != null && tplParam.getDefault != null)
                    List(new TemplateParameterNode(tplParam.getName, transformNodes(tplParam.getDefault, start_line), start_line + line))
                else List[Node]()
            }

            case t : WtTable => {
                val captionNodeOption = t.getBody.iterator.toList.find( (n:WtNode) => n.isInstanceOf[WtTableCaption])
                val caption : Option[String] = if(captionNodeOption.isDefined){Some(captionNodeOption.get.asInstanceOf[WtTableCaption].getBody)} else {None}
                for( tableBody <- t.getBody.iterator.toList.filter((n:WtNode) => n.isInstanceOf[WtTableImplicitTableBody])){
                    var tableBodyObj = tableBody.asInstanceOf[WtTableImplicitTableBody].getBody
                    val rows = tableBodyObj.iterator().toList.filter((n :WtNode) => n.isInstanceOf[WtTableRow]).iterator.toList.map((n : WtNode) => {
                        val tr = n.asInstanceOf[WtTableRow]
                        val cells = tr.getBody.iterator.toList.map((re:WtNode) => {
                            re match {
                                case tableCell: WtTableCell => {
                                    Some(new TableCellNode(transformNodes(tableCell.iterator.toList, start_line), line, getXMLAttribute(tableCell, "rowspan").getOrElse("1").toInt, getXMLAttribute(tableCell, "colspan").getOrElse("1").toInt))
                                }
                                case _ => None
                            }
                        }).filter(_.isDefined).map(_.get)
                        new TableRowNode(cells, line)
                    })
                    return List(new TableNode(caption, rows, line))
                }
                 List()
            }
            case icp : WtIllegalCodePoint => List(new TextNode(icp.getCodePoint(), line))
            case xml : WtXmlElement => {
                val body = xml.getBody
                var content = ""
                body.iterator().toList.filter((n : WtNode ) => n.isInstanceOf[WtText]).map((n : WtNode) => {
                    content = content + n.asInstanceOf[WtText].getContent
                })
                List(new TextNode("<"+xml.getName+">"+content +"</"+xml.getName + ">", start_line + line))
            }
            case cn : WtContentNode => transformNodes(cn, start_line)
            //else
            case _ => List(new TextNode(nodeList2string(wikiNodeFactory.list(node)), start_line + line))
        }
    }

    implicit def nodeList2prepocessedPage(nl: WtNodeList) : PreprocessedWikitext = {
        val t = PreprocessorToParserTransformer.transform(wikiNodeFactory.preproPage(nl, entityMap))
        t
    }

    implicit def preprocessedPage2string(p:PreprocessedWikitext) : String = {
        val t = p.getWikitext()
        t
    }

    implicit def nodeList2string(nl: WtNodeList) : String = {

        preprocessedPage2string(nodeList2prepocessedPage(nl))
    }

    implicit def url2string(url: WtUrl) : String = {
        url.getProtocol +":"+ url.getPath
    }

    implicit def nodes2nodeList(l : List[WtNode]) : WtNodeList = wikiNodeFactory.list(l)
    implicit def nodeList2nodes(l : WtNodeList) : List[WtNode] = l.iterator.toList

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

    class ParameterToDefaultValueResolver(pageId : PageId) extends AstVisitor[WtNode] {

        var entityMap : WtEntityMap = new WtEntityMapImpl()

        var warnings : java.util.List[Warning] = new ArrayList[Warning]()

        def visit(n : WtNode) : WtNode =
        {
            mapInPlace(n)
            return n
        }

        def  visit(n : WtPage) : WtNode =
        {
            this.warnings = n.getWarnings()
            this.entityMap = n.getEntityMap()
            mapInPlace(n)
            return n
        }

        def visit(n : WtTemplateParameter) : WtNode =
        {
            val defValArg = n.getDefault()
            if (defValArg == null)
                return n

            val defVal = defValArg;

            // Shortcut for all those empty default values
            if (defVal.isEmpty())
                return defValArg

            val pprAst = wikiNodeFactory.preproPage(
                defVal, entityMap
            )

            val parsed = engine.postprocessPpOrExpAst(pageId, pprAst);

            val content = parsed.getPage()

            // The parser of course thinks that the given wikitext is a
            // individual page and will wrap even single line text into a
            // paragraph node. We try to catch at least simple cases to improve
            // the resulting AST
            val contentClean = if (content.size() == 1 && content.get(0).getNodeType() == WtNode.NT_PARAGRAPH)
                content.get(0).asInstanceOf[WtParagraph]
            else content

            content
        }
        def visit(n : WtTemplateArgument) : WtNode =
        {
            val value = n.getValue()
            if (value == null)
                return n


            // Shortcut for all those empty default values
            if (value.isEmpty())
                return n

            val pprAst = wikiNodeFactory.preproPage(
                value, entityMap
            )

            val parsed = engine.postprocessPpOrExpAst(pageId, pprAst);

            val content = parsed.getPage

            // The parser of course thinks that the given wikitext is a
            // individual page and will wrap even single line text into a
            // paragraph node. We try to catch at least simple cases to improve
            // the resulting AST
            val contentClean = if (content.size() == 1 && content.get(0).getNodeType() == WtNode.NT_PARAGRAPH)
                content.get(0).asInstanceOf[WtParagraph]
            else content

            // n.setValue(content)
            content
        }
    }

    def getXMLAttribute(node : WtTableCell, name : String) : Option[String] = {
        val xmlAttrs = node.getXmlAttributes()
        xmlAttrs.foreach((an:WtNode) => {
            an match {
                case (xmlAttr : WtXmlAttribute) => {
                    if(xmlAttr.getName == name && xmlAttr.hasValue()){
                        return Some(nodeList2string(xmlAttr.getValue))
                    }
                }
                case _ =>
            }
        })
        None
    }

    def getWrap(node : WtNode) : Tuple2[String, String]= {
        val rtd = node.getAttribute("RTD").asInstanceOf[RtData]

        val start = if(rtd != null && rtd.getFields() != null){
            val rts = rtd.getFields()
            if(rts.length > 0  && rts(0) != null && rts(0).length > 0){
                rts(0)(0).asInstanceOf[String]
            } else defaultStart(node)
        } else defaultStart(node)


        val end = if(rtd != null && rtd.getFields() != null){
            val rts = rtd.getFields()
            if(rts.length > 1 && rts(1) != null && rts(1).length > 0){
                rtd.getFields()(1)(0).asInstanceOf[String]
            } else ""
        } else ""

        (start, end)
    }

    def defaultStart(node : WtNode) : String = node match {
        case i : WtListItem => "*"
        case i : WtDefinitionListDef => ":"
        case i : WtDefinitionListTerm => ":"
        case _ => ""
    }

    def wrap(node : WtContentNode, line : Int, start_line : Int) : List[Node] = {
        val w = getWrap(node)
        var hasExtraNL = false
        val c: List[WtNode] = node.map((a:WtNode)=>{if(a.isInstanceOf[Enumeration] || a.isInstanceOf[WtListItem] || a.isInstanceOf[WtDefinitionList]){hasExtraNL = true; List( wikiNodeFactory.text("\n"), a)} else {List(a)}}).flatten
        List(new TextNode(w._1, line)) ++ transformNodes(c, start_line ) ++ (if(!hasExtraNL){List(new TextNode(w._2, line))} else {List()})
    }
}


