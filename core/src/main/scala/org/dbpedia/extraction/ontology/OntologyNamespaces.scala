package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.{Language, UriUtils}
import java.net.URLDecoder.decode
import scala.collection.mutable.HashMap
import java.util.Locale

/**
 * Manages the ontology namespaces.
 */
object OntologyNamespaces
{
    // TODO: These config settings have nothing to do with ontology namespaces.
    // Move them to some other class. No, better make them configuration parameters. 
  
    //#int
    val genericDomain = Set[String]() // ("en")
    
    val encodeAsURI = Set[String]()


    val DBPEDIA_CLASS_NAMESPACE = "http://dbpedia.org/ontology/"
    val DBPEDIA_DATATYPE_NAMESPACE = "http://dbpedia.org/datatype/"
    val DBPEDIA_PROPERTY_NAMESPACE = "http://dbpedia.org/ontology/"
    val DBPEDIA_SPECIFICPROPERTY_NAMESPACE = "http://dbpedia.org/ontology/"
    //val DBPEDIA_INSTANCE_NAMESPACE = "http://de.dbpedia.org/resource/"
    //val DBPEDIA_GENERAL_NAMESPACE = "http://de.dbpedia.org/property/"

    val OWL_PREFIX = "owl"
    val RDF_PREFIX = "rdf"
    val RDFS_PREFIX = "rdfs"
    val FOAF_PREFIX = "foaf"
    val GEO_PREFIX = "geo"
    val GEORSS_PREFIX = "georss"
    val GML_PREFIX = "gml"
    val XSD_PREFIX = "xsd"
    val DC_PREFIX = "dc"
    val DCT_PREFIX = "dct"
    val DCTERMS_PREFIX = "dcterms"
    val SKOS_PREFIX = "skos"
    val SCHEMA_ORG_PREFIX = "schema"

    val OWL_NAMESPACE = "http://www.w3.org/2002/07/owl#"
    val RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    val RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#" 
    val FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/"
    val GEO_NAMESPACE = "http://www.w3.org/2003/01/geo/wgs84_pos#"
    val GEORSS_NAMESPACE = "http://www.georss.org/georss/"
    val GML_NAMESPACE = "http://www.opengis.net/gml/"
    // Note: "http://www.w3.org/2001/XMLSchema#" is the RDF prefix, "http://www.w3.org/2001/XMLSchema" is the XML namespace URI.
    val XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#"
    val DC_NAMESPACE = "http://purl.org/dc/elements/1.1/"
    val DCT_NAMESPACE = "http://purl.org/dc/terms/"
    val SKOS_NAMESPACE = "http://www.w3.org/2004/02/skos/core#"
    val SCHEMA_ORG_NAMESPACE = "http://schema.org/"

    /**
     * Set of namespaces for which existence of classes or properties is not validated.
     */
    private val nonValidatedNamespaces = Set(
        SCHEMA_ORG_NAMESPACE
    )

    /** 
     * Map containing all supported URI prefixes 
     * TODO: make these configurable.
     */
    private def prefixMap = Map(
        OWL_PREFIX -> OWL_NAMESPACE,
        RDF_PREFIX -> RDF_NAMESPACE,
        RDFS_PREFIX -> RDFS_NAMESPACE, 
        FOAF_PREFIX -> FOAF_NAMESPACE,
        GEO_PREFIX -> GEO_NAMESPACE,
        GEORSS_PREFIX -> GEORSS_NAMESPACE,
        GML_PREFIX -> GML_NAMESPACE,
        XSD_PREFIX -> XSD_NAMESPACE,
        DC_PREFIX -> DC_NAMESPACE,
        DCT_PREFIX -> DCT_NAMESPACE,
        DCTERMS_PREFIX -> DCT_NAMESPACE,
        SKOS_PREFIX -> SKOS_NAMESPACE,
        SCHEMA_ORG_PREFIX -> SCHEMA_ORG_NAMESPACE
    );

    /**
     * Determines the full URI of a name.
     * e.g. foaf:name will be mapped to http://xmlns.com/foaf/0.1/name
     * 
     * FIXME: the language parameter is used to choose between URI/IRI. This is the wrong place for that.
     * 
     * @param The name must be URI-encoded
     * @param $baseUri The base URI which will be used if no prefix (e.g. foaf:) has been found in the given name
     * @param language needed to chose wether a URI or IRI is generated. FIXME: this is the wrong place for that choice.
     * @return string The URI
     */
    def getUri(name : String, baseUri : String, language : Language) : String =
    {
        name.split(":", 2) match
        {
            case Array(prefix, suffix) => prefixMap.get(prefix) match
            {
                case Some(namespace) => appendUri(namespace, suffix, language)  // replace prefix
                case None => appendUri(baseUri, name, language)                 // append "fall-back" baseUri
                // throw new IllegalArgumentException("Unknown prefix " + prefix + " in name " + name);
            }
            case _ => appendUri(baseUri, name, language)
        }
    }

    /**
     * FIXME: the language parameter is used to choose between URI/IRI. This is the wrong place for that.
     */
    def getResource(name : String, language : Language) : String = appendUri(baseUri(language, "resource"), name, language)

    /**
     * FIXME: the language parameter is used to choose between URI/IRI. This is the wrong place for that.
     */
    def getProperty(name : String, language : Language) : String = appendUri(baseUri(language, "property"), name, language)
    
    private def baseUri(lang : Language, path : String) : String = {
        if (genericDomain.contains(lang.wikiCode)) "http://dbpedia.org/"+path+"/" 
        else "http://"+lang.wikiCode+".dbpedia.org/"+path+"/"
    }

    private def appendUri(baseUri : String, encodedSuffix : String, language : Language) : String =
    {
        var uri = baseUri + encodedSuffix
        
        if (baseUri.contains('#') || encodeAsURI.contains(language.wikiCode)) uri
        // FIXME: this cannot really work. It's very hard to correctly encode/decode 
        // a complete URI. Only parts of a URI can be encoded and then combined. 
        // See http://tools.ietf.org/html/rfc2396#section-2.4.2 
        // At this point, it's too late. Known problems:
        // - no distinction between "#" and "%23" - input "http://foo/my%231#bar" becomes "http://foo/my%231%23bar"
        // - no distinction between "/" and "%2F" - input "http://foo/a%2Fb/c" becomes "http://foo/a/b/c"
        // see https://sourceforge.net/mailarchive/message.php?msg_id=28982391 for this list of characters
        else escape(decode(uri, "UTF-8"), "\"#%<>?[\\]^`{|}")
    }

    /**
     * @param str string to process
     * @param chars list of characters that should be percent-encoded if they occur in the string.
     * Must be ASCII characters, i.e. Unicode code points from U+0020 to U+007F (inclusive).
     * This method does not correctly escape characters outside that range.
     * TODO: this method is pretty inefficient. It is used with the same chars all the time, 
     * so we should have an array containing their escaped values and use a lookup table.
     */
    private def escape(str : String, chars : String) : String = {
        val sb = new StringBuilder
        for (c <- str) if (chars.indexOf(c) == -1) sb append c else sb append "%" append c.toInt.toHexString.toUpperCase(Locale.ENGLISH)
        sb.toString
    }      
    
    /**
     * Return true  if the namespace of the given URI is known to be an exception for evaluation (e.g. http://schema.org).
     * Return false if the namespace of the given URI starts with should be validated.
     */
    def skipValidation(name : String) : Boolean =
    {
        // FIXME: the language parameter is used to choose between URI/IRI. This is the wrong place for that.
        val uri = getUri(name, "", Language.Default)
        nonValidatedNamespaces.exists(uri startsWith _)
    }

}
