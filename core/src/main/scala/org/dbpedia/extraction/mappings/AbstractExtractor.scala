package org.dbpedia.extraction.mappings

import java.util.logging.{Logger, Level}
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import java.net.{URLEncoder, URL}
import xml.XML
import io.Source
import java.io.{InputStream, OutputStreamWriter}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts page abstracts.
 *
 * DBpedia-customized MediaWiki instance is required.
 */

class AbstractExtractor( context : {
                             def ontology : Ontology
                             def language : Language } ) extends Extractor
{
    private val maxRetries = 3

    private val timeoutMs = 4000

    private val language = context.language.wikiCode

    private val logger = Logger.getLogger(classOf[AbstractExtractor].getName)

    //TODO make this configurable
    private val apiUrl = "http://localhost/mw-modified/api.php"

    private val apiParametersFormat = "uselang="+language+"&format=xml&action=parse&prop=text&title=%s&text=%s"

    // lazy so testing does not need ontology
    private lazy val shortProperty = context.ontology.getProperty("rdfs:comment")
                                     .getOrElse(throw new Exception("Property 'rdfs:comment' not found"))

    // lazy so testing does not need ontology
    private lazy val longProperty = context.ontology.getProperty("abstract")
                                    .getOrElse(throw new Exception("Property 'abstract' not found"))

    override def extract(pageNode : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        //Only extract abstracts for pages from the Main namespace
        if(pageNode.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        //Don't extract abstracts from redirect and disambiguation pages
        if(pageNode.isRedirect || pageNode.isDisambiguation) return new Graph()

        //Reproduce wiki text for abstract
        val abstractWikiText = getAbstractWikiText(pageNode)
        if(abstractWikiText == "") return new Graph()

        //Retrieve page text
        val text = postProcess( pageNode.title, retrievePage(pageNode.title, abstractWikiText) )

        //Ignore empty abstracts
        if(text.trim.isEmpty) return new Graph()

        //Create a short version of the abstract
        val shortText = short(text)

        //Create statements
        val quadLong = new Quad(context.language, DBpediaDatasets.LongAbstracts, subjectUri, longProperty, text, pageNode.sourceUri)
        val quadShort = new Quad(context.language, DBpediaDatasets.ShortAbstracts, subjectUri, shortProperty, shortText, pageNode.sourceUri)

        if(shortText.isEmpty)
        {
            new Graph(List(quadLong))
        }
        else
        {
            new Graph(List(quadLong, quadShort))
        }
    }


    /**
     * Retrieves a Wikipedia page.
     *
     * @param pageTitle The encoded title of the page
     * @return The page as an Option
     */
    def retrievePage(pageTitle : WikiTitle, pageWikiText : String) : String =
    {
        for(_ <- 1 to maxRetries)
        {
            try
            {
                // Fill parameters
                val parameters = apiParametersFormat.format(pageTitle, URLEncoder.encode(pageWikiText, "UTF-8"))

                // Send data
                val url = new URL(apiUrl)
                val conn = url.openConnection
                conn.setDoOutput(true)
                conn.setConnectTimeout(timeoutMs)
                conn.setReadTimeout(timeoutMs)
                val writer = new OutputStreamWriter(conn.getOutputStream)
                writer.write(parameters)
                writer.flush()
                writer.close()

                // Read answer
                return postProcess(pageTitle, readInAbstract(conn.getInputStream))
            }
            catch
            {
                case ex  : Exception => logger.log(Level.INFO, "Error retrieving abstract of " + pageTitle + ". Retrying...", ex)
            }

            //Thread.sleep(1000)
        }

        throw new Exception("Could not retrieve abstract for page: " + pageTitle)
    }

    /**
     * Returns the first sentences of the given text that have less than 500 characters.
     * A sentence ends with a dot followed by whitespace.
     * TODO: probably doesn't work for most non-European languages.
     * TODO: analyse ActiveAbstractExtractor, I think this works  quite well there,
     * because it takes the first two or three sentences
     * @param text
     * @param max max length
     * @return result string
     */
    def short(text : String, max : Int = 500) : String =
    {
        if (text.size < max) return text

        val builder = new StringBuilder()
        var size = 0

        for(sentence <- text.split("""(?<=\.\s)"""))
        {
            if(size + sentence.size > max)
            {
                if (builder.isEmpty)
                {
                    return sentence
                }
                return builder.toString().trim
            }

            size += sentence.size
            builder.append(sentence)
        }

        builder.toString().trim
    }

    /**
     * Get the parsed and cleaned abstract text from the MediaWiki instance input stream.
     * It returns
     * <api> <parse> <text> ABSTRACT_TEXT </text> </parse> </api>
     */
    private def readInAbstract(inputStream : InputStream) : String =
    {
        // for XML format
        val xmlAnswer = Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("")
        (XML.loadString(xmlAnswer) \ "parse" \ "text").text.trim
    }

    private def postProcess(pageTitle : WikiTitle, text : String) : String =
    {
        def startsWithLowercase(text : String) =
        {
            val firstLetter = text.substring(0,1)
            firstLetter != firstLetter.toUpperCase(context.language.locale)
        }

        //HACK
        if(startsWithLowercase(text))
        {
            val decodedTitle = pageTitle.decoded.replaceFirst(" \\(.+\\)$", "")

            if(!text.toLowerCase.contains(decodedTitle.toLowerCase))
            {
                // happens mainly for Japanese names (abstract starts with template)
                return decodedTitle + " " + text
            }
        }

        text
    }

    private val destinationNamespacesToRender = List(WikiTitle.Namespace.Main, WikiTitle.Namespace.Template)

    private def renderNode(node : Node) = node match
    {
        case InternalLinkNode(destination, _, _, _) => destinationNamespacesToRender contains destination.namespace
        case ParserFunctionNode(_, _, _) => false
        case _ => true
    }


    /**
     * Get the wiki text that contains the abstract text.
     */
    def getAbstractWikiText(pageNode : PageNode) : String =
    {
        // From first TextNode
        val start = pageNode.children.indexWhere{
            case TextNode(text, _) => text.trim != ""
            case InternalLinkNode(destination, _, _, _) => destination.namespace == WikiTitle.Namespace.Main
            case _ => false
        }

        // To first SectionNode (exclusive)
        var end = pageNode.children.indexWhere{
            case sectionNode : SectionNode => true
            case _ => false
        }

        // If there is no SectionNode, To last non-empty TextNode (inclusive)
        if(end == -1)
        {
            val reverseLastTextIndex = pageNode.children.reverse.indexWhere{
                case TextNode(text, _) => text.trim != ""
                case _ => false
            }
            if(reverseLastTextIndex != -1)
            {
                end = pageNode.children.length - reverseLastTextIndex
            }
        }

        // No result if there is no TextNode or no text before a SectionNode
        if(start == -1 || end == -1 || start >= end)
        {
            return ""
        }

        // Re-generate wiki text for found range of nodes
        pageNode.children.slice(start, end)
                .filter(renderNode)
                .map(_.toWikiText())
                .mkString("").trim
    }

}