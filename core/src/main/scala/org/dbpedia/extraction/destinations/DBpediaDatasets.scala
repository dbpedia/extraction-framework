package org.dbpedia.extraction.destinations

/**
 * Defines the datasets which are extracted by DBpedia.
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
    val Revisions = new Dataset("revisions")
    val PageIds = new Dataset("page_ids")
    val SameAs = new Dataset("sameas")

    /**
     * Mapping based
     */
    val OntologyTypes = new Dataset("instance_types")
    val OntologyProperties = new Dataset("mappingbased_properties")
    val SpecificProperties = new Dataset("specific_mappingbased_properties")

    /**
     *  Infobox
     */
    val Infoboxes = new Dataset("infobox_properties")
    val InfoboxProperties = new Dataset("infobox_property_definitions")
    val TemplateVariables = new Dataset("templateVariables")
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