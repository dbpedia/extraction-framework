package org.dbpedia.extraction.destinations.formatters

import java.net.URI
import org.dbpedia.extraction.util.Language
import scala.xml.Utility.{isNameChar,isNameStart}

object UriPolicy {
  
  /**
   * A policy takes a URI and its position in the quad and may return the given URI
   * or a transformed version of it. Human-readable type alias.
   */
  type Policy = (URI, Int) => URI
    
  /**
   * A predicate decides if a policy should be applied for the given DBpedia URI.
   * Human-readable type alias.
   */
  type Predicate = URI => Boolean
  
  val SUBJECT = 1
  val PREDICATE = 2
  val OBJECT = 3
  val DATATYPE = 4
  val CONTEXT = 5
  
  val identity: Policy = { (iri, _) => iri }

  def uris(activeFor: Predicate): Policy = {
    
    (iri, _) => 
    if (activeFor(iri)) {
      new URI(iri.toASCIIString)
    }
    else {
      iri
    }
  }

  def generic(activeFor: Predicate): Policy = {
    
    (iri, _) =>
    if (activeFor(iri)) {
      
      val scheme = iri.getScheme
      val user = iri.getRawUserInfo
      val host = "dbpedia.org"
      val port = iri.getPort
      var path = iri.getRawPath
      var query = iri.getRawQuery
      var frag = iri.getRawFragment
      
      uri(scheme, user, host, port, path, query, frag)
    }
    else {
      iri
    }
  }
  
  /**
   * Check if the tail of the URI could be used as an XML element name. If not, attach an
   * underscore (which is a valid XML name). The resulting URI is guaranteed to be usable
   * as a predicate in RDF/XML - it can be split into a valid namespace URI and a valid XML name.
   * 
   * Examples:
   * 
   * original URI       xml safe URI (may be equal)   possible namespace and name in RDF/XML
   * 
   * http://foo/bar          http://foo/bar           http://foo/           bar
   * http://foo/123          http://foo/123_          http://foo/123        _
   * http://foo/%22          http://foo/%22_          http://foo/%22        _
   * http://foo/%C3%BC       http://foo/%C3%BC_       http://foo/%C3%BC     _
   * http://foo/%C3%BCD      http://foo/%C3%BCD       http://foo/%C3%BC     D
   * http://foo/%            http://foo/%22_          http://foo/%22        _
   * http://foo/bar_(fub)    http://foo/bar_(fub)_    http://foo/bar_(fub)  _
   * http://foo/bar#a123     http://foo/bar#a123      http://foo/bar#       a123
   * http://foo/bar#123      http://foo/bar#123_      http://foo/bar#123    _
   * http://foo/bar#         http://foo/bar#_         http://foo/bar#       _
   * http://foo/bar?a123     http://foo/bar?a123      http://foo/bar?       a123
   * http://foo/bar?a=       http://foo/bar?a=_       http://foo/bar?a=     _
   * http://foo/bar?a=b      http://foo/bar?a=b       http://foo/bar?a=     b
   * http://foo/bar?123      http://foo/bar?123_      http://foo/bar?123    _
   * http://foo/bar?         http://foo/bar?_         http://foo/bar?       _
   * http://foo/             http://foo/_             http://foo/           _
   * http://foo              http://foo/_             http://foo/           _
   * http://foo/:            http://foo/:_            http://foo/:          _
   * http://foo/a:           http://foo/a:_           http://foo/a:         _
   * http://foo/a:b          http://foo/a:b           http://foo/a:         b
   */
  def xmlSafe(activeFor: Predicate): Policy = {
    
    (iri, pos) =>
    if (pos == PREDICATE && activeFor(iri)) {
      
      val scheme = iri.getScheme
      val user = iri.getRawUserInfo
      val host = iri.getHost
      val port = iri.getPort
      var path = iri.getRawPath
      var query = iri.getRawQuery
      var frag = iri.getRawFragment
      
      if (frag != null) frag = xmlSafe(frag)
      else if (query != null) query = xmlSafe(query)
      else if (path != null && path.nonEmpty) path = xmlSafe(path)
      else path = "/_" // convert empty path to "/_"
      
      uri(scheme, user, host, port, path, query, frag)
    }
    else {
      iri
    }
  }
  
  /**
   * Check if the tail of the string could be used as an XML element name. 
   * If not, attach an underscore (which is a valid XML name).
   */
  private def xmlSafe(tail: String): String = {
    
    // Go through tail from back to front, find minimal safe part.
    var index = tail.length
    while (index > 0) {
      
      index -= 1
      
      // If char is part of a %XX sequence, we can't split the URI into a namespace and a name.
      // Note: We know it's a valid IRI. Otherwise we'd need more checks here. 
      if (index >= 2 && tail.charAt(index - 2) == '%') return tail+'_'
      
      val ch = tail.charAt(index)
      
      // If char is not valid as part of a name, we can't use the tail as a name.
      // Note: isNameChar allows ':', but we're stricter.
      if (ch == ':' || ! isNameChar(ch)) return tail+'_'
      
      // If char is valid as start of a name, we can use this part as a name.
      if (isNameStart(ch)) return tail
    }
    
    // We can't use the string as an XML name.
    return tail+'_'
  }
  
  def xmlName(tail: String): String = {
    tail.substring(xmlNameStart(tail))
  }
    
  private def xmlNameStart(tail: String): Int = {
    
    // Go through tail from back to front, find minimal safe part.
    var index = tail.length
    var start = index 
    while (index > 0) {
      
      index -= 1
      
      // If char is part of a %XX sequence, we can't split the URI into a namespace and a name.
      // Note: We know it's a valid IRI. Otherwise we'd need more checks here. 
      if (index >= 2 && tail.charAt(index - 2) == '%') return start
      
      val ch = tail.charAt(index)
      
      // If char is not valid as part of a name, we can't use the tail as a name.
      // Note: isNameChar allows ':', but we're stricter.
      if (ch == ':' || ! isNameChar(ch)) return start
      
      // If char is valid as start of a name, we can use this part as a name.
      if (isNameStart(ch)) start = index
    }
    
    // We can't use the string as an XML name.
    return start
  }
  
  private def uri(scheme: String, user: String, host: String, port: Int, path: String, query: String, frag: String): URI = {
    
    val sb = new StringBuilder
    
    if (scheme != null) sb append scheme append ':'
    
    if (host != null) {
      sb.append("//");
      if (user != null) sb.append(user).append('@');
      sb.append(host);
      if (port != -1) sb.append(':').append(port);
    }
    
    if (path != null) sb.append(path);
    if (query != null) sb.append('?').append(query);
    if (frag != null) sb.append('#').append(frag);
    
    new URI(sb.toString)
  }
  
}