package org.dbpedia.extraction.destinations

/**
 * Defines the datasets which are extracted by DBpedia.
 * TODO: use a Scala Enumeration
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
    val SkosCategories = new Dataset("skos_categories")
    val RevisionUris = new Dataset("revision_uris")
    val RevisionIds = new Dataset("revision_ids")
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
     *  Infobox
     */
    val InfoboxProperties = new Dataset("infobox_properties")
    val InfoboxPropertyDefinitions = new Dataset("infobox_property_definitions")
    val TemplateVariables = new Dataset("template_parameters")
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
}