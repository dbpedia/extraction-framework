package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.util.WikiUtil

import scala.collection.mutable.HashMap
import java.net.URLEncoder
import org.dbpedia.extraction.wikiparser._

class PageContext()
{
    private val uriGenerator = new UriGenerator()

    def generateUri(baseUri : String, node : Node) = uriGenerator.generate(baseUri, node)

    def generateUri(baseUri : String, name : String) = uriGenerator.generate(baseUri, name)
}

private class UriGenerator
{
	var uris = new HashMap[String, Int]()

    //TODO forbid node==null ?
	def generate(baseUri : String, node : Node) : String =
    {
		//TODO This is a 1:1 port from the PHP code and needs (aesthetic) improvement

		var uri : String = null
		
		if(node != null)
		{
	        //Retrieve text
            val sb = new StringBuilder()
	        nodeToString(node, sb)
	
            uri = generate(baseUri, sb.toString)
		}
		else
		{
			uri = generate(baseUri, null : String)
		}

        return uri
    }

    def generate(baseUri : String, name : String) : String =
    {
		//TODO This is a 1:1 port from the PHP code and needs (aesthetic) improvement

		var uri : String = null

		if(name != null)
		{
            var text = name

	        //Normalize text
	        text = WikiUtil.removeWikiEmphasis(text)
	        text = text.replace("&nbsp;", " ")//TODO decode all html entities here
	        text = text.replaceAll(" +", " ") //remove duplicate spaces
	        text = text.replace('(', ' ')
	        text = text.replace(')', ' ')
	        text = text.replaceAll("\\<.*?\\>", "") //strip tags
	        text = text.trim
	        if(text.length > 50) text = text.substring(0, 50)
	        text = text.replace(' ', '_')
	        text = URLEncoder.encode(text, "UTF-8")

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
		else
		{
			uri = baseUri;
		}

        //Resolve collisions
        if(!uris.contains(uri))
        {
            //No collision
            uris(uri) = 1;
        }
        else
        {
            //Collision found
            val index = uris(uri)
            uris(uri) = index + 1
            uri += "__" + index
        }

        return uri
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
