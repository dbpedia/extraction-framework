package org.dbpedia.extraction.wikiparser.impl.simple

import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.mappings.Redirects
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.iri.{IRISyntaxException, UriUtils}

import scala.util.{Failure, Success}

object SimpleWikiParser
{
    private val logger = Logger.getLogger(this.getClass.getName)

    private val MaxNestingLevel = 10
    private val MaxErrors = 1000

    private val commentEnd = new Matcher(List("-->"))

    private val htmlTagEndOrStart = new Matcher(List("/>", "<"), false)
    private val refEnd = new Matcher(List("</ref>"))
    private val mathEnd = new Matcher(List("</math>"))
    private val codeEnd = new Matcher(List("</code>"))
    private val sourceEnd = new Matcher(List("</source>"))
        
    private val internalLinkLabelOrEnd = new Matcher(List("|", "]]", "\n"))
    private val internalLinkEnd = new Matcher(List("]]", "\n"), true)

    private val externalLinkLabelOrEnd = new Matcher(List(" ", "]", "\n"))
    private val externalLinkEnd = new Matcher(List("]", "\n"), true)

    private val linkEnd = new Matcher(List(" ", "{","}", "[", "]", "\n", "\t"))

    // '|=' is not valid wiki markup but safe to include, see http://sourceforge.net/tracker/?func=detail&atid=935521&aid=3572779&group_id=190976
    private val propertyValueOrEnd = new Matcher(List("|=","=", "|", "}}"), true)
    private val propertyEnd = new Matcher(List("|", "}}"), true)
    private val templateParameterEnd = new Matcher(List("|", "}}}"), true)
    private val propertyEndOrParserFunctionNameEnd = new Matcher(List("|", "}}", ":"), true)
    private val parserFunctionEnd = new Matcher(List("}}"), true)

    private val tableRowEnd1 = new Matcher(List("|}", "|+", "|-", "|", "!"))
    private val tableRowEnd2 = new Matcher(List("|}", "|-", "|", "!"))

    private val tableCellEnd1 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!", "|", "!"), true)
    private val tableCellEnd2 = new Matcher(List("|}", "|-", "|", "!"))
    private val tableCellEnd3 = new Matcher(List("\n ", "\n|}", "\n|-", "\n|", "\n!", "||", "!!"), true)

    private val sectionEnd = new Matcher(List("=\n", "=\r", "\n"), true)

    /**
     * Parses WikiText source and builds an Abstract Syntax Tree.
     *
     * @param page The page to be parsed.
     * @return The PageNode which represents the root of the AST
     * @throws WikiParserException if an error occured during parsing
     */
    def apply(page : WikiPage, templateRedirects: Redirects = new Redirects(Map())) : Option[PageNode] =
    {

      if (page.format != null && page.format.nonEmpty && page.format != "text/x-wiki")
        None
      else if(false)
        None
      else
      {
        //Parse source
        val nodes = parseUntil(new Matcher(List(), true), new Source(page.source, page.title.language), 0, templateRedirects)

        //Return page node
        Some(new PageNode(page.title, page.id, page.revision, page.timestamp, page.contributorID, page.contributorName, page.source, nodes))
      }
    }
    
    private def  parseUntil(matcher : Matcher, source : Source, level : Int, templateRedirects: Redirects) : List[Node] =
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
            val m = source.find(matcher, throwIfNoMatch = false)

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
                throw new WikiParserException("Node not closed; expected "+matcher, line, source.findLine(line))
            }
            else
            {
                if(source.lastTag("<!--"))
                {
                    //Skip html comment
                    source.find(commentEnd, throwIfNoMatch = false)
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
                         val newNode = createNodes(source, level + 1, templateRedirects)

                         //Add text node
                         if(!currentText.isEmpty)
                         {
                             nodes ::= TextNode(currentText, lastLine)
                             currentText = ""
                         }

                         //Add new node
                         nodes :::= newNode
                    }
                    catch
                    {
                        case ex : TooManyErrorsException => throw ex
                        case ex : WikiParserException =>
                            logger.log(Level.FINE, "Error parsing node. "+ex.getMessage, ex)
                            source.pos = startPos
                            source.line = startLine
                            source.errors += 1
                            currentText += m.tag
                    }
                }
            }

            lastPos = source.pos
            lastLine = source.line
        }
        
        nodes.reverse
    }

    private def skipHtmlTag(source : Source, matcher : Matcher)
    {
        source.find(htmlTagEndOrStart, throwIfNoMatch = false)
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
                source.find(matcher, throwIfNoMatch = false)
            }
        }
        //else we found "/>"
    }
    
    private def createNodes(source : Source, level : Int, templateRedirects: Redirects) : List[Node] =
    {
        if(source.lastTag("[") || source.lastTag("http"))
        {
            parseLink(source, level, templateRedirects)
        }
        else if(source.lastTag("{{"))
        {
            if (source.pos < source.length && source.getString(source.pos, source.pos+1) == "{")
            {
                source.pos = source.pos+1   //advance 1 char
                return List(parseTemplateParameter(source, level, templateRedirects))
            }

            parseTemplate(source, level, templateRedirects)
        }
        else if(source.lastTag("{|"))
        {
            List(parseTable(source, level, templateRedirects))
        }
        else if(source.lastTag("\n="))
        {
            List(parseSection(source, templateRedirects))
        }
        else
            throw new WikiParserException("Unknown element type", source.line, source.findLine(source.line))
    }

  /**
   * Try to parse a link node.
   * Pay attention to invalid ExternalLinkNodes, as they are very likely to be plain text nodes
   *
   * @param source
   * @param level
   * @return
   */
    private def parseLink(source : Source, level : Int, templateRedirects: Redirects) : List[Node] =
    {
        val startPos = source.pos
        val startLine = source.line
        
        if(source.lastTag("[["))
        {
            // FIXME: this block is a 98% copy of the next block
          
            //val m = source.find(internalLinkLabelOrEnd)

            //Set destination
            //val destination = source.getString(startPos, source.pos - m.tag.length).trim
            val destination = parseUntil(internalLinkLabelOrEnd, source, level, templateRedirects)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationUri =
            if(destination.isEmpty) {
              ""
            } else destination.head match {
                case node: TextNode => node.text
                case _ => throw new WikiParserException("Failed to parse internal link: " + destination, startLine, source.findLine(startLine))
            }

            //Parse label
            val nodes =
                if(source.lastTag("|"))
                {
                   parseUntil(internalLinkEnd, source, level, templateRedirects)
                }
                else
                {
                    //No label found => Use destination as label
                    List(TextNode(destinationUri, source.line))
                }

            /**
             * At the moment, link parsing does not support nested constructs like templates, etc so we have to check it manually here
             * this is mainly hacked to support cases like [[{{#property:p38}}]] or [[{{#property:p38|from=Qxxx}}]]
             */
            val templStart = "{{"
            val templEnd = "}}"
            val label = nodes.map(_.toPlainText).mkString(" ").trim

            var adjujstedDestinationUri = destinationUri
            var adjustedNodes = nodes
            if (destinationUri.contains(templStart) || label.contains(templEnd)) // there is a template to define the link
            {
                //get the text inside the link
                val newText = if (destinationUri.equals(label)) destinationUri else destinationUri + "|" + label
                //reparse the text
                val newSource = new Source(newText, source.language)
                newSource.line = source.line
                val newNodes = parseUntil(new Matcher(List(), true), newSource, 0, templateRedirects)

                val newNodesToText = newNodes.map(_.toPlainText).mkString(" ").trim
                if (newNodesToText.isEmpty && newNodes.nonEmpty)
                {
                    return newNodes
                } else if (!newNodesToText.contains('|')) // same target / label
                {
                    adjujstedDestinationUri = newNodesToText
                    adjustedNodes = newNodes
                } else if (newNodesToText.contains('|'))//we need to split the label from the link
                {
                    adjujstedDestinationUri = newNodesToText.substring(0,newNodesToText.indexOf('|'))
                    adjustedNodes = NodeUtil.splitNodes(newNodes, "|").drop(0).flatten //remove 1st part
                }
            }

            try {
                List(createInternalLinkNode(source, adjujstedDestinationUri, adjustedNodes, startLine, destination))
            } catch {
                // This happens when en interwiki link has a language that is not defined and thows an unknown namespace error
                case e: IllegalArgumentException => throw new WikiParserException("Failed to parse internal link: " + destination, startLine, source.findLine(startLine))
            }
        }
        else if(source.lastTag("["))
        {
            // FIXME: this block is a 98% copy of the previous block
          
            //val tag = source.find(externalLinkLabelOrEnd)

            //Set destination
            //val destinationURI = source.getString(startPos, source.pos - 1).trim
            val destination = parseUntil(externalLinkLabelOrEnd, source, level, templateRedirects)
            //destination is the parsed destination (will be used by e.g. the witkionary module)
            val destinationURI = 
            if (destination.isEmpty) {
              ""
            } else destination.head match {
                case node: TextNode => node.text
                case _ =>
                    // The following line didn't make sense. createExternalLinkNode() will simply throw a NullPointerException.
                    // null // has a semantic within the wiktionary module, and should never occur for wikipedia

                    throw new WikiParserException("Failed to parse external link: " + destination, startLine, source.findLine(startLine))
            }

            var hasLabel = true

            //Parse label
            val nodes =
                if(source.lastTag(" "))
                {
                    parseUntil(externalLinkEnd, source, level, templateRedirects)
                }
                else
                {
                    //No label found => Use destination as label
                    hasLabel = false
                    List(TextNode(destinationURI, source.line))
                }

            try {
              List(createExternalLinkNode(source, destinationURI, nodes, startLine, destination))
            } catch {
              case _ : WikiParserException => // if the URL is not valid then it is a plain text node
                List(TextNode("[" + destinationURI + (if (hasLabel) " " + nodes.map(_.toPlainText).mkString else "") + "]", source.line))
            }
        }
        else
        {
            val result = source.find(linkEnd)
            //The end tag (e.g. ' ') is not part of the link itself
            source.seek(-result.tag.length)

            //Set destination
            val destinationURI = source.getString(startPos - 4, source.pos).trim
            //Use destination as label
            val nodes = List(TextNode(destinationURI, source.line))

            List(createExternalLinkNode(source, destinationURI, nodes, startLine, nodes))
        }
    }

    private def createExternalLinkNode(source : Source, destination : String, nodes : List[Node], line : Int, destinationNodes : List[Node]) : LinkNode =
    {
            // TODO: Add a validation routine which conforms to Mediawiki
            // This will fail for news:// or gopher:// protocols

            //See http://www.mediawiki.org/wiki/Help:Links#External_links
            val relProtocolDest = if (destination.startsWith("//")) "http:" + destination else destination

            // Do not accept non-absolute links because '[]' can be used as wiki text
            // e.g. CC1=CC(=CC(=C1O)[N+](=O)[O-])[N+](=O)[O-]
            if (!UriUtils.hasKnownScheme(relProtocolDest)) throw new WikiParserException("Invalid external link: " + destination, line, source.findLine(line))

            val sameHost = if (relProtocolDest.contains("{{SERVERNAME}}")) relProtocolDest.replace("{{SERVERNAME}}", source.language.baseUri.replace("http://", "")) else relProtocolDest

            UriUtils.createURI(sameHost) match{
                case Success(u) => ExternalLinkNode(u, nodes, line, destinationNodes)
                case Failure(f) => f match {
                    // As per URL.toURI documentation non-strictly RFC 2396 compliant URLs cannot be parsed to URIs
                    case _: IRISyntaxException => throw new WikiParserException("Invalid external link: " + destination, line, source.findLine(line))
                    case _ => throw f
                }
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

    private def parseTemplateParameter(source : Source, level : Int, templateRedirects: Redirects) : TemplateParameterNode =
    {
        val line = source.line
        val keyNodes = parseUntil(templateParameterEnd , source, level, templateRedirects)

        if(keyNodes.size != 1 || ! keyNodes.head.isInstanceOf[TextNode])
                throw new WikiParserException("Template variable contains invalid elements", line, source.findLine(line))
        
        // FIXME: removing "<includeonly>" here is a hack.
        // We need a preprocessor that resolves stuff like <includeonly>...</includeonly> 
        // based on configuration flags.
        val key = keyNodes.head.toWikiText.replace("<includeonly>", "").replace("</includeonly>", "").replace("<noinclude>", "").replace("</noinclude>", "")

        // FIXME: parseUntil(templateParameterEnd) should be correct. Without it, we don't actually 
        // consume the source until the end of the template parameter. But if we use it, the parser
        // fails for roughly twice as many pages, so for now we deactivate it with "if (true)".
        val nodes = if (source.lastTag("}}}")) List.empty else parseUntil(templateParameterEnd, source, level, templateRedirects)

        TemplateParameterNode(key, nodes, line)
    }

    private def parseTemplate(source : Source, level : Int, templateRedirects: Redirects) : List[Node] =
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
                val nodes = parseUntil(propertyEndOrParserFunctionNameEnd, source, level, templateRedirects)

                val templateName = nodes match
                {
                    case TextNode(text, _, _) :: _ => text
                    case _ => throw new WikiParserException("Invalid Template name", startLine, source.findLine(startLine))
                }

                val decodedName = WikiUtil.cleanSpace(templateName).capitalize(source.language.locale)
                if(source.lastTag(":"))
                {
                    return List(parseParserFunction(decodedName, source, level, templateRedirects))
                }
                title = new WikiTitle(decodedName, Namespace.Template, source.language)
            }
            else
            {
                val propertyNode = parseProperty(source, curKeyIndex.toString, level, templateRedirects)
                properties ::= propertyNode

                if(propertyNode.key == curKeyIndex.toString)
                {
                    curKeyIndex += 1
                }
            }

            //Reached template end?
            if(source.lastTag("}}"))
            {
                return TemplateNode.transform(new TemplateNode(templateRedirects.resolve(title), properties.reverse, startLine))
            }
        }
        
        throw new WikiParserException("Template not closed", startLine, source.findLine(startLine))
    }

    private def parseProperty(source : Source, defaultKey : String, level : Int, templateRedirects: Redirects) : PropertyNode =
    {
        val line = source.line
        var nodes = parseUntil(propertyValueOrEnd, source, level, templateRedirects)
        var key = defaultKey
 
        if(source.lastTag("="))
        {
            //The currently parsed node is a key
            if(nodes.size != 1 || !nodes.head.isInstanceOf[TextNode])
                throw new WikiParserException("Template property key contains invalid elements", line, source.findLine(line))
            
            key = nodes.head.retrieveText.get.trim

            //Parse the corresponding value
            nodes = parseUntil(propertyEnd, source, level, templateRedirects)
        }

        PropertyNode(key, nodes, line)
    }

    private def parseParserFunction(decodedName : String, source : Source, level : Int, templateRedirects: Redirects) : ParserFunctionNode =
    {
        val children = parseUntil(parserFunctionEnd, source, level, templateRedirects)
        val startLine = source.line

        ParserFunctionNode(decodedName, children, startLine)
    }
    
    private def parseTable(source : Source, level : Int, templateRedirects: Redirects) : TableNode =
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
                        nodes ::= TableRowNode(List.empty, source.line)
                        return TableNode(caption, nodes.reverse, line)
                    }
                }
                
                //Parse row
                nodes ::= parseTableRow(source, level, templateRedirects)
    
                //Reached table end?
                if(source.lastTag("|}"))
                {
                    done = true
                }
            }
        }
        
        TableNode(caption, nodes.reverse, line)
    }

    private def parseTableRow(source : Source, level : Int, templateRedirects: Redirects) : TableRowNode =
    {
        val line = source.line
        var nodes = List[TableCellNode]()
        
        while(true)
        {
            //Parse table cell
            nodes ::= parseTableCell(source, level, templateRedirects)

            //Reached row end?
            if(source.lastTag("|}") || source.lastTag("|-"))
            {
                return TableRowNode(nodes.reverse, line)
            }
        }
        
        null
    }

    private def parseTableCell(source : Source, level : Int, templateRedirects: Redirects) : TableCellNode =
    {
        val startPos = source.pos
        val startLine = source.line
        var rowspan = 1
        var colspan = 1
        var nodes = parseUntil(tableCellEnd1, source, level, templateRedirects)

        val lookBack = source.getString(source.pos - 2, source.pos)

        if(lookBack == "\n ")
        {
            source.find(tableCellEnd2)
        }
        else if((lookBack(1) == '|' || lookBack(1) == '!') && lookBack(0) != '\n' && lookBack(0) != '|' && lookBack(0) != '!' && nodes.nonEmpty)
        {
            //This cell contains formatting parameters
            val formattingStr = source.getString(startPos, source.pos - 1).trim

            rowspan = parseTableParam("rowspan", formattingStr)
            colspan = parseTableParam("colspan", formattingStr)

            //Parse the cell contents
            nodes = this.parseUntil(tableCellEnd3, source, level, templateRedirects)
            if(source.lastTag("\n "))
            {
                source.find(tableCellEnd2)
            }
        }
        
        TableCellNode(nodes, startLine, rowspan, colspan)
    }

    private def parseTableParam(name : String, str : String) : Int =
    {
        //Find start index of the value
        var start = str.indexOf(name)
        if(start == -1)
        {
            return 1
        }
        start = str.indexOf('=', start)
        if(start == -1)
        {
            return 1
        }
        start += 1

        //Find end index of the value
        var end = str.indexOf(' ', start)
        if(end == -1)
        {
            end = str.length - 1
        }

        //Convert to integer
        var valueStr = str.substring(start, end + 1)
        valueStr = valueStr.replace("\"", "").trim

        try
        {
            valueStr.toInt
        }
        catch
        {
            case _ : NumberFormatException => 1
        }
    }

    private def parseSection(source : Source, templateRedirects: Redirects) : SectionNode =
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
        val nodes = this.parseUntil(sectionEnd, source, level, templateRedirects)
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
        nodes.last match {
            case lastTextNode: TextNode if lastTextNode.text.endsWith("=") =>
                val cleanNodes = nodes.init :+ lastTextNode.copy(text = lastTextNode.text.dropRight(level - 1))
                return SectionNode(name, level, cleanNodes, source.line - 1);
            case _ =>
        }

        SectionNode(name, level, nodes, source.line - 1)
    }
}
