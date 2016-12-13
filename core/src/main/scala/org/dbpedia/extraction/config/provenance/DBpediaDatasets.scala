package org.dbpedia.extraction.config.provenance

import org.dbpedia.extraction.ontology.DBpediaNamespace
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
    for (inner <- mappingsFile.keys() if inner != "TODOs") {
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
        //wkd uris
        datasets(set.encoded + "_wkd_uris") = set.copyDataset(
            naturalName = set.name + " Wikidata Uris",
            descr = set.description.getOrElse("Undescribed Dataset") + " Normalized dataset matching Wikidata resources.",
            fileName = set.encoded + "_wkd_uris",
            traits = set.traits + DatasetTrait.WikidataUris,
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
    val OntologyPropertiesObjects = datasets.get("mappingbased_objects_uncleaned").get
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
    val TemplateMappings = datasets.get("template_mappings").get
    val TemplateMappingsHints = datasets.get("template_mapping_hints").get
    val TemplateMappingsHintsInstance = datasets.get("template_mapping_hints_instance").get
    val InfoboxTest = datasets.get("infobox_test").get

    /**
     * Abstracts
     */
    val ShortAbstracts = datasets.get("short_abstracts").get
    val LongAbstracts = datasets.get("long_abstracts").get
    val MissingShortAbstracts = datasets.get("missing_short_abstracts").get
    val MissingLongAbstracts = datasets.get("missing_long_abstracts").get
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
    val FileInformation = datasets.get("file_information").get


    /**
     * Wikidata
     */
    //val WikidataLabelsMappingsWiki = datasets.get("wikidata_labels").get
    val WikidataLabelsRest = datasets.get("labels_nmw").get
    val WikidataSameAs = datasets.get("sameas_all_wikis").get
    val WikidataNameSpaceSameAs = datasets.get("sameas_wikidata").get
    val WikidataSameAsExternal = datasets.get("sameas_external").get
    val WikidataAliasMappingsWiki = datasets.get("alias").get
    val WikidataAliasRest = datasets.get("alias_nmw").get
    val WikidataDescriptionMappingsWiki = datasets.get("description").get
    val WikidataDescriptionRest = datasets.get("description_nmw").get
    val WikidataProperty= datasets.get("properties").get
    val WikidataR2R_literals = OntologyPropertiesLiterals
    val WikidataR2R_objects = OntologyPropertiesObjects
    val WikidataR2R_ontology = datasets.get("ontology_subclassof").get
    val WikidataR2R_mappingerrors = datasets.get("wikidata_r2r_mapping_errors").get
    val WikidataReifiedR2R = datasets.get("mappingbased_properties_reified").get // keep same name with other languages
    val WikidataReifiedR2RQualifier= datasets.get("mappingbased_properties_reified_qualifiers").get // keep same name with other languages
    val WikidataRaw = datasets.get("raw_unredirected").get
    val WikidataRawRedirected = datasets.get("raw").get
    val WikidataRawReified = datasets.get("raw_reified").get
    val WikidataRawReifiedQualifiers = datasets.get("raw_reified_qualifiers").get
    val WikidataReference = datasets.get("references").get
    val WikidataDublicateIriSplit = datasets.get("wikidata_duplicate_iri_split").get

    /**
     * Citations
     */
    val CitationLinks = datasets.get("citation_links").get
    val CitationData = datasets.get("citation_data").get
    val CitatedFacts = datasets.get("cited_facts").get  //TODO add description @Dimitris
    //val CitationTypes = datasets.get("citation_types").get


  /**
    * misc
    */
  val MainDataset = datasets.get("main_dataset").get
  val WiktionaryDataset = datasets.get("wiktionary_dbpedia_org").get
  val TestDataset = new Dataset("test_dataset", "this is just a test", Language.English, "2015-10", "test_dataset", Seq(MainDataset), null, Seq(), "2015-10", DatasetTrait.ValueSet(DatasetTrait.Ordered, DatasetTrait.Provenance))


  def getDataset(dataset: Dataset, language: Language, version: String): Dataset = getDataset(dataset.encoded, language, version)

  def getDataset(name: String, language: Language = null, version: String = null) =
  {
    val n = if(name.startsWith(DBpediaNamespace.DATASET.toString))
      name.substring(DBpediaNamespace.DATASET.toString.length, if(name.indexOf("?") >= 0) name.indexOf("?") else name.length)
    else
      name.trim.toLowerCase.replaceAll("-", "_").replaceAll("\\s+", "_")
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
