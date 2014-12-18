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

    // Speedup removal of scheme
    private val httpSchemeRemover = Pattern.compile("http://", Pattern.LITERAL)

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
        val hasScheme = hasKnownScheme(uri)

        // URL() throws an exception if no scheme is specified so we temporarily add it here
        val iri = if (hasScheme) parseIRI(new URL(uri))
                else parseIRI(new URL("http://" + uri))

        // We remove the scheme if manually added in previous step
        if (hasScheme) iri
        else new URI(httpSchemeRemover.matcher(iri.toString).replaceFirst(""))
    }

    /**
     * Returns a URI from a URL. Solve the problem of IDN that are currently not supported from java.net.URI but only from java.net.URL
     * We try to parse the string with URL and check if the domain name is in IDN form and then reconstruct a URI
     *
     * @param url
     * @return Encoded URI representation of the input URI string
     * @throws URISyntaxException if the input uri is not a valid URI and cannot be encoded
     */
    def parseIRI( url : URL ) : URI =
    {
        val safeHost : String = try {
            IDN.toASCII(url.getHost())
        } catch {
            case _ : IllegalArgumentException => url.getHost()
        }

        // If in IDN form, replace the host with IDN.toASCII
        if (safeHost.equals(url.getHost)) {
            url.toURI
        }
        else {
            val start = url.getProtocol + "://" + (if (url.getUserInfo != null) url.getUserInfo + "@" else "")
            new URI(url.toString.replace(start + url.getHost, start + safeHost))
        }
    }
}
