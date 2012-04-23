package org.dbpedia.extraction.util

import java.net.URI

object UriUtils
{
    private val knownSchemes = Set("http", "https", "ftp")
    
    /**
     * TODO: comment
     * FIXME: toASCIIString converts IRIs to URIs
     */
    def cleanLink( uri : URI ) : Option[String] =
    {
      if (knownSchemes.contains(uri.getScheme)) Some(uri.normalize.toASCIIString) 
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
        if (path eq child ) throw new IllegalArgumentException("["+parent+"] is not a parent directory of ["+child+"]")
        path
    }
}
