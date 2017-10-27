package org.dbpedia.extraction.config.provenance

import org.dbpedia.extraction.ontology.DBpediaNamespace
import org.dbpedia.extraction.util
import org.dbpedia.extraction.util.{JsonConfig, Language, StringUtils, WikiUtil}

import scala.collection.mutable
import scala.util.Try

/**
 * Defines the datasets which are extracted by DBpedia.
  *
  * IMPORTANT: when introducing new datasets: make sure you included a new entry in main/resources/datasetdefinitions.json!!!!
 */
object DBpediaDatasets
{
    private val datasets = new mutable.HashMap[String, Dataset]()
    private val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("datasetdefinitions.json"))
    private var tempMap = new mutable.HashMap[String, Dataset]()
    for (inner <- mappingsFile.keys() if inner != "TODOs") {
        val in = mappingsFile.getMap(inner)
        for(filename <- in.keys) {
            val properties = in(filename)
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
                },
                Option(properties.get("traits")) match {
                  case Some(n) => DatasetTrait.ValueSet(n.asText().split(",").map(x => DatasetTrait.withName(x.trim)):_*)
                  case None => DatasetTrait.ValueSet.empty
                },
                Option(properties.get("defaultgraph")) match {
                    case Some(n) => n.asText()
                    case None => null
                },
                Option(properties.get("keywords")) match {
                  case Some(n) => n.asText().split(",").map(_.trim)
                  case None => Seq()
                }
            )
        }
    }

    //resolve orig and sources pointer
    for(set <- tempMap.values)
    {
        set.sources.headOption match{
            case Some(d) => set.sources = d.description.get.split(",").map(_.trim).map(x => tempMap.getOrElse(x, 1)).collect{ case a : Dataset => a}
            case None =>
        }

        datasets(set.encoded) = set

        //TODO make the next lines configurable or define them all in the config file!
        //-en-uris
        datasets(set.encoded + "_en_uris") = set.copyDataset(
            naturalName = set.name + " English Uris",
            descr = set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching English DBpedia resources.",
            fileName = set.encoded + "_en_uris",
            traits = set.traits + DatasetTrait.EnglishUris,
            sources = Seq(set)
        )
        //wkd uris unsorted
        datasets(set.encoded + "_wkd_uris_unsorted") = set.copyDataset(
            naturalName = set.name + " Wikidata Uris unsorted",
            descr = set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching Wikidata resources (unsorted).",
            fileName = set.encoded + "_wkd_uris_unsorted",
            traits = set.traits ++ Seq(DatasetTrait.WikidataUris, DatasetTrait.Unsorted),
            sources = Seq(set)
        )
        //wkd uris
        datasets(set.encoded + "_wkd_uris") = set.copyDataset(
            naturalName = set.name + " Wikidata Uris",
            descr = set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching Wikidata resources (sorted).",
            fileName = set.encoded + "_wkd_uris",
            traits = set.traits ++ Seq(DatasetTrait.WikidataUris, DatasetTrait.Ordered),
            sources = Seq(set)
        )
        //unredirected
        datasets(set.encoded + "_unredirected") = set.copyDataset(
            naturalName = set.name + " Unredirected",
            descr = set.description.getOrElse("Undescribed Dataset") + " This datasets resources were not resolved along Wikipedia redirects.",
            fileName = set.encoded + "_unredirected",
            traits = set.traits + DatasetTrait.Unredirected,
            sources = Seq(set)
        )
    }

    //TODO these datasets should be replaced - create wikidata sameas links files
    for(lang <- Language.map.values)
        datasets("interlanguage_links_" + lang.wikiCode) = new Dataset(
            "Wikidata Interlanguage Links for " + lang.name,
            "Interlanguage links generated from the Wikidata dump file.",
            Language.Wikidata,
            null,
            "interlanguage_links_" + lang.wikiCode
        )



    tempMap = null
    
    /**
      * Source
      */
    val PagesArticles: Dataset = datasets("pages_articles")

    /**
     * General
     */
    val Labels: Dataset = datasets("labels")
    val CategoryLabels: Dataset = datasets("category_labels")
    val Images: Dataset = datasets("images")
    val GeoCoordinates: Dataset = datasets("geo_coordinates")
    val Persondata: Dataset = datasets("persondata")
    val Pnd: Dataset = datasets("pnd")
    val Redirects: Dataset = datasets("redirects")
    val RedirectsTransitive: Dataset = datasets("transitive_redirects")
    val ArticleCategories: Dataset = datasets("article_categories")
    val ArticleTemplates: Dataset = datasets("article_templates")
    val ArticleTemplatesNested: Dataset = datasets("article_templates_nested")
    val TemplateDefinitions: Dataset = datasets("template_definitions")
    val SkosCategories: Dataset = datasets("skos_categories")
    val RevisionUris: Dataset = datasets("revision_uris")
    val RevisionIds: Dataset = datasets("revision_ids")
    val RevisionMeta: Dataset = datasets("revision_meta")
    val PageIds: Dataset = datasets("page_ids")
    val InterLanguageLinks: Dataset = datasets("interlanguage_links") // Since the inter-language links were moved from Wikipedia to Wikidata, we now extract these links from the Wikidata dump, not from Wikipedia pages.")
    val InterLanguageLinksChapter: Dataset = datasets("interlanguage_links_chapters")
    val Genders: Dataset = datasets("genders")
    val TopicalConcepts: Dataset = datasets("topical_concepts")
    val UriSameAsIri: Dataset = datasets("uri_same_as_iri")
    val FlickrWrapprLinks: Dataset = datasets("flickr_wrappr_links")
    val PageLength: Dataset = datasets("page_length")
    val ImageGalleries: Dataset = datasets("image_galleries")
    val ImageAnnotations: Dataset = datasets("image_annotations")
    val KMLFiles: Dataset = datasets("kml_files")
    val AnchorText: Dataset = datasets("anchor_text")
    val SurfaceForms: Dataset = datasets("surface_forms")
    val Sounds: Dataset = datasets("sounds")

    /**
     * Mapping based
     */
    val OntologyTypes: Dataset = datasets("instance_types")
    val OntologyTypesTransitive: Dataset = datasets("instance_types_transitive")
    val OntologyPropertiesObjects: Dataset = datasets("mappingbased_objects_uncleaned")
    val OntologyPropertiesLiterals: Dataset = datasets("mappingbased_literals")
    val OntologyPropertiesGeo: Dataset = datasets("geo_coordinates_mappingbased")
    val SpecificProperties: Dataset = datasets("specific_mappingbased_properties")

    val OntologyPropertiesObjectsCleaned: Dataset = datasets("mappingbased_objects")
    val OntologyPropertiesDisjointRange: Dataset = datasets("mappingbased_objects_disjoint_range")
    val OntologyPropertiesDisjointDomain: Dataset = datasets("mappingbased_objects_disjoint_domain")

    val LhdDboInstanceTypes: Dataset = datasets("instance_types_lhd_dbo")
    val LhdExtInstanceTypes: Dataset = datasets("instance_types_lhd_ext")
    val TaxDboInstanceTypes: Dataset = datasets("instance_types_dbtax_dbo")
    val TaxExtInstanceTypes: Dataset = datasets("instance_types_dbtax_ext")
    val SDInstanceTypes: Dataset = datasets("instance_types_sdtyped_dbo")

    /**
     * French population template
     */
     val FrenchPopulation: Dataset = datasets("french_population")

    /**
     *  Infobox
     */
    val InfoboxProperties: Dataset = datasets("infobox_properties")
    val InfoboxPropertiesMapped: Dataset = datasets("infobox_properties_mapped")
    val InfoboxPropertiesExtended: Dataset = datasets("infobox_properties_extended")
    val InfoboxPropertyDefinitions: Dataset = datasets("infobox_property_definitions")
    val TemplateParameters: Dataset = datasets("template_parameters")
    val TemplateMappings: Dataset = datasets("template_mappings")
    val TemplateMappingsHints: Dataset = datasets("template_mapping_hints")
    val TemplateMappingsHintsInstance: Dataset = datasets("template_mapping_hints_instance")
    val InfoboxTest: Dataset = datasets("infobox_test")

    /**
     * Abstracts & Page text
     */
    val ShortAbstracts: Dataset = datasets("short_abstracts")
    val LongAbstracts: Dataset = datasets("long_abstracts")
    val MissingShortAbstracts: Dataset = datasets("missing_short_abstracts")
    val MissingLongAbstracts: Dataset = datasets("missing_long_abstracts")
    val NifContext: Dataset = datasets("nif_context")
    val NifPageStructure: Dataset = datasets("nif_page_structure")
    val NifTextLinks: Dataset = datasets("nif_text_links")
    val RawTables: Dataset = datasets("raw_tables")
    val Equations: Dataset = datasets("equations")

    /**
     * Links
     */
    val LinksToWikipediaArticle: Dataset = datasets("wikipedia_links")
    val ExternalLinks: Dataset = datasets("external_links")
    val PageLinks: Dataset = datasets("page_links")
    val DisambiguationLinks: Dataset = datasets("disambiguations")
    val Homepages: Dataset = datasets("homepages")
    val OutDegree: Dataset = datasets("out_degree")
    val FreebaseLinks: Dataset = datasets("freebase_links")
    val GeonamesLinks: Dataset = datasets("geonames_links")
    val CommonsLink: Dataset = datasets("commons_page_links")


    /**
     * Files
     */
    val FileInformation: Dataset = datasets("file_information")


    /**
     * Wikidata
     */
    //val WikidataLabelsMappingsWiki = datasets.get("wikidata_labels").get
    val WikidataLabelsRest: Dataset = datasets("labels_nmw")
    val WikidataSameAs: Dataset = datasets("sameas_all_wikis")
    val WikidataNameSpaceSameAs: Dataset = datasets("sameas_wikidata")
    val WikidataSameAsExternal: Dataset = datasets("sameas_external")
    val WikidataAliasMappingsWiki: Dataset = datasets("alias")
    val WikidataAliasRest: Dataset = datasets("alias_nmw")
    val WikidataDescriptionMappingsWiki: Dataset = datasets("description")
    val WikidataDescriptionRest: Dataset = datasets("description_nmw")
    val WikidataProperty: Dataset = datasets("properties")
    val WikidataR2R_literals = OntologyPropertiesLiterals
    val WikidataR2R_objects = OntologyPropertiesObjects
    val WikidataR2R_ontology: Dataset = datasets("ontology_subclassof")
    val WikidataR2R_mappingerrors: Dataset = datasets("wikidata_r2r_mapping_errors")
    val WikidataReifiedR2R: Dataset = datasets("mappingbased_properties_reified") // keep same name with other languages
    val WikidataReifiedR2RQualifier: Dataset = datasets("mappingbased_properties_reified_qualifiers") // keep same name with other languages
    val WikidataRaw: Dataset = datasets("raw_unredirected")
    val WikidataRawRedirected: Dataset = datasets("raw")
    val WikidataRawReified: Dataset = datasets("raw_reified")
    val WikidataRawReifiedQualifiers: Dataset = datasets("raw_reified_qualifiers")
    val WikidataReference: Dataset = datasets("references")
    val WikidataDublicateIriSplit: Dataset = datasets("wikidata_duplicate_iri_split")
    val WikidataTypeLikeStatements: Dataset = datasets("wikidata_type_like_statements")
    val WikidataPersondataRaw: Dataset = datasets("wikidata_persondata_raw")

    val WikidataR2RErrorDataset = DBpediaDatasets.WikidataR2R_mappingerrors
    val WikidataDuplicateIRIDataset = DBpediaDatasets.WikidataDublicateIriSplit

    /**
     * Citations
     */
    val CitationLinks: Dataset = datasets("citation_links")
    val CitationData: Dataset = datasets("citation_data")
    val CitatedFacts: Dataset = datasets("cited_facts")  //TODO add description @Dimitris
    //val CitationTypes = datasets.get("citation_types").get


  /**
    * misc
    */
  val MainDataset: Dataset = datasets("main_dataset")
  val WiktionaryDataset: Dataset = datasets("wiktionary_dbpedia_org")
  val TestDataset = new Dataset("test_dataset", "this is just a test", null, null, "test_dataset", Seq(MainDataset), null, Seq(), null, DatasetTrait.ValueSet(DatasetTrait.Ordered, DatasetTrait.Provenance))


  def getDataset(dataset: Dataset, language: Language, version: String): Try[Dataset] = getDataset(dataset.encoded, language, version)

  def getDataset(name: String, language: Language = null, version: String = null): Try[Dataset] =
  {
    Try {
      val n = if (name.startsWith(DBpediaNamespace.DATASET.toString))
        name.substring(DBpediaNamespace.DATASET.toString.length, if (name.indexOf("?") >= 0) name.indexOf("?") else name.length)
      else
        util.WikiUtil.wikiEncode(name.toLowerCase).replaceAll("-", "_")
      datasets.get(n) match {
        case Some(d) => {
          Option(language) match {
            case Some(l) => {
              Option(version) match {
                case Some(v) => d.copyDataset(lang = l, versionEntry = v)
                case None => d.copyDataset(lang = l)
              }
            }
            case None => d
          }
        }
        case None => throw new NotImplementedError("DBpediaDataset class is missing the declaration of dataset " + name.replaceAll("-", "_").replaceAll("\\s+", "_"))
      }
    }
  }
}
