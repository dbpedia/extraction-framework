package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.StringUtils.{replacements,escape}
import java.lang.StringBuilder
import scala.collection.mutable.HashMap

class RdfNamespace(val prefix: String, val namespace: String, val validate: Boolean) {
  
  def append(suffix: String): String =
  {
    val sb = new StringBuilder
    sb.append(namespace)
    append(sb, suffix)
    sb.toString
  }
  
  /**
   * Sub classes may override.
   */
  protected def append(sb: StringBuilder, suffix: String): Unit = {
    // FIXME: this cannot really work. It's very hard to correctly encode/decode
    // a complete URI. Only parts of a URI can be encoded and then combined.
    // See http://tools.ietf.org/html/rfc2396#section-2.4.2
    // At this point, it's too late. Known problems:
    // - no distinction between "#" and "%23" - input "my%231#bar" becomes "my%231%23bar"
    // - no distinction between "/" and "%2F" - input "/foo/a%2Fb/c" becomes "/foo/a/b/c"
    // - similar for all other characters in iriEscapes
    escape(sb, suffix, RdfNamespace.iriEscapes)
  }
  
}

object RdfNamespace {
  
  // for this list of characters, see RFC 3987 and https://sourceforge.net/mailarchive/message.php?msg_id=28982391
  private val iriEscapes = {
    val chars = ('\u0000' to '\u0020').mkString + "\"#%<>?[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    replacements('%', chars)
  }

  private val prefixMap = new HashMap[String, RdfNamespace]
  
  private def ns(prefix: String, namespace: String, map: Boolean, validate: Boolean = true): RdfNamespace = {
    val ns = new RdfNamespace(prefix, namespace, validate)
    if (map) prefixMap(prefix) = ns
    ns
  }
  
  val OWL = ns("owl", "http://www.w3.org/2002/07/owl#", true)
  val RDF = ns("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", true)
  val RDFS = ns("rdfs", "http://www.w3.org/2000/01/rdf-schema#", true)
  val FOAF = ns("foaf", "http://xmlns.com/foaf/0.1/", true)
  val GEO = ns("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#", true)
  val GEORSS = ns("georss", "http://www.georss.org/georss/", true)
  val GML = ns("gml", "http://www.opengis.net/gml/", true)
  // Note: "http://www.w3.org/2001/XMLSchema#" is the RDF prefix, "http://www.w3.org/2001/XMLSchema" is the XML namespace URI.
  val XSD = ns("xsd", "http://www.w3.org/2001/XMLSchema#", true)
  val DC = ns("dc", "http://purl.org/dc/elements/1.1/", true)
  // DCT and DCTERMS: two prefixes for one namespace
  val DCT = ns("dct", "http://purl.org/dc/terms/", true)
  val DCTERMS = ns("dcterms", "http://purl.org/dc/terms/", true)
  val SKOS = ns("skos", "http://www.w3.org/2004/02/skos/core#", true)
  val SCHEMA = ns("schema", "http://schema.org/", true, false) // don't validate
  
  def forName(name: String): Option[RdfNamespace] = name.split(":", 2) match {
    case Array(prefix, suffix) => prefixMap.get(prefix)
    case _ => None
  }

  /**
   * Return true if the namespace of the given name should be validated.
   * Return false if the namespace of the given name is known to be an exception for validation (e.g. http://schema.org).
   */
  def validate(name : String): Boolean = forName(name) match {
    case Some(namespace) => namespace.validate
    case None => true
  }
  
  /**
   * Determines the full URI of a name.
   * e.g. foaf:name will be mapped to http://xmlns.com/foaf/0.1/name
   * 
   * @param name MUST NOT be URI-encoded
   * @param defaultBase base URI which will be used if no prefix (e.g. foaf:) has been found 
   * in the given name
   * @return full URI
   */
  def fullUri(name: String, default: RdfNamespace) : String = name.split(":", 2) match {
    case Array(prefix, suffix) => prefixMap.get(prefix) match {
      // replace prefix
      case Some(namespace) => namespace.append(suffix)
      // use default namespace
      case None => default.append(name)
    }
    case _ => default.append(name)
  }

}
