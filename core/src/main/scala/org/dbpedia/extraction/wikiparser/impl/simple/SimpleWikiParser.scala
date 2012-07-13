package org.dbpedia.extraction.wikiparser.impl.simple

import org.dbpedia.extraction.util.{Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.{Disambiguation, Redirect}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.RichString.wrapString
import java.net.URI
import java.util.logging.{Level, Logger}
import java.lang.IllegalArgumentException

import SimpleWikiParser._

object SimpleWikiParser
{
    private val logger = Logger.getLogger(classOf[SimpleWikiParser].getName)

    private val MaxNestingLevel = 10
    private val MaxErrors = 1000

    private val commentEnd = new Matcher(List("-->"));

    private val htmlTagEndOrStart = new Matcher(List("/>", "<"), false);
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
    private val propertyEndOrParserFunctionNameEnd = new Matcher(List("|", "}}", ":"), true);
    private val parserFunctionEnd = new Matcher(List("}}"), true);

    private val tableRowEnd1 = new Matcher(List("|}", "|+", "|-", "|", "!"));
    private val tableRowEnd2 = new Matcher(List("|}", "|-", "|", "!"));

    private val tableCellEnd1 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!", "|", "!"), true);
    private val tableCellEnd2 = new Matcher(List("|}", "|-", "|", "!"));
    private val tableCellEnd3 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!"), true);

    private val sectionEnd = new Matcher(List("=\n", "=\r", "\n"), true);
}

/**
 * Port of the DBpedia WikiParser for PHP.
 */
//TODO section names should only contain the contents of the TextNodes
final class SimpleWikiParser extends WikiParser
{
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
        // TODO: the regex used in org.dbpedia.extraction.mappings.Redirects.scala is probably a bit better
        val redirectRegex = """(?is)\s*(?:""" + Redirect(page.title.language).mkString("|") + """)\s*:?\s*\[\[.*"""
        // TODO: also extract the redirect target.
        // TODO: compare extracted redirect target to the one found by Wikipedia (stored in the WikiPage object).
        // Problems:
        // - if the WikiPage object was not read from XML dump or api.php, redirect may not be set in WikiPage
        // - generating the XML dump files takes several days, and the wikitext is obviously not generated at the
        //   same time as the redirect target, so sometimes they do not match.
        // In a nutshell: if the redirect in WikiPage is different from what we find, we're probably correct.
        val isRedirect = page.source.matches(redirectRegex)

        //Check if this page is a Disambiguation
        //TODO resolve template titles
        val disambiguationNames = Disambiguation.get(page.title.language).getOrElse(Set("Disambig"))
        val isDisambiguation = nodes.exists(node => findTemplate(node, disambiguationNames, page.title.language))

        //Return page node
        new PageNode(page.title, page.id, page.revision, page.timestamp, isRedirect, isDisambiguation, nodes)
    }

    private def findTemplate(node : Node, names : Set[String], language : Language) : Boolean = node match
    {
        case TemplateNode(title, _, _) => names.contains(title.decoded)
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
                // FIXME: matcher.toString is not defined, message will be useless
                throw new WikiParserException("Node not closed; expected "+matcher, line, source.findLine(line));
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
                    skipHtmlTag(source, refEnd)
                }
                else if(source.lastTag("<math"))
                {
                    //Skip math tag
                    skipHtmlTag(source, mathEnd)
                }
                else if(source.lastTag("<code"))
                {
                    //Skip code tag
                    skipHtmlTag(source, codeEnd)
                }
                else if(source.lastTag("<source"))
                {
                    //Skip source tag
                    skipHtmlTag(source, sourceEnd)
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
                            logger.log(Level.FINE, "Error parsing node. "+ex.getMessage, ex)

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

    private def skipHtmlTag(source : Source, matcher : Matcher)
    {
        source.find(htmlTagEndOrStart, false)
        if(source.lastTag("<"))
        {
            val endString = matcher.userTags.headOption
                                            .getOrElse(throw new IllegalArgumentException("Matcher must have one closing HTML tag"))
                                            .substring(1) // cut the first "<"
            if(source.nextTag(endString))
            {
                source.seek(endString.length())
            }
            else
            {
                source.find(matcher, false)
            }
        }
        //else we found "/>"
    }
    
    private def createNode(source : Source, level : Int) : Node =
    {
        if(source.lastTag("[") || source.lastTag("http"))
        {
            parseLink(source, level)
        }
        else if(source.lastTag("{{"))
        {
            if (source.pos < source.length && source.getString(source.pos, source.pos+1) == "{")
            {
                source.pos = source.pos+1   //advance 1 char
                return parseTemplateParameter(source, level)
            }

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
            // FIXME: this block is a 98% copy of the next block
          
            //val m = source.find(internalLinkLabelOrEnd)

            //Set destination
            //val destination = source.getString(startPos, source.pos - m.tag.length).trim
            val destination = parseUntil(internalLinkLabelOrEnd, source, level)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationUri =
            if(destination.size == 0) {
              ""
            } else if(destination(0).isInstanceOf[TextNode]) {
              destination(0).asInstanceOf[TextNode].text
            } else {
              // The following line didn't make sense. createInternalLinkNode() will simply throw a NullPointerException.
              // null // has a semantic within the wiktionary module, and should never occur for wikipedia
              
              throw new WikiParserException("Failed to parse internal link: " + destination, startLine, source.findLine(startLine))
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

            createInternalLinkNode(source, destinationUri, nodes, startLine, destination)
        }
        else if(source.lastTag("["))
        {
            // FIXME: this block is a 98% copy of the previous block
          
            //val tag = source.find(externalLinkLabelOrEnd)

            //Set destination
            //val destinationURI = source.getString(startPos, source.pos - 1).trim
            val destination = parseUntil(externalLinkLabelOrEnd, source, level)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationURI = 
            if (destination.size == 0) {
              ""
            } else if(destination(0).isInstanceOf[TextNode]) {
              destination(0).asInstanceOf[TextNode].text
            } else {
              // The following line didn't make sense. createExternalLinkNode() will simply throw a NullPointerException.
              // null // has a semantic within the wiktionary module, and should never occur for wikipedia
              
              throw new WikiParserException("Failed to parse external link: " + destination, startLine, source.findLine(startLine))
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

            createExternalLinkNode(source, destinationURI, nodes, startLine, destination)
        }
        else
        {
            val result = source.find(linkEnd)
            //The end tag (e.g. ' ') is not part of the link itself
            source.seek(-result.tag.length)

            //Set destination
            val destinationURI = source.getString(startPos - 4, source.pos).trim
            //Use destination as label
            val nodes = List(new TextNode(destinationURI, source.line))

            createExternalLinkNode(source, destinationURI, nodes, startLine, nodes)
        }
    }

    private def createExternalLinkNode(source : Source, destination : String, nodes : List[Node], line : Int, destinationNodes : List[Node]) : LinkNode =
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
    
    private def createInternalLinkNode(source : Source, destination : String, nodes : List[Node], line : Int, destinationNodes : List[Node]) : LinkNode =
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

    private def parseTemplateParameter(source : Source, level : Int) : TemplateParameterNode =
    {
        val line = source.line
        val keyNodes = parseUntil(templateParameterEnd , source, level)

        if(keyNodes.size != 1 || ! keyNodes.head.isInstanceOf[TextNode])
                throw new WikiParserException("Template variable contains invalid elements", line, source.findLine(line))
        
        // FIXME: removing "<includeonly>" here is a hack.
        // We need a preprocessor that resolves stuff like <includeonly>...</includeonly> 
        // based on configuration flags.
        val key = keyNodes.head.toWikiText.replace("<includeonly>", "").replace("</includeonly>", "").replace("<noinclude>", "").replace("</noinclude>", "")

        // FIXME: parseUntil(templateParameterEnd) should be correct. Without it, we don't actually 
        // consume the source until the end of the template parameter. But if we use it, the parser
        // fails for roughly twice as many pages, so for now we deactivate it with "if (true)".
        val nodes = if (true || source.lastTag("}}}")) List.empty else parseUntil(templateParameterEnd, source, level)

        new TemplateParameterNode(key, nodes, line)
    }

    private def parseTemplate(source : Source, level : Int) : Node =
    {
        val startLine = source.line
        var title : WikiTitle = null;
        var properties = List[PropertyNode]()
        var curKeyIndex = 1

        while(true)
        {
            //The first entry denotes the name of the template or parser function
            if(title == null)
            {
                val nodes = parseUntil(propertyEndOrParserFunctionNameEnd, source, level)

                val templateName = nodes match
                {
                    case TextNode(text, _) :: _ => text
                    case _ => throw new WikiParserException("Invalid Template name", startLine, source.findLine(startLine))
                }

                val decodedName = WikiUtil.cleanSpace(templateName).capitalize(source.language.locale)
                if(source.lastTag(":"))
                {
                    return parseParserFunction(decodedName, source, level)
                }
                title = new WikiTitle(decodedName, Namespace.Template, source.language)
            }
            else
            {
                val propertyNode = parseProperty(source, curKeyIndex.toString, level)
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

    private def parseParserFunction(decodedName : String, source : Source, level : Int) : ParserFunctionNode =
    {
        val children = parseUntil(parserFunctionEnd, source, level)
        val startLine = source.line

        ParserFunctionNode(decodedName, children, startLine)
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
                return new TableRowNode(nodes.reverse, line)
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
        
        new TableCellNode(nodes, startLine, rowspan, colspan)
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
