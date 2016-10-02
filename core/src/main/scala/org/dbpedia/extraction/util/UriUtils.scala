package org.dbpedia.extraction.util

import java.net._
import java.util.regex.Pattern

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.extraction.util.WikiUtil._
import org.dbpedia.util.text.uri.UriDecoder

object UriUtils
{
    private val knownSchemes = Set("http", "https", "ftp")

    private val knownPrefixes = knownSchemes.map(_ + "://")

    def hasKnownScheme(uri: String) : Boolean = knownPrefixes.exists(uri.startsWith(_))

    /**
     * TODO: comment
     */
    def cleanLink( uri : URI ) : Option[String] =
    {
      if (knownSchemes.contains(uri.getScheme)) Some(uri.normalize.toString) 
      else None
    }

    /**
     * Relativizes the given parent URI against a child URI.
     *
     * @param parent
     * @param child
     * @return path from parent to child
     * @throws IllegalArgumentException if parent is not a parent directory of child.
     */
    def relativize( parent : URI, child : URI ) : URI =
    {
        val path = parent.relativize(child)
        if (path eq child) throw new IllegalArgumentException("["+parent+"] is not a parent directory of ["+child+"]")
        path
    }

    private val DBPEDIA_URI = "^http://([a-z-]+.)?dbpedia.org/resource/.*$".r.pattern

  /**
    * decodes (ASCII) uris and transforms them into iris with the DBpedia naming rules
    * @param uri
    * @return
    */
    def uriToIri(uri: String): String = {
        //TODO this needs some further testing
        if (DBPEDIA_URI.matcher(uri).matches()) {

            // unescape all \\u escaped characters
            val input = StringEscapeUtils.unescapeJava(uri)

            // Here's the list of characters that we re-encode (see WikiUtil.iriReplacements):
            // "#%<>?[\]^`{|}

            // we re-encode backslashes and we currently can't decode Turtle, so we disallow it
            if (input.contains("\\")) throw new IllegalArgumentException("URI contains backslash: [" + input + "]")

            // decoding the whole URI is ugly, but should work for us.
            var decoded = UriDecoder.decode(input)

            decoded = cleanSpace(decoded)
            decoded = decoded.replace('\n', ' ')
            decoded = decoded.replace('\t', ' ')

            // re-encode URI according to our own rules
            wikiEncode(decoded)
        }
        else
            uri
    }
}
