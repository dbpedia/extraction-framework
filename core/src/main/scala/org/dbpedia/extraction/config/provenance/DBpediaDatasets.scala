package org.dbpedia.extraction.config.provenance

import org.dbpedia.extraction.util.{JsonConfig, Language}

import scala.collection.mutable

/**
 * Defines the datasets which are extracted by DBpedia.
 */
object DBpediaDatasets
{
    private val datasets = new mutable.HashMap[String, Dataset]()
    private val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("datasetdefinitions.json"))

    private var tempMap = new mutable.HashMap[String, Dataset]()
    for (inner <- mappingsFile.keys()) {
        val in = mappingsFile.getMap(inner)
        for(filename <- in.keys) {
            val properties = in.get(filename).get
            tempMap(filename) = new Dataset(
                Option(properties.get("name")) match {
                    case Some(n) => n.asText()
                    case None => throw new IllegalArgumentException("Property 'name' is missing for Dataset definition of: " + filename)
                },
                Option(properties.get("desc")) match {
                    case Some(n) => n.asText()
                    case None => null
                },
                Option(properties.get("lang")) match {
                    case Some(n) => Language.apply(n.asText())
                    case None => null
                },
                Option(properties.get("vers")) match {
                    case Some(n) => n.asText()
                    case None => null
                },
                filename,
                Option(properties.get("orig")) match {
                    case Some(n) => new Dataset("alibi set will be replaces", n.asText())
                    case None => null
                },
                Option(properties.get("sources")) match {
                    case Some(n) => Seq(new Dataset("alibi set will be replaces", n.asText()))
                    case None => Seq()
                },
                Option(properties.get("result")) match {
                    case Some(n) => n.asText()
                    case None => null
                },
                Option(properties.get("input")) match {
                    case Some(n) => n.asText().split(",").map(_.trim)
                    case None => Seq()
                },
                Option(properties.get("depr")) match {
                    case Some(n) => n.asText()
                    case None => null
                }
            )
        }
    }

    //resolve orig and sources pointer
    for(set <- tempMap.values)
    {
        set.origin match{
            case Some(d) => set.origin = tempMap.get(d.description.get)
            case None =>
        }
        set.sources.headOption match{
            case Some(d) => set.sources = d.description.get.split(",").map(_.trim).map(x => tempMap.getOrElse(x, 1)).collect{ case a : Dataset => a}
            case None =>
        }

        datasets(set.encoded) = set

        //TODO make the next lines configurable or define them all in the config file!
        //-en-uris
        datasets(set.encoded + "_en_uris") = new Dataset(
            set.name + " English Uris",
            set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching English DBpedia resources.",
            null, null,
            set.encoded + "_en_uris",
            set,
            Seq(set)
        )
        //wkd uris
        datasets(set.encoded + "_wkd_uris") = new Dataset(
            set.name + " Wikidata Uris",
            set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching Wikidata DBpedia resources.",
            null, null,
            set.encoded + "_wkd_uris",
            set,
            Seq(set)
        )
        //wkd uris
        datasets(set.encoded + "_unredirected") = new Dataset(
            set.name + " Unredirected",
            set.description.getOrElse("Undescribed Dataset") + " This datasets resources were not resolved along Wikipedia redirects.",
            null, null,
            set.encoded + "_unredirected",
            set,
            Seq(set)
        )
    }

    tempMap = null
    
    /**
      * Source
      */
    val PagesArticles = datasets.get("pages_articles").get

    /**
     * General
     */
    val Labels = datasets.get("labels").get
    val CategoryLabels = datasets.get("category_labels").get
    val Images = datasets.get("images").get
    val GeoCoordinates = datasets.get("geo_coordinates").get
    val Persondata = datasets.get("persondata").get
    val Pnd = datasets.get("pnd").get
    val Redirects = datasets.get("redirects").get
    val RedirectsTransitive = datasets.get("transitive_redirects").get
    val ArticleCategories = datasets.get("article_categories").get
    val ArticleTemplates = datasets.get("article_templates").get
    val ArticleTemplatesNested = datasets.get("article_templates_nested").get
    val SkosCategories = datasets.get("skos_categories").get
    val RevisionUris = datasets.get("revision_uris").get
    val RevisionIds = datasets.get("revision_ids").get
    val RevisionMeta = datasets.get("revision_meta").get
    val PageIds = datasets.get("page_ids").get
    val InterLanguageLinks = datasets.get("interlanguage_links").get // Since the inter-language links were moved from Wikipedia to Wikidata, we now extract these links from the Wikidata dump, not from Wikipedia pages.")
    val InterLanguageLinksChapter = datasets.get("interlanguage_links_chapters").get
    val Genders = datasets.get("genders").get
    val TopicalConcepts = datasets.get("topical_concepts").get
    val UriSameAsIri = datasets.get("uri_same_as_iri").get
    val FlickrWrapprLinks = datasets.get("flickr_wrappr_links").get
    val PageLength = datasets.get("page_length").get
    val ImageGalleries = datasets.get("image_galleries").get
    val ImageAnnotations = datasets.get("image_annotations").get
    val KMLFiles = datasets.get("kml_files").get
    val AnchorText = datasets.get("anchor_text").get
    val SurfaceForms = datasets.get("surface_forms").get
    val Sounds = datasets.get("sounds").get

    /**
     * Mapping based
     */
    val OntologyTypes = datasets.get("instance_types").get
    val OntologyTypesTransitive = datasets.get("instance_types_transitive").get
    val OntologyPropertiesObjects = new Dataset("mappingbased_objects_uncleaned")
    val OntologyPropertiesLiterals = datasets.get("mappingbased_literals").get
    val OntologyPropertiesGeo = datasets.get("geo_coordinates_mappingbased").get
    val SpecificProperties = datasets.get("specific_mappingbased_properties").get

    val OntologyPropertiesObjectsCleaned = datasets.get("mappingbased_objects").get
    val OntologyPropertiesDisjointRange = datasets.get("mappingbased_objects_disjoint_range").get
    val OntologyPropertiesDisjointDomain = datasets.get("mappingbased_objects_disjoint_domain").get

    val LhdDboInstanceTypes = datasets.get("instance_types_lhd_dbo").get
    val LhdExtInstanceTypes = datasets.get("instance_types_lhd_ext").get
    val TaxDboInstanceTypes = datasets.get("instance_types_dbtax_dbo").get
    val TaxExtInstanceTypes = datasets.get("instance_types_dbtax_ext").get
    val SDInstanceTypes = datasets.get("instance_types_sdtyped_dbo").get

    /**
     * French population template
     */
     val FrenchPopulation = datasets.get("french_population").get

    /**
     *  Infobox
     */
    val InfoboxProperties = datasets.get("infobox_properties").get
    val InfoboxPropertiesMapped = datasets.get("infobox_properties_mapped").get
    val InfoboxPropertyDefinitions = datasets.get("infobox_property_definitions").get
    val TemplateParameters = datasets.get("template_parameters").get
    val InfoboxTest = new Dataset("infobox_test")

    /**
     * Abstracts
     */
    val ShortAbstracts = datasets.get("short_abstracts").get
    val LongAbstracts = datasets.get("long_abstracts").get
    val MissingShortAbstracts = new Dataset("missing_short_abstracts")
    val MissingLongAbstracts = new Dataset("missing_long_abstracts")
    val NifAbstractContext = datasets.get("nif_abstract_context").get
    val NifPageStructure = datasets.get("nif_page_structure").get
    val NifTextLinks = datasets.get("nif_text_links").get

    /**
     * Links
     */
    val LinksToWikipediaArticle = datasets.get("wikipedia_links").get
    val ExternalLinks = datasets.get("external_links").get
    val PageLinks = datasets.get("page_links").get
    val DisambiguationLinks  = datasets.get("disambiguations").get
    val Homepages = datasets.get("homepages").get
    val OutDegree = datasets.get("out_degree").get
    val FreebaseLinks = datasets.get("freebase_links").get
    val GeonamesLinks = datasets.get("geonames_links").get
    val CommonsLink = datasets.get("commons_page_links").get


    /**
     * Files
     */
    val FileInformation = new Dataset("file_information")


    /**
     * Wikidata
     */
    val WikidataLabelsMappingsWiki = new Dataset("wikidata_labels")
    val WikidataLabelsRest = new Dataset("wikidata_labels_nmw")
    val WikidataSameAs = new Dataset("sameas_all_wikis")
    val WikidataNameSpaceSameAs = new Dataset("sameas_wikidata")
    val WikidataSameAsExternal = new Dataset("sameas_external")
    val WikidataAliasMappingsWiki = new Dataset("alias")
    val WikidataAliasRest = new Dataset("alias_nmw")
    val WikidataDescriptionMappingsWiki = new Dataset("description")
    val WikidataDescriptionRest = new Dataset("description_nmw")
    val WikidataProperty= new Dataset("properties")
    val WikidataR2R_literals = OntologyPropertiesLiterals
    val WikidataR2R_objects = OntologyPropertiesObjects
    val WikidataR2R_ontology = new Dataset("ontology_subclassof")
    val WikidataR2R_mappingerrors = new Dataset("wikidata_r2r_mapping_errors")
    val WikidataReifiedR2R = new Dataset("mappingbased_properties_reified") // keep same name with other languages
    val WikidataReifiedR2RQualifier= new Dataset("mappingbased_properties_reified_qualifiers") // keep same name with other languages
    val WikidataRaw = new Dataset("raw_unredirected")
    val WikidataRawRedirected = new Dataset("raw")
    val WikidataRawReified = new Dataset("raw_reified")
    val WikidataRawReifiedQualifiers = new Dataset("raw_reified_qualifiers")
    val WikidataReference = new Dataset("references")
    val WikidataDublicateIriSplit = new Dataset("wikidata_duplicate_iri_split")

    /**
     * Citations
     */
    val CitationLinks = datasets.get("citation_links").get
    val CitationData = datasets.get("citation_data").get
    val CitatedFacts = new Dataset("cited_facts")  //TODO add description @Dimitris
    //val CitationTypes = new Dataset("citation_types")


  /**
    * misc
    */
  val MainDataset = datasets.get("main_dataset").get


  //TODO update this when getting temp datasets
  def getDataset(name: String) =
      datasets.get(name.trim.toLowerCase.replaceAll("-", "_").replaceAll("\\s+", "_")) match{
        case Some(d) => d
        case None => throw new NotImplementedError("DBpediaDataset class is missing the declaration of dataset " + name.replaceAll("-", "_").replaceAll("\\s+", "_"))
    }
}
