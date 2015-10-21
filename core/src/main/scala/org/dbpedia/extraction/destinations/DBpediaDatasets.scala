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
    val Persondata = new Dataset("persondata_unredirected")
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
    val TopicalConcepts = new Dataset("topical_concepts_unredirected")
    val IriSameAsUri = new Dataset("iri_same_as_uri")
    val FlickrWrapprLinks = new Dataset("flickr_wrappr_links")
    val PageLength = new Dataset("page_length")
    val ImageGalleries = new Dataset("image_galleries")
    val ImageAnnotations = new Dataset("image_annotations")
    val KMLFiles = new Dataset("kml_files")
    val AnchorText = new Dataset("anchor_text")
    val SurfaceForms = new Dataset("surface_forms")

    /**
     * Mapping based
     */
    val OntologyTypes = new Dataset("instance_types")
    val OntologyTypesTransitive = new Dataset("instance_types_transitive")
    val OntologyPropertiesObjects = new Dataset("mappingbased_objects_uncleaned_unredirected")   //TODO changes here should be reflected to the related wikidata dataset
    val OntologyPropertiesLiterals = new Dataset("mappingbased_literals")
    val OntologyPropertiesGeo = new Dataset("geo_coordinates_mappingbased")
    val SpecificProperties = new Dataset("specific_mappingbased_properties")

    /**
     * French population template
     */
     val FrenchPopulation = new Dataset("french_population")

    /**
     *  Infobox
     */
    val InfoboxProperties = new Dataset("infobox_properties_unredirected")
    val InfoboxPropertyDefinitions = new Dataset("infobox_property_definitions")
    val TemplateParameters = new Dataset("template_parameters")
    val InfoboxTest = new Dataset("infobox_test")

    /**
     * Abstracts
     */
    val ShortAbstracts = new Dataset("short_abstracts")
    val LongAbstracts = new Dataset("long_abstracts")
    val MissingShortAbstracts = new Dataset("missing_short_abstracts")
    val MissingLongAbstracts = new Dataset("missing_long_abstracts")

    /**
     * Links
     */
    val LinksToWikipediaArticle = new Dataset("wikipedia_links")
    val ExternalLinks = new Dataset("external_links")
    val PageLinks = new Dataset("page_links_unredirected")
    val DisambiguationLinks  = new Dataset("disambiguations_unredirected")
    val Homepages = new Dataset("homepages")
    val OutDegree = new Dataset("out_degree")


    /**
     * Files
     */
    val FileInformation = new Dataset("file_information")


    /**
     * Wikidata
     */
    val WikidataLabelsMappingsWiki = new Dataset("labels")
    val WikidataLabelsRest = new Dataset("labels-nmw")
    val WikidataSameAs = new Dataset("sameas-all-wikis")
    val WikidataNameSpaceSameAs = new Dataset("sameas-wikidata")
    val WikidataSameAsExternal = new Dataset("sameas-external")
    val WikidataAliasMappingsWiki = new Dataset("alias")
    val WikidataAliasRest = new Dataset("alias-nmw")
    val WikidataDescriptionMappingsWiki = new Dataset("description")
    val WikidataDescriptionRest = new Dataset("description-nmw")
    val WikidataProperty= new Dataset("properties")
    val WikidataR2R_literals = OntologyPropertiesLiterals
    val WikidataR2R_objects = OntologyPropertiesObjects
    val WikidataReifiedR2R = new Dataset("mappingbased_properties-reified") // keep same name with other languages
    val WikidataReifiedR2RQualifier= new Dataset("mappingbased_properties-reified-qualifiers") // keep same name with other languages
    val WikidataRaw = new Dataset("raw_unredirected")
    val WikidataRawReified = new Dataset("raw-reified")
    val WikidataRawReifiedQualifiers = new Dataset("raw-reified-qualifiers")
    val WikidataReference = new Dataset("references")

    /**
     * Citations
     */
    val CitationLinks = new Dataset("citation_links")
    val CitationData = new Dataset("citation_data")
    val CitationTypes = new Dataset("citation_types")
}
