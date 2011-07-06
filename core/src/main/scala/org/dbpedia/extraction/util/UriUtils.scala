package org.dbpedia.extraction.util

import java.net.URI;
import java.net.URLDecoder;


object UriUtils
{
    def cleanLink(uri : URI) : Option[String] =
    {
        if(uri.getScheme != "http" && uri.getScheme != "https" && uri.getScheme != "ftp") return None

        val uriStr = uri.normalize.toASCIIString

        Some(uriStr)
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

    def toIRIString(uri:String) : String =
    {
        URLDecoder.decode(uri,"UTF-8").replace(">","%3E")
    }

    def toURIString(uri:String) : String =
    {
        //URLEncoder.encode(uri,"UTF-8")
        WikiUtil.wikiEncode(uri)
    }
}
