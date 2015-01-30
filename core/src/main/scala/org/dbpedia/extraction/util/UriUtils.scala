package org.dbpedia.extraction.util

import java.net._
import java.util.regex.Pattern

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

}
