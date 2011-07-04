package org.dbpedia.extraction.wikiparser.impl.simple

import org.dbpedia.extraction.util.{Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.{Disambiguation, Redirect}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.StringUtils._
import java.net.URI
import java.util.logging.{Level, Logger}

/**
 * Port of the DBpedia WikiParser for PHP.
 */
//TODO support parsing functions: {{formatnum:{{CanPopCommas}} }}
//TODO section names should only contain the contents of the TextNodes
final class SimpleWikiParser extends WikiParser
{
    private val logger = Logger.getLogger(classOf[SimpleWikiParser].getName)

    private val MaxNestingLevel = 10
    private val MaxErrors = 1000

    //TODO move matchers to companion object

    private val commentEnd = new Matcher(List("-->"));
    private val htmlTagEnd = new Matcher(List("/>"), false, disallowedTags = List("<"));
    private val refEnd = new Matcher(List("</ref>"));
    private val mathEnd = new Matcher(List("</math>"));
    private val codeEnd = new Matcher(List("</code>"));
    private val sourceEnd = new Matcher(List("</source>"));
        
    private val internalLinkLabelOrEnd = new Matcher(List("|", "]]", "\n"));
    private val internalLinkEnd = new Matcher(List("]]", "\n"), true);

    private val externalLinkLabelOrEnd = new Matcher(List(" ", "]", "\n"));
    private val externalLinkEnd = new Matcher(List("]", "\n"), true);

    private val linkEnd = new Matcher(List(" ", "{","}", "[", "]", "\n", "\t"));

    private val propertyValueOrEnd = new Matcher(List("=", "|", "}}"), true);
    private val propertyEnd = new Matcher(List("|", "}}"), true);
    private val templateParameterEnd = new Matcher(List("|", "}}}"), true);

    private val tableRowEnd1 = new Matcher(List("|}", "|+", "|-", "|", "!"));
    private val tableRowEnd2 = new Matcher(List("|}", "|-", "|", "!"));

    private val tableCellEnd1 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!", "|", "!"), true);
    private val tableCellEnd2 = new Matcher(List("|}", "|-", "|", "!"));
    private val tableCellEnd3 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!"), true);

    private val sectionEnd = new Matcher(List("=\n", "=\r", "\n"), true);
    
    /**
     * Parses WikiText source and builds an Abstract Syntax Tree.
     *
     * @param page The page to be parsed.
     * @return The PageNode which represents the root of the AST
     * @throws WikiParserException if an error occured during parsing
     */
    def apply(page : WikiPage) : PageNode =
    {
        //Parse source
        val nodes = parseUntil(new Matcher(List(), true), new Source(page.source, page.title.language), 0)

        //Check if this page is a Redirect
        val redirectRegex = """(?is)\s*(?:""" + Redirect(page.title.language).getOrElse(Set("#redirect")).mkString("|") + """)\s*:?\s*\[\[.*"""
        val isRedirect = page.source.matches(redirectRegex)

        //Check if this page is a Disambiguation
        //TODO resolve template titles
        val disambiguationNames = Disambiguation(page.title.language).getOrElse(Set("Disambig"))
        val isDisambiguation = nodes.exists(node => findTemplate(node, disambiguationNames, page.title.language))

        //Return page node
        new PageNode(page.title, page.id, page.revision, isRedirect, isDisambiguation, nodes)
    }

    private def findTemplate(node : Node, names : Set[String], language : Language) : Boolean = node match
    {
        case TemplateNode(title, _, _) if names.contains(title.decoded) => true
        case TemplateNode(title, _, _) => false
        case _ => node.children.exists(node => findTemplate(node, names, language))
    }
    
    private def  parseUntil(matcher : Matcher, source : Source, level : Int) : List[Node] =
    {
        val line = source.line

        //Check nesting level
        if(level > MaxNestingLevel)
        {
            throw new WikiParserException("Maximum nesting level exceeded", line, source.findLine(line))
        }

        //Check number of errors
        if(source.errors > MaxErrors)
        {
            throw new TooManyErrorsException(line, source.findLine(line))
        }

    	var nodes = List[Node]()
        var lastPos = source.pos
        var lastLine = source.line
        var currentText = ""

        while(true)
        {
            val m = source.find(matcher, false);

            //Add text
            if(m.matched && source.pos - lastPos > m.tag.length)
            {
                currentText += source.getString(lastPos, source.pos - m.tag.length)
            }
            else if(!m.matched)
            {
                currentText += source.getString(lastPos, source.pos)
            }

            //If this text is at the beginning => remove leading whitespace
            if(nodes.isEmpty)
            {
            	currentText = currentText.replaceAll("^\\s+", "")
            }
            
            //If this text is at the end => remove trailing whitespace and return
            if((!m.matched && level == 0) || !m.isStdTag)
            {
            	if(currentText.isEmpty)
            	{
            		return nodes.reverse
            	}
            	else
            	{
                nodes ::= TextNode(currentText, lastLine)
            		return nodes.reverse
            	}
            }

            //Check result of seek
            if(!m.matched)
            {
               throw new WikiParserException("Node not closed", line, source.findLine(line));
            }
            else
            {
                if(source.lastTag("<!--"))
                {
                    //Skip html comment
                    source.find(commentEnd, false)
                }
                else if(source.lastTag("<ref"))
                {
                    //Skip reference
                    source.find(htmlTagEnd, false) match
                    {
                        case m : MatchResult if m.matched => m
                        case _ => source.find(refEnd, false)
                    }
                }
                else if(source.lastTag("<math"))
                {
                    //Skip math tag
                    source.find(htmlTagEnd, false) match
                    {
                        case m : MatchResult if m.matched => m
                        case _ => source.find(mathEnd, false)
                    }
                }
                else if(source.lastTag("<code"))
                {
                    //Skip code tag
                    source.find(htmlTagEnd, false) match
                    {
                        case m : MatchResult if m.matched => m
                        case _ => source.find(codeEnd, false)
                    }
                }
                else if(source.lastTag("<source"))
                {
                    //Skip source tag
                    source.find(htmlTagEnd, false) match
                    {
                        case m : MatchResult if m.matched => m
                        case _ => source.find(sourceEnd, false)
                    }
                }
                else
                {
                    val startPos = source.pos
                    val startLine = source.line

                    try
                    {
                         //Parse new node
                         val newNode = createNode(source, level + 1)

                         //Add text node
                         if(!currentText.isEmpty)
                         {
                             nodes ::= TextNode(currentText, lastLine)
                             currentText = ""
                         }

                         //Add new node
                         nodes ::= newNode
                    }
                    catch
                    {
                        case ex : TooManyErrorsException => throw ex
                        case ex : WikiParserException =>
                        {
                            logger.log(Level.FINE, "Error parsing node.", ex)

                            source.pos = startPos
                            source.line = startLine
                            source.errors += 1

                            currentText += m.tag
                        }
                    }
                }
            }

            lastPos = source.pos;
            lastLine = source.line;
        }
        
        nodes.reverse
    }
    
    private def createNode(source : Source, level : Int) : Node =
    {
        if(source.lastTag("[") || source.lastTag("http"))
        {
            parseLink(source, level)
        }
        else if(source.lastTag("{{"))
        {   val nextToken = source.getString(source.pos, source.pos+1)
            if ( nextToken == "{")
                return parseTemplateParameter(source, level)
            //special template code {{#if
            if ( nextToken == "#")
                throw new WikiParserException("Unknown element type", source.line, source.findLine(source.line));
            parseTemplate(source, level)
        }
        else if(source.lastTag("{|"))
        {
            parseTable(source, level)
        }
        else if(source.lastTag("\n="))
        {
            parseSection(source)
        }
        else
            throw new WikiParserException("Unknown element type", source.line, source.findLine(source.line));
    }
    
    private def parseLink(source : Source, level : Int) : LinkNode =
    {
        val startPos = source.pos
        val startLine = source.line
        
        if(source.lastTag("[["))
        {
            //val m = source.find(internalLinkLabelOrEnd)

            //Set destination
            //val destination = source.getString(startPos, source.pos - m.tag.length).trim
            val destination = parseUntil(internalLinkLabelOrEnd, source, level)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationUri = if(destination.size == 0){""} else if(destination(0).isInstanceOf[TextNode]){
              destination(0).asInstanceOf[TextNode].text
            } else {
              null //has a semantic within the wiktionary module, and should never occur for wikipedia
            }

            //Parse label
            val nodes =
                if(source.lastTag("|"))
                {
                   parseUntil(internalLinkEnd, source, level)
                }
                else
                {
                    //No label found => Use destination as label
                    List(new TextNode(destinationUri, source.line))
                }

            createLinkNode(source, destinationUri, nodes, startLine, false, destination)
        }
        else if(source.lastTag("["))
        {
            //val tag = source.find(externalLinkLabelOrEnd)

            //Set destination
            //val destinationURI = source.getString(startPos, source.pos - 1).trim
            val destination = parseUntil(externalLinkLabelOrEnd, source, level)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationURI = if(destination.size == 0){""} else if(destination(0).isInstanceOf[TextNode]){
              destination(0).asInstanceOf[TextNode].text
            } else {
              null //has a semantic within the wiktionary module, and should never occur for wikipedia
            }
            //Parse label
            val nodes =
                if(source.lastTag(" "))
                {
                    parseUntil(externalLinkEnd, source, level);
                }
                else
                {
                    //No label found => Use destination as label
                    List(new TextNode(destinationURI, source.line))
                }

            createLinkNode(source, destinationURI, nodes, startLine, true, destination)
        }
        else
        {
            val result = source.find(this.linkEnd)
            //The end tag (e.g. ' ') is not part of the link itself
            source.seek(-result.tag.length)

            //Set destination
            val destinationURI = source.getString(startPos - 4, source.pos).trim
            //Use destination as label
            val nodes = List(new TextNode(destinationURI, source.line))

            createLinkNode(source, destinationURI, nodes, startLine, true, nodes)
        }
    }

    private def createLinkNode(source : Source, destination : String, nodes : List[Node], line : Int, external : Boolean, destinationNodes : List[Node]) : LinkNode =
    {
        if(external)
        {
            try
            {
        	    ExternalLinkNode(URI.create(destination), nodes, line, destinationNodes)
            }
            catch
            {
                case _ : IllegalArgumentException => throw new WikiParserException("Invalid external link: " + destination, line, source.findLine(line))
            }
        }
        else
        {
            val destinationTitle = WikiTitle.parse(destination, source.language)

            if(destinationTitle.language == source.language)
            {
                InternalLinkNode(destinationTitle, nodes, line, destinationNodes)
            }
            else
            {
                 InterWikiLinkNode(destinationTitle, nodes, line, destinationNodes)
            }
        }
    }

    private def parseTemplateParameter(source : Source, level : Int) : TemplateParameterNode =
    {
        val line = source.line
        source.pos = source.pos+1   //advance 1 char
        var nodes = parseUntil(templateParameterEnd , source, level)

        if(nodes.size != 1 || !nodes.head.isInstanceOf[TextNode])
                throw new WikiParserException("Template variable contains invalid elements", line, source.findLine(line))

        new TemplateParameterNode( nodes.head.toWikiText(), source.lastTag("|"), line)
    }
    
    private def parseTemplate(source : Source, level : Int) : TemplateNode =
    {
    	val startLine = source.line
    	var title : WikiTitle = null;
    	var properties = List[PropertyNode]()
    	var curKeyIndex = 1

        while(true)
        {
            val propertyNode = parseProperty(source, curKeyIndex.toString, level)
            
            //The first entry denotes the name of the template
            if(title == null)
            {
                //TODO support parser functions
                var templateName = propertyNode.children match
                {
                    case TextNode(text, _) :: _ => text
                    case _ => throw new WikiParserException("Invalid Template name", startLine, source.findLine(startLine))
                }

                //Remove arguments of parser functions as they are not supported
                templateName = templateName.split(":", 2) match
                {
                    case Array(function, name) => name
                    case _ => templateName
                }

                val decodedName = WikiUtil.cleanSpace(templateName).capitalizeLocale(source.language.locale)
                title = new WikiTitle(decodedName, WikiTitle.Namespace.Template, source.language)
            }
            else
            {
                properties ::= propertyNode

                if(propertyNode.key == curKeyIndex.toString)
                {
            	    curKeyIndex += 1
                }
            }

            //Reached template end?
            if(source.lastTag("}}"))
            {
                return TemplateNode(title, properties.reverse, startLine)
            }
        }
    	
    	throw new WikiParserException("Template not closed", startLine, source.findLine(startLine))
    }
    
    private def parseProperty(source : Source, defaultKey : String, level : Int) : PropertyNode =
    {
    	val line = source.line
    	var nodes = parseUntil(propertyValueOrEnd, source, level)
    	var key = defaultKey
 
        if(source.lastTag("="))
        {
            //The currently parsed node is a key
        	if(nodes.size != 1 || !nodes.head.isInstanceOf[TextNode])
        		throw new WikiParserException("Template property key contains invalid elements", line, source.findLine(line))
        	
            key = nodes.head.retrieveText.get.trim

            //Parse the corresponding value
            nodes = parseUntil(propertyEnd, source, level);
        }
        
        PropertyNode(key, nodes, line)
    }
    
    private def parseTable(source : Source, level : Int) : TableNode =
    {
    	val startPos = source.pos
    	val line = source.line
 
    	var nodes = List[TableRowNode]()
    	var caption : Option[String] = None

        //Parse rows
        var done = false
        while(!done)
        {
            //Find first row
            val m = source.find(tableRowEnd1) //"|}", "|+", "|-", "|", "!"
            val tag = m.tagIndex

            if(tag == 0) //"|}"
            {
                //Reached table end
                done = true
            }
            else if(tag == 1) //"|+"
            {
                //Found caption
                caption = Some(source.getString(startPos, source.pos - 2).trim)
            }
            else
            {
                if(tag == 2) //"|-"
                {
                    //Move to first cell
                    val m2 = source.find(tableRowEnd2) //"|}", "|-", "|", "!"
    
                    if(m2.tagIndex == 0 || m2.tagIndex == 1)
                    {
                        //Empty row
                        nodes ::= new TableRowNode(List.empty, source.line)
                        return TableNode(caption, nodes.reverse, line);
                    }
                }
                
                //Parse row
	            nodes ::= parseTableRow(source, level)
	
	            //Reached table end?
	            if(source.lastTag("|}"))
	            {
	                done = true
	            }
            }
        }
    	
    	TableNode(caption, nodes.reverse, line);
    }

    private def parseTableRow(source : Source, level : Int) : TableRowNode =
    {
    	val line = source.line
    	var nodes = List[TableCellNode]()
    	
        while(true)
        {
            //Parse table cell
            nodes ::= parseTableCell(source, level)

            //Reached row end?
            if(source.lastTag("|}") || source.lastTag("|-"))
            {
                new TableRowNode(nodes.reverse, line)
            }
        }
        
        null
    }

    private def parseTableCell(source : Source, level : Int) : TableCellNode =
    {
        val startPos = source.pos
    	val startLine = source.line
    	var rowspan = 1
    	var colspan = 1
        var nodes = parseUntil(tableCellEnd1, source, level)

        val lookBack = source.getString(source.pos - 2, source.pos)

        if(lookBack == "\n ")
        {
            source.find(tableCellEnd2)
        }
        else if((lookBack(1) == '|' || lookBack(1) == '!') && lookBack(0) != '\n' && lookBack(0) != '|' && lookBack(0) != '!' && !nodes.isEmpty)
        {
            //This cell contains formatting parameters
            val formattingStr = source.getString(startPos, source.pos - 1).trim

            rowspan = parseTableParam("rowspan", formattingStr)
            colspan = parseTableParam("colspan", formattingStr)

            //Parse the cell contents
            nodes = this.parseUntil(tableCellEnd3, source, level)
            if(source.lastTag("\n "))
            {
                source.find(tableCellEnd2);
            }
        }
        
        val node = new TableCellNode(nodes, startLine)
        node.setAnnotation("rowspan", rowspan)
        node.setAnnotation("colspan", colspan)
        node
    }

    private def parseTableParam(name : String, str : String) : Int =
    {
        //Find start index of the value
        var start = str.indexOf(name);
        if(start == -1)
        {
            return 1;
        }
        start = str.indexOf('=', start)
        if(start == -1)
        {
            return 1;
        }
        start += 1;

        //Find end index of the value
        var end = str.indexOf(' ', start)
        if(end == -1)
        {
            end = str.length - 1;
        }

        //Convert to integer
        var valueStr = str.substring(start, end + 1)
        valueStr = valueStr.replace("\"", "").trim

        try
        {
        	valueStr.toInt;
        }
        catch
        {
        	case _ => 1
        }
    }

    private def parseSection(source : Source) : SectionNode =
    {
        val line = source.line

        //Determine level
        var level = 1
        while(source.nextTag("="))
        {
            level += 1
            source.seek(1)
        }

        //Get name
        val startPos = source.pos
        val nodes = this.parseUntil(sectionEnd, source, level)
        source.seek(-1)
        if(nodes.isEmpty)
        {
            throw new WikiParserException("Section was not closed", line, source.findLine(line))
        }
        val endPos = source.pos - level - 1
        if(endPos <= startPos)
        {
            throw new WikiParserException("Invalid section tag", line, source.findLine(line))
        }
        val name = source.getString(startPos, endPos).trim

        //Remove trailing '=' from section name
        if(nodes.last.isInstanceOf[TextNode] && nodes.last.asInstanceOf[TextNode].text.endsWith("=")){
          val lastTextNode = nodes.last.asInstanceOf[TextNode]
          val cleanNodes = nodes.init :+ lastTextNode.copy(text = lastTextNode.text.dropRight(level - 1))
          return SectionNode(name, level, cleanNodes, source.line - 1);
        }

        SectionNode(name, level, nodes, source.line - 1);
    }
}
