package org.dbpedia.extraction.util

import java.net.{URISyntaxException, MalformedURLException, URI, URL, IDN}

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

    /**
     * Returns a URI from a string. Solve the problem of IDN that are currently not supported from java.net.URI but only from java.net.URL
     * We try to parse the string with URL and check if the domain name is in IDN form and then reconstruct a URI
     *
     * @param uri
     * @return Encoded URI representation of the input URI string
     * @throws MalformedURLException if the input uri is not a valid URL
     * @throws URISyntaxException if the input uri is not a valid URI and cannot be encoded
     */
    def parseIRI( uri : String ) : URI =
    {
        val uriWithScheme = if (hasKnownScheme(uri)) uri else "http://" + uri
        val url = new URL(uriWithScheme)
        val safeHost : String = try {
            IDN.toASCII(url.getHost())
        } catch {
            case _ : IllegalArgumentException => url.getHost()
        }

        // If in IDN form, replace the host with IDN.toASCII
        // We do not add the scheme here since it could produce false URIs
        if (safeHost.equals(url.getHost)) {
            new URI(uri)
        }
        else {
            new URI(uri.replace(url.getHost, safeHost))
        }
    }
}
