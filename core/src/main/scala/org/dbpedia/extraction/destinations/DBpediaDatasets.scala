package org.dbpedia.extraction.destinations

/**
 * Defines the datasets which are extracted by DBpedia.
 * TODO: add references to the extractor classes.
 */
object DBpediaDatasets
{
    /**
     * General
     */
    val Labels = new Dataset("labels")
    val CategoryLabels = new Dataset("category_labels")
    val Images = new Dataset("images")
    val GeoCoordinates = new Dataset("geo_coordinates")
    val Persondata = new Dataset("persondata")
    val Pnd = new Dataset("pnd")
    val Redirects = new Dataset("redirects")
    val ArticleCategories = new Dataset("article_categories")
    val ArticleTemplates = new Dataset("article_templates")
    val SkosCategories = new Dataset("skos_categories")
    val RevisionUris = new Dataset("revision_uris")
    val RevisionIds = new Dataset("revision_ids")
    val RevisionMeta = new Dataset("revision_meta")
    val PageIds = new Dataset("page_ids")
    val InterLanguageLinks = new Dataset("interlanguage_links")
    val Genders = new Dataset("genders")
    val TopicalConcepts = new Dataset("topical_concepts")
    val IriSameAsUri = new Dataset("iri_same_as_uri")
    val FlickrWrapprLinks = new Dataset("flickr_wrappr_links")

    /**
     * Mapping based
     */
    val OntologyTypes = new Dataset("instance_types")
    val OntologyProperties = new Dataset("mappingbased_properties")
    val SpecificProperties = new Dataset("specific_mappingbased_properties")
    
    /**
     * French population template
     */
     val FrenchPopulation = new Dataset("french_population")

    /**
     *  Infobox
     */
    val InfoboxProperties = new Dataset("infobox_properties")
    val InfoboxPropertyDefinitions = new Dataset("infobox_property_definitions")
    val TemplateParameters = new Dataset("template_parameters")
    val InfoboxTest = new Dataset("infobox_test")

    /**
     * Abstracts
     */
    val ShortAbstracts = new Dataset("short_abstracts")
    val LongAbstracts = new Dataset("long_abstracts")

    /**
     * Links
     */
    val LinksToWikipediaArticle = new Dataset("wikipedia_links")
    val ExternalLinks = new Dataset("external_links")
    val PageLinks = new Dataset("page_links")
    val DisambiguationLinks  = new Dataset("disambiguations")
    val Homepages = new Dataset("homepages")
    
    

    /**
     * Wikidata outputDatasets
     */
     
    //for the dummy wikidata Extractor skeleton file
    val Wikidata = new Dataset("wikidata")
    
    //language links dump in the form of
    //<http://L1.dbpedia.org/resource/X2> <http://www.w3.org/2002/07/owl#sameAs> <http://L2.dbpedia.org/resource/X2> .
    val WikidataLL = new Dataset("wikidata-ll")
    
    //Multi lingual labels triples 
    //<http://wikidata.dbpedia.org/resource/Q549> <http://www.w3.org/2000/01/rdf-schema#label> "Bojovnica pestrá"@sk .
    val WikidataLabels = new Dataset("wikidata-labels")
    
    //mappings between Wikidata entities inside DBpedia and DBpedia entities using the owl:sameas property
    //<http://wikidata.dbpedia.org/resource/Q1934> <http://www.w3.org/2002/07/owl#sameAs> <http://ar.dbpedia.org/resource/سيدني_غوفو> .
    val WikidataSameAs = new Dataset("wikidata-sameas")
    
    //Mapping between Wikidata Entities URIs and Their Equivalent URIs used in DBpedia 
    //<http://wikidata.dbpedia.org/resource/Q18> <http://www.w3.org/2002/07/owl#sameAs> <http://wikidata.org/entity/Q18> .
    val WikidataNameSpaceSameAs = new Dataset("wikidata-namespace-sameas")
    
    // wikidata facts triples 
    val WikidataFacts = new Dataset("wikidata")
    //wikidata facts triples with mapped properties to DBpedia ones 
    val WikidataMappedFacts = new Dataset("wikidata-mapped")


}
