package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.StringUtils.{replacements,escape}
import java.lang.StringBuilder
import scala.collection.mutable.HashMap

/**
 * @param prefix may be null
 */
class RdfNamespace(val prefix: String, val namespace: String, val validate: Boolean) extends java.io.Serializable {
  
  // TODO: rename to '+'? Or add alias method '+'?
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
    escape(sb, suffix, RdfNamespace.iriEscapes)
  }
  
}

object RdfNamespace {
  
  // for this list of characters, see RFC 3987 and https://sourceforge.net/mailarchive/message.php?msg_id=28982391
  private val iriEscapes = {
    val chars = ('\u0000' to '\u0020').mkString + "\"#%<>?[\\]^`{|}" + ('\u007F' to '\u009F').mkString
    replacements('%', chars)
  }

  val prefixMap = new HashMap[String, RdfNamespace]
  
  private def ns(prefix: String, namespace: String, validate: Boolean = true): RdfNamespace = {
    val ns = new RdfNamespace(prefix, namespace, validate)
    prefixMap(prefix) = ns
    ns
  }

  def getNamespace(uri: String): RdfNamespace ={
    for(p <- prefixMap)
      if(uri.startsWith(p._2.namespace))
        return p._2
    null
  }

  def resolvePrefix(uri: String): String ={
    val parts = split(null, uri)
    if(parts._1 != null)
      parts._1.append(parts._2)
    else
      uri
  }
  
  // FIXME: move these to mappings wiki
  
  val OWL = ns("owl", "http://www.w3.org/2002/07/owl#")
  val RDF = ns("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
  val RDFS = ns("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
  val FOAF = ns("foaf", "http://xmlns.com/foaf/0.1/")
  val GEO = ns("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#")
  val GEORSS = ns("georss", "http://www.georss.org/georss/")
  val GML = ns("gml", "http://www.opengis.net/gml/")
  // Note: "http://www.w3.org/2001/XMLSchema#" is the RDF prefix, "http://www.w3.org/2001/XMLSchema" is the XML namespace URI.
  val XSD = ns("xsd", "http://www.w3.org/2001/XMLSchema#")
  val DC = ns("dc", "http://purl.org/dc/elements/1.1/")
  // DCT and DCTERMS: two prefixes for one namespace
  val DCT = ns("dct", "http://purl.org/dc/terms/")
  val DCTERMS = ns("dcterms", "http://purl.org/dc/terms/")
  val SKOS = ns("skos", "http://www.w3.org/2004/02/skos/core#")
  val SCHEMA = ns("schema", "http://schema.org/", false) 
  val BIBO = ns("bibo", "http://purl.org/ontology/bibo/", false)
  val WIKIDATA = ns("wikidata", "http://www.wikidata.org/entity/", false)
  val MAPPINGS = ns("mappings", "http://mappings.dbpedia.org/wiki/", false)
  val D0 = ns("d0", "http://www.ontologydesignpatterns.org/ont/d0.owl#", false)
  val DUL = ns("dul", "http://www.ontologydesignpatterns.org/ont/dul/DUL.owl#", false)
  val RR = ns("rr", "http://www.w3.org/ns/r2rml#")
  val RML = ns("rml", "http://semweb.mmlab.be/ns/rml#")
  val QL = ns("ql", "http://semweb.mmlab.be/ns/ql#")
  val DBO = ns("dbo", "http://dbpedia.org/ontology/")
  val FNML = ns("fnml", "http://semweb.mmlab.be/ns/fnml#")
  val FNO = ns("fno", "http://w3id.org/function/ontology#")
  val DBF = ns("dbf", "http://dbpedia.org/function/")
  val CRML = ns("crml", "http://semweb.mmlab.be/ns/crml#")
  val NIF = ns("nif", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#")
  val HTML = ns("html", "http://www.w3.org/1999/xhtml/")

  //the following namespaces are required for supporting the entries in the mappings Wiki as of 2014-07-15
  val CIDOCCRM = ns("cidoccrm", "http://purl.org/NET/cidoc-crm/core#", false)
  val BIO = ns("bio", "http://purl.org/vocab/bio/0.1/", false)
  
  // doing the new global mappings
  val CEO = ns("ceo","https://linkeddata.cultureelerfgoed.nl/vocab/def/ceo#",false)
  val BAG = ns("bag","http://bag.basisregistraties.overheid.nl/def/bag#",false)
  val DBLP2 = ns("dblp2","https://dblp.org/rdf/schema-2020-07-01#",false)
  val bibschema = ns("bibschema","https://bib.schema.org/",false)
  val GN = ns("gn","http://www.geonames.org/ontology#",false)
  val MO = ns("mo","http://purl.org/ontology/mo/",false)
  val GND = ns("gnd","https://d-nb.info/standards/elementset/gnd#",false)
  val RKD = ns("rkd","http://data.rkd.nl/def#",false)

  /**
   * @return namespace for prefix and suffix, default namespace and full name if no match found 
   */
  def split(default: RdfNamespace, name: String): (RdfNamespace, String) = {
    val parts = name.split(":", 2) // TODO: use name.split(":", -1) instead???
    if (parts.size == 2 && prefixMap.contains(parts(0)))
      (prefixMap(parts(0)), parts(1))
    else {
      val nss = getNamespace(name)
      if(nss != null)
        (nss, name.substring(nss.namespace.length))
      else
        (default, name)
    }
  }
  
  /**
   * Return true if the namespace of the given name should be validated.
   * Return false if the namespace of the given name is known to be an exception for validation (e.g. http://schema.org).
   */
  def validate(name : String): Boolean = {
    val (namespace, _) = split(null, name)
    namespace != null && namespace.validate
  }
  
  /**
   * Determines the full URI of a name.
   * e.g. foaf:name will be mapped to http://xmlns.com/foaf/0.1/name
   * 
   * FIXME: this method doesn't work well - too many defaults, too loose checking:
   * fullUri("http://foo", RdfNamespace.SCHEMA) returns "http://schema.org/http://foo"
   *  
   * @param name MUST NOT be URI-encoded
   * @param default base URI which will be used if no prefix (e.g. foaf:) has been found 
   * in the given name
   * @return full URI
   */
  def fullUri(default: RdfNamespace, name: String): String = { 
    val (namespace, rest) = split(default, name)
    namespace.append(rest)
  }
}
