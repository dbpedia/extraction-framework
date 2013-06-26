package org.dbpedia.extraction.util

import java.net.{URISyntaxException, MalformedURLException, URI, URL}

object UriUtils
{
    private val knownSchemes = Set("http", "https", "ftp")
    
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

    /**
     * Encodes the give URI string - overcomes URLEncoder issues
     * @param uri
     * @return Encoded URI representation of the input URI string
     * @throws MalformedURLException if the input uri is not a valid URL
     * @throws URISyntaxException if the input uri is not a valid URI and cannot be encoded
     */
    def encode( uri : String ) : URI =
    {
        def url = new URL(uri)
        new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef())
    }
}
