package org.dbpedia.extraction.destinations.formatters

import java.net.{URI,URISyntaxException}
import org.dbpedia.extraction.util.Language

object UriPolicy {
  
  val SUBJECT = 1
  val PREDICATE = 2
  val OBJECT = 3
  val DATATYPE = 4
  val CONTEXT = 5
  
  val identity: (URI, Int) => URI = { (iri, pos) => iri }

  val uris: (URI, Int) => URI = { (iri, pos) => new URI(iri.toASCIIString) }

  def generic(languages: Language*): (URI, Int) => URI = {
    val generic = languages.map(_.dbpediaDomain).toSet
    
    (iri, pos) => 
      if (generic.contains(iri.getHost)) copy(iri, "dbpedia.org", iri.getRawPath)
      else iri
  }
  
  // throws URISyntaxException 
  private def copy(uri: URI, host: String, path: String): URI = {
    
    val scheme = uri.getScheme
    val user = uri.getRawUserInfo
    val port = uri.getPort
    val query = uri.getRawQuery
    val fragment = uri.getRawFragment
    
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
    if (fragment != null) sb.append('#').append(fragment);
    
    new URI(sb.toString)
  }
  
}