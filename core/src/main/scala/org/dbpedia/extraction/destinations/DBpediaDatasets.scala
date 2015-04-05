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
    val MissingShortAbstracts = new Dataset("missing_short_abstracts")
    val MissingLongAbstracts = new Dataset("missing_long_abstracts")

    /**
     * Links
     */
    val LinksToWikipediaArticle = new Dataset("wikipedia_links")
    val ExternalLinks = new Dataset("external_links")
    val PageLinks = new Dataset("page_links")
    val DisambiguationLinks  = new Dataset("disambiguations")
    val Homepages = new Dataset("homepages")
    val OutDegree = new Dataset("out_degree")


    /**
     * Files
     */
    val FileInformation = new Dataset("file_information")


    /**
     * Wikidata
     */
    val Wikidata = new Dataset("wikidata")
    val WikidataLL = new Dataset("wikidata-ll")
    val WikidataLabelsMappingsWiki = new Dataset("wikidata-labels-mappingswiki")
    val WikidataLabelsRest = new Dataset("wikidata-labels-rest")
    val WikidataSameAs = new Dataset("wikidata-sameas")
    val WikidataNameSpaceSameAs = new Dataset("wikidata-namespace-sameas")
    val WikidataMappedFacts = new Dataset("wikidata-mapped")
    val WikidataAliasMappingsWiki = new Dataset("wikidata-alias-mappingswiki")
    val WikidataAliasRest = new Dataset("wikidata-alias-rest")
    val WikidataDescriptionMappingsWiki = new Dataset("wikidata-description-mappingswiki")
    val WikidataDescriptionRest = new Dataset("wikidata-description-rest")
    val WikidataProperty= new Dataset("wikidata-property")
    val WikidataR2R = new Dataset("wikidata-r2r")
    val WikidataReifiedR2R = new Dataset("wikidata-r2r-reified")
    val WikidataReifiedR2RQualifier= new Dataset("wikidata-r2r-reified-qualifiers")
    val WikidataRaw = new Dataset("wikidata-raw")
    val WikidataRawReified = new Dataset("wikidata-raw-reified")
    val WikidataRawReifiedQualifiers = new Dataset("wikidata-raw-reified-qualifiers")
    val WikidataReference = new Dataset("wikidata-reference")
}
