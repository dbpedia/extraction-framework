package org.dbpedia.extraction.util

import java.net._

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

  def createUri(uri: String): URI ={
    // unescape all \\u escaped characters
    val input = StringEscapeUtils.unescapeJava(uri)

    // Here's the list of characters that we re-encode (see WikiUtil.iriReplacements):
    // "#%<>?[\]^`{|}

    // we re-encode backslashes and we currently can't decode Turtle, so we disallow it
    if (input.contains("\\"))
      throw new IllegalArgumentException("URI contains backslash: [" + input + "]")
    new URI(StringUtils.escape(input, WikiUtil.iriReplacements))
  }

  /**
    * decodes (ASCII) uris and transforms them into iris with the DBpedia naming rules
    *
    * @param uri
    * @return
    */
    def uriToIri(uri: String): String = {
      uriToIri(createUri(uri))
    }

  /**
    * see uriToIri(uri: String)
    *
    * @param uri
    * @return
    */
  def uriToIri(uri: URI): String = {

    if(uri.getScheme == "http" && uri.getPath.startsWith("/resource/") && uri.getHost.replaceAll("([a-z-]+.)?dbpedia.org", "").length == 0)
    {
      // re-encode URI according to our own rules
      uri.getScheme + "://" +
        uri.getAuthority +
        uri.getPath  +
        (if(uri.getQuery != null) "?" + encodeAndClean(uri.getQuery) else "")+
        (if(uri.getFragment != null) "#" + encodeAndClean(uri.getFragment) else "")
    }
    else
      uri.toString
  }

  private def encodeAndClean(uriPart: String): String={
      wikiEncode(cleanSpace(UriDecoder.decode(uriPart).replace('\n', ' ').replace('\t', ' ')))
    }

  def encodeUriComponent(comp: String): String={
    URLEncoder.encode(comp, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
  }
}
