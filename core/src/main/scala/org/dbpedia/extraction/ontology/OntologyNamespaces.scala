package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.UriUtils
import java.net.URLEncoder
/**
 * Manages the ontology namespaces.
 */
object OntologyNamespaces
{
    //#int
    val specificLanguageDomain = Set("de", "el", "it", "ru")
    val encodeAsIRI = Set("de", "el", "ru")

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
     * @param The name must be URI-encoded
     * @param $baseUri The base URI which will be used if no prefix (e.g. foaf:) has been found in the given name
     * @return string The URI
     */
    def getUri(name : String, baseUri : String) : String =
    {
    	val parts = name.split(":", 2)

        if (parts.length == 2)
        {
            val prefix = parts(0);
            val suffix = parts(1);
            
            prefixMap.get(prefix) match
            {
            	case Some(namespace) => appendUri(namespace, suffix)
            	case None => appendUri(baseUri, name)
            	// throw new IllegalArgumentException("Unknown prefix " + prefix + " in name " + name);
            }
        }
        else
        {
            appendUri(baseUri, name)
        }
    }

    def getResource(name : String, lang : String) : String =
    {
        val domain = if ( specificLanguageDomain.contains(lang)) "http://" + lang + ".dbpedia.org/resource/" else "http://dbpedia.org/resource/"
        appendUri(domain, name, lang)
    }

    def getProperty(name : String, lang : String) : String =
    {
        val domain = if ( specificLanguageDomain.contains(lang)) "http://" + lang + ".dbpedia.org/property/" else "http://dbpedia.org/property/"
        appendUri(domain, name, lang)
    }

    private def appendUri( baseUri : String, suffix : String, lang : String = "" ) : String =
    {
        if (!baseUri.contains('#'))
        {
            //return baseUri + suffix;
            if (! encodeAsIRI.contains(lang))
                // some bad URIs passed 
                baseUri + URLEncoder.encode(suffix, "UTF-8")
            else
                UriUtils.toIRIString(baseUri+suffix)
        }
        else
        {
            // fragments must not contain ':' or '/', according to our Validate class
            baseUri + suffix.replace("/", "%2F").replace(":", "%3A")
        }
    }

    /**
     * Return true  if the namespace of the given URI is known to be an exception for evaluation (e.g. http://schema.org).
     * Return false if the namespace of the given URI starts with should be validated.
     */
    def skipValidation(uri : String) : Boolean =
    {
        OntologyNamespaces.nonValidatedNamespaces.exists(getUri(uri, "") startsWith _)
    }

}
