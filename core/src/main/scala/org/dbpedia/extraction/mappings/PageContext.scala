package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.util.WikiUtil

import java.net.URLEncoder
import org.dbpedia.extraction.wikiparser._

/**
 * TODO: move this to PageNode?
 */
class PageContext()
{
    private val uriGenerator = new UriGenerator()

    def generateUri(baseUri : String, node : Node) = uriGenerator.generate(baseUri, node)

    def generateUri(baseUri : String, name : String) = uriGenerator.generate(baseUri, name)
}

private class UriGenerator
{
    var uris = Map[String, Int]()

    def generate(baseUri : String, node : Node) : String =
    {
        node match
        {
            case _ : Node =>
            {
                val sb = new StringBuilder()
                nodeToString(node, sb)
                generate(baseUri, sb.toString())
            }
            case null => generate(baseUri, "") //TODO forbid node==null ?
        }
    }

    def generate(baseUri : String, name : String) : String =
    {
        var uri : String = baseUri

        if(name != "")
        {
            var text = name

            //Normalize text
            text = WikiUtil.removeWikiEmphasis(text)
            text = text.replace("&nbsp;", " ")//TODO decode all html entities here
            text = text.replace('(', ' ')
            text = text.replace(')', ' ')
            text = text.replaceAll("\\<.*?\\>", "") //strip tags
            text = WikiUtil.cleanSpace(text)
            if(text.length > 50) text = text.substring(0, 50)
            text = WikiUtil.wikiEncode(text)

            //Test if the base URI ends with a prefix of text
            var i = baseUri.length - 1
            var done = false
            while(!done && i > 0 && baseUri.length - i < text.length)
            {
                if(baseUri.regionMatches(true, i, text, 0, baseUri.length - i))
                {
                    text = text.substring(baseUri.length - i)
                    done = true
                }

                i -= 1
            }

            //Remove leading underscore
            if(!text.isEmpty && text(0) == '_')
            {
                text = text.substring(1)
            }

            //Generate URI
            uri = baseUri + "__" + text
        }

        val index = uris.getOrElse(uri, 0) + 1
        uris = uris.updated(uri, index)
        uri + "__" + index.toString
    }

    private def nodeToString(node : Node, sb : StringBuilder)
    {
        node match
        {
            case TextNode(text, _) => sb.append(text)
            case null => //ignore
            case _ : TemplateNode => //ignore
            case _ : TableNode => //ignore
            case _ => node.children.foreach(child => nodeToString(child, sb))
        }
    }
}
