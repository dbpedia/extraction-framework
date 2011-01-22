package org.dbpedia.extraction.util

import java.net.URI

object UriUtils
{
    def cleanLink(uri : URI) : Option[String] =
    {
        if(uri.getScheme != "http" && uri.getScheme != "https" && uri.getScheme != "ftp") return None

        val uriStr = uri.normalize.toASCIIString

        return Some(uriStr)
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
        return path
    }

}
