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
    val Labels = new Dataset("labels", "Titles of all Wikipedia Articles in the corresponding language.")
    val CategoryLabels = new Dataset("category_labels", "Labels for Categories.")
    val Images = new Dataset("images", "Main image and corresponding thumbnail from Wikipedia article.")
    val GeoCoordinates = new Dataset("geo_coordinates", "Geographic coordinates extracted from Wikipedia.")
    val Persondata = new Dataset("persondata_unredirected", "Information about persons (date and place of birth etc., extracted from the English and German Wikipedia, represented using the FOAF vocabulary.")
    val Pnd = new Dataset("pnd")
    val Redirects = new Dataset("redirects", "Dataset containing redirects between articles in Wikipedia.")
    val ArticleCategories = new Dataset("article_categories", "Links from concepts to categories using the SKOS vocabulary.")
    val ArticleTemplates = new Dataset("article_templates")
    val SkosCategories = new Dataset("skos_categories", "Information which concept is a category and how categories are related using the SKOS Vocabulary.")
    val RevisionUris = new Dataset("revision_uris", "Dataset linking DBpedia resource to the specific Wikipedia article revision used in this DBpedia release.")
    val RevisionIds = new Dataset("revision_ids", "Dataset linking a DBpedia resource to the revision ID of the Wikipedia article the data was extracted from. Until DBpedia 3.7, these files had names like 'revisions_en.nt'. Since DBpedia 3.9, they were renamed to 'revisions_ids_en.nt' to distinguish them from the new 'revision_uris_en.nt' files.")
    val RevisionMeta = new Dataset("revision_meta", "Dataset containing additional revision information")
    val PageIds = new Dataset("page_ids", "Dataset linking a DBpedia resource to the page ID of the Wikipedia article the data was extracted from.")
    val InterLanguageLinks = new Dataset("interlanguage_links", "Dataset linking a DBpedia resource to the same resource in other languages and in ((http:www.wikidata.org Wikidata,,. Since the inter-language links were moved from Wikipedia to Wikidata, we now extract these links from the Wikidata dump, not from Wikipedia pages.")
    val Genders = new Dataset("genders")
    val TopicalConcepts = new Dataset("topical_concepts_unredirected")
    val IriSameAsUri = new Dataset("iri_same_as_uri", "owl:sameAs links between the ((http:tools.ietf.org/html/rfc3987 IRI,, and ((http:tools.ietf.org/html/rfc3986 URI,, format of DBpedia resources. Only extracted when IRI and URI are actually different.")
    val FlickrWrapprLinks = new Dataset("flickr_wrappr_links")
    val PageLength = new Dataset("page_length", "Numbers of characters contained in a Wikipedia article's source.")
    val ImageGalleries = new Dataset("image_galleries")
    val ImageAnnotations = new Dataset("image_annotations")
    val KMLFiles = new Dataset("kml_files")
    val AnchorText = new Dataset("anchor_text", "Texts used in links to refer to Wikipedia articles from other Wikipedia articles.")
    val SurfaceForms = new Dataset("surface_forms", "Texts used to refer to Wikipedia articles. Includes the anchor texts data, the names of redirects pointing to an article and the actual article name.")

    /**
     * Mapping based
     */
    val OntologyTypes = new Dataset("instance_types", "Contains triples of the form $object rdf:type $class from the mapping-based extraction.")
    val OntologyTypesTransitive = new Dataset("instance_types_transitive", "Contains transitive rdf:type $class based on the DBpedia ontology.")
    val OntologyProperties = new Dataset("mappingbased_properties_uncleaned_unredirected", "Errors detected in the mapping based properties. At them moment the errors are limited to ranges that are disjoint with the property definition.")   //TODO changes here should be reflected to the related wikidata dataset
    val OntologyPropertiesGeo = new Dataset("geo_coordinates_mappingbased")
    val SpecificProperties = new Dataset("specific_mappingbased_properties", "Infobox data from the mapping-based extraction, using units of measurement more convenient for the resource type, e.g. square kilometres instead of square metres for the area of a city.")

    /**
     * French population template
     */
     val FrenchPopulation = new Dataset("french_population", "French dataset about population.")

    /**
     *  Infobox
     */
    val InfoboxProperties = new Dataset("infobox_properties_unredirected", "Information that has been extracted from Wikipedia infoboxes. Note that this data is in the less clean /property/ namespace. The Mapping-based Properties (/ontology/ namespace, should always be preferred over this data.")
    val InfoboxPropertyDefinitions = new Dataset("infobox_property_definitions", "All properties / predicates used in infoboxes.")
    val TemplateParameters = new Dataset("template_parameters")
    val InfoboxTest = new Dataset("infobox_test")

    /**
     * Abstracts
     */
    val ShortAbstracts = new Dataset("short_abstracts", "Short Abstracts (max. 500 characters long, of Wikipedia articles.")
    val LongAbstracts = new Dataset("long_abstracts", "Full abstracts of Wikipedia articles, usually the first section.")
    val MissingShortAbstracts = new Dataset("missing_short_abstracts")
    val MissingLongAbstracts = new Dataset("missing_long_abstracts")

    /**
     * Links
     */
    val LinksToWikipediaArticle = new Dataset("wikipedia_links", "Dataset linking DBpedia resource to corresponding article in Wikipedia.")
    val ExternalLinks = new Dataset("external_links", "Links to external web pages about a concept.")
    val PageLinks = new Dataset("page_links_unredirected", "Dataset containing internal links between DBpedia instances. The dataset was created from the internal links between Wikipedia articles. The dataset might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms.")
    val DisambiguationLinks  = new Dataset("disambiguations_unredirected", "Links extracted from Wikipedia ((http:en.wikipedia.org/wiki/Wikipedia:Disambiguation disambiguation,, pages. Since Wikipedia has no syntax to distinguish disambiguation links from ordinary links, DBpedia has to use heuristics.")
    val Homepages = new Dataset("homepages", "Links to homepages of persons, organizations etc.")
    val OutDegree = new Dataset("out_degree", "Number of links emerging from a Wikipedia article and pointing to another Wikipedia article.")


    /**
     * Files
     */
    val FileInformation = new Dataset("file_information")


    /**
     * Wikidata
     */
    val WikidataLabelsMappingsWiki = new Dataset("labels_mappingswiki")
    val WikidataLabelsRest = new Dataset("labels_rest")
    val WikidataSameAs = new Dataset("sameas_all_wikis")
    val WikidataNameSpaceSameAs = new Dataset("sameas_wikidata")
    val WikidataSameAsExternal = new Dataset("sameas_external")
    val WikidataAliasMappingsWiki = new Dataset("alias_mappingswiki")
    val WikidataAliasRest = new Dataset("alias_rest")
    val WikidataDescriptionMappingsWiki = new Dataset("description_mappingswiki")
    val WikidataDescriptionRest = new Dataset("description_rest")
    val WikidataProperty= new Dataset("properties")
    val WikidataR2R = new Dataset("mappingbased_properties_uncleaned_unredirected")  // keep same name with other languages
    val WikidataReifiedR2R = new Dataset("mappingbased_properties_reified") // keep same name with other languages
    val WikidataReifiedR2RQualifier= new Dataset("mappingbased_properties_reified_qualifiers") // keep same name with other languages
    val WikidataRaw = new Dataset("raw_unredirected")
    val WikidataRawReified = new Dataset("raw_reified")
    val WikidataRawReifiedQualifiers = new Dataset("raw_reified_qualifiers")
    val WikidataReference = new Dataset("references")

    /**
     * Citations
     */
    val CitationLinks = new Dataset("citation_links")
    val CitationData = new Dataset("citation_data")
    val CitationTypes = new Dataset("citation_types")
}
