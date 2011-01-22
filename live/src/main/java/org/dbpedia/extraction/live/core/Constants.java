package org.dbpedia.extraction.live.core;

import org.ini4j.Options;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 1, 2010
 * Time: 6:47:00 PM
 * This class contains some constants that are used throughout the live extraction process  
 */
public class Constants{
    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
    public static final String RDF_PROPERTY = "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";
    public static final String DBPEDIA_NS = LiveOptions.options.get("dbpedia_ns");
    public static final String DB_META_NS = LiveOptions.options.get("db_meta_ns");
    public static final String DB_PROPERTY_NS = DBPEDIA_NS + "property/";
    public static final String error_character = "\\uFFFD";
    public static final String DBCOMM_ABSTRACT = DB_PROPERTY_NS + "abstract_live";
    public static final String DBCOMM_COMMENT = DB_PROPERTY_NS + "comment_live";
    public static final String STARTSWITH = "startswith";
    public static final String EXACT = "exactmatch";
    public static final String DB_RESOURCE_NS = DBPEDIA_NS + "resource/";
    public static final String DB_YAGO_NS = DBPEDIA_NS + "class/yago/";
    public static final String DB_ONTOLOGY_NS = DBPEDIA_NS + "ontology/";
    public static final String DB_COMMUNITY_NS = DBPEDIA_NS + "ontology/";
    public static final String PAGE_ID = DB_PROPERTY_NS + "pageId";
    public static final String REVISION_ID = DB_PROPERTY_NS + "revisionId";
    public static final String GML_NAMESPACE = "http://www.opengis.net/gml/";


    public static final String RDFS_COMMENT = "http://www.w3.org/2000/01/rdf-schema#comment";


    public static final String OWL_SAMEAS = "http://www.w3.org/2002/07/owl#sameAs";
    public static final String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";

    public static final String DC_MODIFIED = "http://purl.org/dc/terms/modified";
    public static final String DC_DESCRIPTION = "http://purl.org/dc/elements/1.1/description";
    public static final String DC_RIGHTS = "http://purl.org/dc/terms/rights";

    public static final String FOAF_PAGE = "http://xmlns.com/foaf/0.1/page";
    public static final String FOAF_NAME = "http://xmlns.com/foaf/0.1/name";
    public static final String FOAF_GIVENNAME = "http://xmlns.com/foaf/0.1/givenname";
    public static final String FOAF_SURNAME = "http://xmlns.com/foaf/0.1/surname";
    public static final String FOAF_PERSON = "http://xmlns.com/foaf/0.1/Person";
    public static final String FOAF_DEPICTION = "http://xmlns.com/foaf/0.1/depiction";
    public static final String FOAF_THUMBNAIL = "http://xmlns.com/foaf/0.1/thumbnail";
    public static final String FOAF_IMG = "http://xmlns.com/foaf/0.1/img";
    public static final String FOAF_HOMEPAGE = "http://xmlns.com/foaf/0.1/homepage";

    public static final String SKOS_SUBJECT = "http://www.w3.org/2004/02/skos/core#subject";
    public static final String SKOS_PREFLABEL = "http://www.w3.org/2004/02/skos/core#prefLabel";
    public static final String SKOS_CONCEPT = "http://www.w3.org/2004/02/skos/core#Concept";
    public static final String SKOS_BROADER = "http://www.w3.org/2004/02/skos/core#broader";

    public static final String GEORSS_POINT = "http://www.georss.org/georss/point";
    public static final String GEORSS_RADIUS = "http://www.georss.org/georss/radius";

    public static final String WGS_LAT = "http://www.w3.org/2003/01/geo/wgs84_pos#lat";
    public static final String WGS_LONG = "http://www.w3.org/2003/01/geo/wgs84_pos#long";

    public static final String GEO_FEATURECLASS = "http://www.geonames.org/ontology#featureClass";
    public static final String GEO_FEATURECODE = "http://www.geonames.org/ontology#featureCode";
    public static final String GEO_POPULATION = "http://www.geonames.org/ontology#population";

    //should not use macro for now
    public static final String YAGO_LANDMARK =  DB_YAGO_NS + "Landmark108624891";


    /*
     * These seem to be defined already
    */
    public static final String XS_DATETIME = "http://www.w3.org/2001/XMLSchema#dateTime";
    public static final String XS_DATE = "http://www.w3.org/2001/XMLSchema#date";
    public static final String XS_FLOAT = "http://www.w3.org/2001/XMLSchema#float";
    public static final String XS_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
    public static final String XS_DECIMAL = "http://www.w3.org/2001/XMLSchema#decimal";
    
    /*
    *  DBpedia Vocabulary
    * */
    public static final String DB_REDIRECT = DB_PROPERTY_NS + "redirect";
    public static final String DB_ABSTRACT = DB_ONTOLOGY_NS + "abstract";
    public static final String DB_DISAMBIGUATES = DB_PROPERTY_NS + "disambiguates";
    public static final String DB_WIKILINK = DB_PROPERTY_NS + "wikilink";
    public static final String DB_WORDNET_TYPE = DB_PROPERTY_NS + "wordnet_type";
    public static final String DB_CHARACTERCOUNT = DB_PROPERTY_NS + "characterCount";
    public static final String DB_HASPHOTOCOLLECTION = DB_PROPERTY_NS + "hasPhotoCollection";
    public static final String DB_MY_CHEM_PROPERTY = DB_PROPERTY_NS + "my_chem_property";
    public static final String DB_REFERENCE = DB_PROPERTY_NS + "reference";
    public static final String DB_WIKIPAGEUSESTEMPLATE = DB_PROPERTY_NS + "wikiPageUsesTemplate";
    public static final String DB_BIRTH = DB_PROPERTY_NS + "birth";
    public static final String DB_BIRTHPLACE = DB_PROPERTY_NS + "birthPlace";
    public static final String DB_DEATH = DB_PROPERTY_NS + "death";
    public static final String DB_DEATHPLACE = DB_PROPERTY_NS + "deathPlace";
    public static final String DB_CLASS_BOOK = DBPEDIA_NS + "class/Book";
    public static final String DB_WIKIPAGE_EN = DB_PROPERTY_NS + "wikipage-en";

    /*
     * Ontology Vocabulary
     * */
    public static final String DBO_INDIVIDUALISED_PND = DB_ONTOLOGY_NS + "Person/individualisedPnd";
    public static final String DBO_NON_INDIVIDUALISED_PND = DB_ONTOLOGY_NS + "Person/nonIndividualisedPnd";
    public static final String DBO_THUMBNAIL = DB_ONTOLOGY_NS + "thumbnail";
    /*
     * ANNOTATION VOCABULARY:
     *
     * */

    public static final String AXIOM_PREFIX = DB_META_NS + "axiom";
    public static final String OWL_AXIOM = "http://www.w3.org/2002/07/owl#Axiom";
    public static final String OWL_SUBJECT = "http://www.w3.org/2002/07/owl#annotatedSource";
    public static final String OWL_PREDICATE = "http://www.w3.org/2002/07/owl#annotatedProperty";
    public static final String OWL_OBJECT = "http://www.w3.org/2002/07/owl#annotatedTarget";



    /*
     * Meta Vocabulary
     * */
    public static final String DBM_EXTRACTEDFROMTEMPLATE = DB_META_NS + "extractedfromtemplate";
    public static final String DBM_ONDELETECASCADE = DB_META_NS + "sourcepage";
    public static final String DBM_ORIGIN = DB_META_NS + "origin";
    public static final String DBM_SOURCEPAGE = DB_META_NS + "sourcepage";
    public static final String DBM_REVISION = DB_META_NS + "revision";
    //was oaiidentifier
    public static final String DBM_OAIIDENTIFIER = DB_META_NS + "pageid";
    public static final String DBM_EDITLINK = DB_META_NS + "editlink";

    /*
    For Statistics
    */
    public static final String CREATEDTRIPLES = "created_Triples";
    public static final String STAT_TOTAL = "Total";
    
    //Namespaces
    public static final String MW_CATEGORY_NAMESPACE = "Category";
    public static final String MW_FILE_NAMESPACE = "File";
    public static final String MW_FILEALTERNATIVE_NAMESPACE = "FileAlt";
    public static final String MW_TEMPLATE_NAMESPACE = "Template";

    //Used for CategoryLabelExtractor
    public static final String DB_CATEGORY_NS = DB_RESOURCE_NS + MW_CATEGORY_NAMESPACE + ":";

}
