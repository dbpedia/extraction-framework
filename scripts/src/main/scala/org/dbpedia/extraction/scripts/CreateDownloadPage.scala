package org.dbpedia.extraction.scripts

import java.io.File
import java.text.{DecimalFormat, DecimalFormatSymbols}
import java.util.Locale

import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.{IOUtils, StringPlusser}

import scala.collection.mutable.HashMap

/**
 * Generate Wacko Wiki source text for http://wiki.dbpedia.org/Downloads and all its sub pages.
 * 
 * Example call:
 * 
 * ../run CreateDownloadPage src/main/data/lines-bytes-packed.txt
 */
object CreateDownloadPage {
  
// UPDATE for new release
val current = "2015-04"
  
// UPDATE for new release
val previous = List("2014","3.9", "3.8", "3.7", "3.6", "3.5.1", "3.5", "3.4", "3.3", "3.2", "3.1", "3.0", "3.0RC", "2.0")

// UPDATE for new release
val dumpDates =
"The datasets were extracted from ((http://dumps.wikimedia.org/ Wikipedia dumps)) generated in " +
"February / March 2015." +
"See also all ((DumpDatesDBpedia"+tag(current)+" specific dump dates and times)).\n";
  
// UPDATE for new release
val allLanguages = 128

// CCHECK / UPDATE for new release
// All languages that have a significant number of mapped articles.
// en must be first, we use languages.drop(1) in datasetPages()
val languages = List("en", "bg", "ca", "cs", "de", "es", "eu", "fr", "hu", "id", "it", "ja", "ko", "nl", "pl", "pt",
  "ru", "tr")

// UPDATE when DBpedia Wiki changes. Wiki link to page Datasets, section about internationalized datasets.
val l10nDatasetsSection = "Datasets#h18-19"

val dbpediaLeipzigUrl = "http://downloads.dbpedia.org/"
val dbpediaMannheimUrl = "http://data.dws.informatik.uni-mannheim.de/dbpedia/"
val dbpediaUrl = dbpediaLeipzigUrl

val zipSuffix = ".bz2"

val titles = new HashMap[String, String]()

val formatter = new DecimalFormat("0.#", new DecimalFormatSymbols(Locale.ENGLISH))

private def niceDecimal(num: Long): String = {
  if (num < 1000) formatter.format(num)
  else if (num < 1000000) formatter.format(num / 1000F)+"K"
  else formatter.format(num / 1000000F)+"M"
}

private def niceBytes(bytes: Long): String = {
  if (bytes < 1024) formatter.format(bytes)
  else if (bytes < 1048576) formatter.format(bytes / 1024F)+"KiB"
  else if (bytes < 1073741824) formatter.format(bytes / 1048576F)+"MiB"
  else formatter.format(bytes / 1073741824F)+"GiB"
}

def loadTitles(file: File): Unit = {
  // read lines in this format: 
  // dbpedia_3.9.owl lines:4622 bytes:811552 gzip:85765 bzip2:50140
  // links/revyu_links.nt lines:6 bytes:1008 gzip:361 bzip2:441
  // af/geo_coordinates_af.nt lines:82 bytes:11524 gzip:1141 bzip2:1228
  IOUtils.readLines(file) { line =>
    if (line == null) return // all done
    val parts = line.split("\\s+", -1)
    if (parts.length != 9) throw new IllegalArgumentException("bad line format: "+line)
    val path = parts(0).replace("core-i18n/","")
    val lines = parts(2).toLong
    val bytes = parts(4).toLong
    val gzip = parts(6).toLong
    val bzip2 = parts(8).toLong
    titles(path) = "Lines:\u00A0"+niceDecimal(lines)+"; File\u00A0size\u00A0(download):\u00A0"+niceBytes(bzip2)+"; File\u00A0size\u00A0(unpacked):\u00A0"+niceBytes(bytes)
  }
}

val OntologyPage = "Ontology"
val DataC14NPage = "DataC14N"
val DataL10NPage = "DataL10N"
val LinksPage = "Links"
val DescPage = "Desc"
val NLPPage = "NLP"

// path: dbpedia_3.9.owl, af/geo_coordinates_af.nq, links/revyu_links.nt
class FileInfo(val path: String, val title: String) {
  
  // example: 3.9/af/geo_coordinates_af.nq.bz2
  val fullPath = if (path.startsWith("links"))
    current+"/"+path+zipSuffix
                  else current+"/core-i18n/"+path+zipSuffix
  
  // example: http://downloads.dbpedia.org/3.9/af/geo_coordinates_af.nq.bz2
  val downloadUrl = dbpediaUrl+fullPath
  
  // example: http://downloads.dbpedia.org/preview.php?file=3.9_sl_af_sl_geo_coordinates_af.nq.bz2
  val previewUrl = dbpediaLeipzigUrl+"preview.php?file="+fullPath.replace("/", "_sl_")
}
  
abstract class Fileset(
  val name: String,
  val file: String,
  val text: String,
  val formats: List[String],
  val pages: Set[String]
) 
{
  // null if data file didn't contain any info about this file
  def file(language: String, modifier: String, format: String, required: Boolean): FileInfo = {
    val p = path(language, modifier, format)
    val t = titles.getOrElse(p, null)
    
    if (t != null) new FileInfo(p, t)
    else if (! required) null
    else throw new IllegalArgumentException("found no data for "+p)
  }
  
  def path(language: String, modifier: String, format: String): String
  
  def anchor(prefix: String = "") = (prefix+name).replaceChars(" ()~", "-").toLowerCase(Locale.ENGLISH)
}

class Ontology(name: String, file: String, text: String)
extends Fileset(name, file, text, List("owl"), Set(OntologyPage))
{
  // dbpedia_3.9.owl
  override def path(language: String, modifier: String, format: String) = file+modifier+"."+format
}

class Dataset(name: String, file: String, text: String, pages: Set[String] = null)
extends Fileset(name, file, text, List("nt", "nq", "ttl"), pages)
{
  // example: af/geo_coordinates_af.nt
  override def path(language: String, modifier: String, format: String) = language+"/"+file+modifier+"_"+language+"."+format
}

class Linkset(name: String, file: String, text: String)
extends Fileset(name, file, text, List("nt"), Set(LinksPage))
{
  // example: links/yago_types.nt
  override def path(language: String, modifier: String, format: String) = language+"/"+file+modifier+"."+format
}

def tag(version: String): String = version.replace(".", "")

val ontology =
new Ontology("DBpedia Ontology", "dbpedia_"+current, "//The DBpedia ontology in OWL. See ((http://svn.aksw.org/papers/2013/SWJ_DBpedia/public.pdf our SWJ paper)) for more details.//")
// We have to split the main datasets into many small sub-pages because wacko wiki is true to its
// name and fails to display a page (or even print a proper error message) when it's too long. 
val datasets = List(
  List(
    new Dataset("Mapping-based Types", "instance-types", "//Contains triples of the form $object rdf:type $class from the mapping-based extraction.//"),
    new Dataset("Mapping-based Types (Transitive)", "instance-types-transitive", "//Contains transitive rdf:type $class based on the DBpedia ontology.//"),
    new Dataset("Mapping-based Types (Heuristic)", "instance_types_sdtyped-dbo", "//Contains 3.4M additional triples of the form $object rdf:type $class that were generated using the heuristic described in ((http://www.heikopaulheim.com/documents/iswc2013.pdf Paulheim/Bizer: Type Inference on Noisy RDF Data (ISWC 2013))). The estimated precision of those statements is 95%.//"),
    new Dataset("Inferred Types LHD (dbo)", "instance_types_lhd_dbo", "//Abstract-based inferred types based on the DBpedia ontology. See http://ner.vse.cz/datasets/linkedhypernyms/ for more details.//"),
    new Dataset("Inferred Types LHD (ext)", "instance_types_lhd_ext", "//Abstract-based inferred types beyond the DBpedia ontology. See http://ner.vse.cz/datasets/linkedhypernyms/ for more details.//"),
    new Dataset("Inferred Types DBTax (dbo)", "instance_types_dbtax-dbo", "//Category-based inferred types based on the DBpedia ontology. See http://it.dbpedia.org/2015/02/dbpedia-italiana-release-3-4-wikidata-e-dbtax/?lang=en for more details.//"),
    new Dataset("Inferred Types DBTax (ext)", "instance_types_dbtax_ext", "//Category-based inferred types beyond the DBpedia ontology. See http://it.dbpedia.org/2015/02/dbpedia-italiana-release-3-4-wikidata-e-dbtax/?lang=en for more details.//", Set(DataC14NPage)),

    new Dataset("Mapping-based Properties", "mappingbased-properties", "//High-quality data extracted from Infoboxes using the mapping-based extraction. The predicates in this dataset are in the /ontology/ namespace.//\n  Note that this data is of much higher quality than the Raw Infobox Properties in the /property/ namespace. For example, there are three different raw Wikipedia infobox properties for the birth date of a person. In the the /ontology/ namespace, they are all **mapped onto one relation** http://dbpedia.org/ontology/birthDate. It is a strong point of DBpedia to unify these relations.//"),
    new Dataset("Mapping-based Properties (errors)", "mappingbased-properties-errors-unredirected", "//Errors detected in the mapping based properties. At them moment the errors are limited to ranges that are disjoint with the property definition.//")
  ),
  List(
    //new Dataset("Mapping-based Properties (Cleaned)", "mappingbased-properties-cleaned", "//This file contains the statements from the Mapping-based Properties, with incorrect statements identified by heuristic inference being removed.//", Set(DataC14NPage)),
    new Dataset("Mapping-based Properties (Specific)", "specific-mappingbased-properties", "//Infobox data from the mapping-based extraction, using units of measurement more convenient for the resource type, e.g. square kilometres instead of square metres for the area of a city.//")
  ),
  List(
    new Dataset("Titles", "labels", "//Titles of all Wikipedia Articles in the corresponding language.//"),
    new Dataset("Short Abstracts", "short-abstracts", "//Short Abstracts (max. 500 characters long) of Wikipedia articles.//")
  ),
  List(
    new Dataset("Extended Abstracts", "long-abstracts", "//Full abstracts of Wikipedia articles, usually the first section.//"),
    new Dataset("Images", "images", "//Main image and corresponding thumbnail from Wikipedia article.//")
  ),
  List(
    new Dataset("Geographic Coordinates", "geo-coordinates", "//Geographic coordinates extracted from Wikipedia.//"),
    new Dataset("Raw Infobox Properties", "infobox-properties", "//Information that has been extracted from Wikipedia infoboxes. Note that this data is in the less clean /property/ namespace. The Mapping-based Properties (/ontology/ namespace) should always be preferred over this data.//")
  ),
  List(
    new Dataset("Raw Infobox Property Definitions", "infobox-property-definitions", "//All properties / predicates used in infoboxes.//"),
    new Dataset("Homepages", "homepages", "//Links to homepages of persons, organizations etc.//")
  ),
  List(
    new Dataset("Persondata", "persondata", "//Information about persons (date and place of birth etc.) extracted from the English and German Wikipedia, represented using the FOAF vocabulary.//"),
    new Dataset("Inter-Language Links", "interlanguage-links", "//Dataset linking a DBpedia resource to the same resource in other languages and in ((http://www.wikidata.org Wikidata)). Since the inter-language links were moved from Wikipedia to Wikidata, we now extract these links from the Wikidata dump, not from Wikipedia pages.//"),
    new Dataset("Inter-Language Links (chapters)", "wikidata-links-chapters", "//Dataset linking a DBpedia resource to the same resource in other languages and in ((http://www.wikidata.org Wikidata)). These are links only between DBpedia chapters.//")
  ),
  List(
    new Dataset("Articles Categories", "article-categories", "//Links from concepts to categories using the SKOS vocabulary.//"),
    new Dataset("Categories (Labels)", "category-labels", "//Labels for Categories.//"),
    new Dataset("Categories (Skos)", "skos-categories", "//Information which concept is a category and how categories are related using the SKOS Vocabulary.//")
  ),
  List(
    new Dataset("External Links", "external-links", "//Links to external web pages about a concept.//"),
    new Dataset("Links to Wikipedia Article", "wikipedia-links", "//Dataset linking DBpedia resource to corresponding article in Wikipedia.//"),
    new Dataset("Wikipedia Pagelinks", "page-links", "//Dataset containing internal links between DBpedia instances. The dataset was created from the internal links between Wikipedia articles. The dataset might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms.//")
  ),
  List(
    new Dataset("Redirects", "redirects", "//Dataset containing redirects between articles in Wikipedia.//"),
    new Dataset("Transitive Redirects", "transitive-redirects", "//Redirects dataset in which multiple redirects have been resolved and redirect cycles have been removed.//")
  ),
  List(
    new Dataset("Disambiguation links", "disambiguations", "//Links extracted from Wikipedia ((http://en.wikipedia.org/wiki/Wikipedia:Disambiguation disambiguation)) pages. Since Wikipedia has no syntax to distinguish disambiguation links from ordinary links, DBpedia has to use heuristics.//"),
    new Dataset("IRI-same-as-URI links", "iri-same-as-uri", "//owl:sameAs links between the ((http://tools.ietf.org/html/rfc3987 IRI)) and ((http://tools.ietf.org/html/rfc3986 URI)) format of DBpedia resources. Only extracted when IRI and URI are actually different.//")
  ),
  List(
    new Dataset("Page IDs", "page-ids", "//Dataset linking a DBpedia resource to the page ID of the Wikipedia article the data was extracted from.//"),
    new Dataset("Revision IDs", "revision-ids", "//Dataset linking a DBpedia resource to the revision ID of the Wikipedia article the data was extracted from. Until DBpedia 3.7, these files had names like 'revisions_en.nt'. Since DBpedia 3.9, they were renamed to 'revisions_ids_en.nt' to distinguish them from the new 'revision_uris_en.nt' files.//"),
    new Dataset("Revision URIs", "revision-uris", "//Dataset linking DBpedia resource to the specific Wikipedia article revision used in this DBpedia release.//")
  ),
  List(
//    new Dataset("Anchor Texts", "anchor-texts", "//Texts used in links to refer to Wikipedia articles from other Wikipedia articles.//"),
    //new Dataset("Surface Forms", "surface-forms", "//Texts used to refer to Wikipedia articles. Includes the anchor texts data, the names of redirects pointing to an article and the actual article name.//"),
    new Dataset("Page Length", "page-length", "//Numbers of characters contained in a Wikipedia article's source.//")
  ),
  List(
    new Dataset("Page Outdegree", "out-degree", "//Number of links emerging from a Wikipedia article and pointing to another Wikipedia article.//")
    //new Dataset("Old Interlanguage Links", "old-interlanguage-links", "//Remaining interlanguage extracted directly from Wikipedia articles. However, the main part of interlanguage links has been moved to Wikidata.//")
  )
)

val linksets = List(
  new Linkset("Links to Amsterdam Museum data", "amsterdammuseum_links", "//Links between DBpedia and ((http://semanticweb.cs.vu.nl/lod/am/ Amsterdam Museum)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/amsterdammuseum Silk link specification)).//"),
  new Linkset("Links to BBC Wildlife Finder", "bbcwildlife_links", "//Links between DBpedia and ((http://www.bbc.co.uk/wildlifefinder/ BBC Wildlife Finder)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/bbcwildlife Silk link specification)).//"),
  new Linkset("Links to RDF Bookmashup", "bookmashup_links", "//Links between books in DBpedia and data about them provided by the ((http://www4.wiwiss.fu-berlin.de/bizer/bookmashup/ RDF Book Mashup)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to Bricklink", "bricklink_links", "//Links between DBpedia and ((http://kasabi.com/dataset/bricklink Bricklink)). Links created manually.//"),
  new Linkset("Links to CORDIS", "cordis_links", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/cordis/ CORDIS)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/cordis Silk link specifications)).//"),
  new Linkset("Links to ~DailyMed", "dailymed_links", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/dailymed/ DailyMed)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dailymed Silk link specifications)).//"),
  new Linkset("Links to DBLP", "dblp_links", "//Links between computer scientists in DBpedia and their publications in the ((http://www.informatik.uni-trier.de/~ley/db/ DBLP)) database. Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dblp Silk link specification)).//"),
  new Linkset("Links to DBTune", "dbtune_links", "//Links between DBpedia and ((http://dbtune.org/ DBTune)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dbtune Silk link specifications)).//"),
  new Linkset("Links to Diseasome", "diseasome_links", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/diseasome/ Diseasome)).  Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/diseasome Silk link specifications)).//"),
  new Linkset("Links to ~DrugBank", "drugbank_links", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/drugbank/ DrugBank)).  Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/drugbank Silk link specifications)).//"),
  new Linkset("Links to EUNIS", "eunis_links", "//Links between DBpedia and ((http://eunis.eea.europa.eu EUNIS)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/eunis Silk link specification)).//"),
  new Linkset("Links to Eurostat (Linked Statistics)", "eurostat_linkedstatistics_links", "//Links between DBpedia and ((http://eurostat.linked-statistics.org Eurostat (Linked Statistics))). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/eurostat_linkedstatistics Silk link specifications)).//"),
  new Linkset("Links to Eurostat (WBSG)", "eurostat_wbsg_links", "//Links between countries and regions in DBpedia and data about them from ((http://www4.wiwiss.fu-berlin.de/eurostat/ Eurostat (WBSG))). Links created manually.//"),
  new Linkset("Links to CIA World Factbook", "factbook_links", "//Links between DBpedia and the ((http://www4.wiwiss.fu-berlin.de/factbook/ CIA World Factbook)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/factbook Silk link specifications)).//"),
  new Linkset("Links to flickr wrappr", "flickrwrappr_links", "//Links between DBpedia concepts and photo collections depicting them generated by ((http://www4.wiwiss.fu-berlin.de/flickrwrappr/ flickr wrappr)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/extraction_framework/file/dump/core/src/main/scala/org/dbpedia/extraction/mappings/FlickrWrapprLinkExtractor.scala Scala extractor)).//"),
  new Linkset("Links to Freebase", "freebase_links", "//Links between DBpedia and ((http://www.freebase.com/ Freebase (MIDs))). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/extraction_framework/file/dump/scripts/src/main/scala/org/dbpedia/extraction/scripts/CreateFreebaseLinks.scala Scala script)).//"),
  new Linkset("Links to GADM", "gadm_links", "//Links between DBpedia and ((http://gadm.geovocab.org/ GADM)). Links created by ((https://github.com/dbpedia/dbpedia-links/tree/master/datasets/dbpedia.org/gadm.geovocab.org/scripts Java program)).//"),
  new Linkset("Links to ~GeoNames", "geonames_links", "//Links between geographic places in DBpedia and data about them from ((http://www.geonames.org/ GeoNames)). Update mechanism: ((https://github.com/dbpedia/extraction-framework/blob/dump/scripts/src/main/bash/process-geonames.txt generated from latest GeoNames datasets)).//"),
  new Linkset("Links to ~GeoSpecies", "geospecies_links", "//Links between DBpedia and ((http://lod.geospecies.org/ GeoSpecies)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/geospecies Silk link specification)).//"),
  new Linkset("Links to GHO", "gho_links", "//Links between DBpedia and ((http://gho.aksw.org GHO)) (Global Health Observatory). Links created by ((http://aksw.org/Projects/LIMES LIMES)).//"),
  new Linkset("Links to Project Gutenberg", "gutenberg_links", "//Links between writers in DBpedia and data about them from ((www4.wiwiss.fu-berlin.de/gutendata/ Project Gutenberg)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/gutenberg Silk link specification)).//"),
  new Linkset("Links to Italian Public Schools", "italian_public_schools_links", "//Links between DBpedia and ((http://www.linkedopendata.it/datasets/scuole Italian Public Schools)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/italian-public-schools Silk link specification)).//"),
  new Linkset("Links to ~LinkedGeoData", "linkedgeodata_links", "//Links between DBpedia and ((http://linkedgeodata.org LinkedGeoData)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/linkedgeodata Silk link specifications)).//"),
  new Linkset("Links to ~LinkedMDB", "linkedmdb_links", "//Links between DBpedia and ((http://www.linkedmdb.org LinkedMDB)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/linkedmdb Silk link specifications)).//"),
  new Linkset("Links to ~MusicBrainz", "musicbrainz_links", "//Links between artists, albums and songs in DBpedia and data about them from ((http://musicbrainz.org/ MusicBrainz)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to New York Times", "nytimes_links", "//Links between ((http://www.nytimes.com/ New York Times)) subject headings and DBpedia concepts. Links copied from the original data set.//"),
  new Linkset("Links to ~OpenCyc", "opencyc_links", "//Links between DBpedia and ((http://opencyc.org/ OpenCyc)) concepts. ((OpenCyc Details)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/opencyc PHP script)).//"),
  new Linkset("Links to ~OpenEI (Open Energy Info)", "openei_links", "//Links between DBpedia and ((http://en.openei.org OpenEI)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/openei Silk link specifications)).//"),
  new Linkset("Links to Revyu", "revyu_links", "//Links between DBpedia and ((http://revyu.com Revyu)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to SIDER", "sider_links", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/sider/ SIDER)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/sider Silk link specifications)).//"),
  new Linkset("Links to RDF-TCM", "tcm_links", "//Links between DBpedia and ((http://code.google.com/p/junsbriefcase/wiki/RDFTCMData TCMGeneDIT)). Links copied from the original data set.//"),
  new Linkset("Links to UMBEL", "umbel_links", "//Links between DBpedia and ((http://umbel.org/ UMBEL)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to US Census", "uscensus_links", "//Links between US cities and states in DBpedia and data about them from ((http://www.rdfabout.com/demo/census/ US Census)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to ~WikiCompany", "wikicompany_links", "//Links between companies in DBpedia and companies in ((http://www4.wiwiss.fu-berlin.de/wikicompany/ WikiCompany)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to ~WordNet Classes", "wordnet_links", "//Classification links to ((http://www.w3.org/TR/wordnet-rdf/ RDF representations)) of ((http://wordnet.princeton.edu/ WordNet)) classes. Update mechanism: copy over from previous release.//"),
  new Linkset("YAGO links", "yago_links", "//Dataset containing owl:sameAs links between DBpedia and ((http://www.mpi-inf.mpg.de/yago-naga/yago/ YAGO)) instances. Update mechanism: ((https://github.com/dbpedia/extraction-framework/blob/dump/scripts/src/main/bash/process-yago.txt generated from latest YAGO datasets)).//"),
  new Linkset("YAGO type links", "yago_type_links", "//Dataset containing owl:equivalentClass links between types in the ~http://dbpedia.org/class/yago/ and ~http://yago-knowledge.org/resource/ namespaces. Update mechanism: ((https://github.com/dbpedia/extraction-framework/blob/dump/scripts/src/main/bash/process-yago.txt generated from latest YAGO datasets)).//"),
  new Linkset("YAGO types", "yago_types", "//Dataset containing ((http://www.mpi-inf.mpg.de/yago-naga/yago/ YAGO)) type information for DBpedia resources. Update mechanism: ((https://github.com/dbpedia/extraction-framework/blob/dump/scripts/src/main/bash/process-yago.txt generated from latest YAGO datasets)).//"),
  new Linkset("YAGO type hierarchy", "yago_taxonomy", "//Dataset containing the hierarchy of ((http://www.mpi-inf.mpg.de/yago-naga/yago/ YAGO)) classes in the ~http://dbpedia.org/class/yago/ namespace. Update mechanism: ((https://github.com/dbpedia/extraction-framework/blob/dump/scripts/src/main/bash/process-yago.txt generated from latest YAGO datasets)).//")
)

var target: File = null

def main(args: Array[String]) {
  require(args != null && args.length >= 1 && args(0).nonEmpty, "need one arg: file containing data file paths and their content numbers; second arg: target folder (optional)")
  loadTitles(new File(args(0)))
  if (args.length > 1) target = new File(args(1))
  generate
}

def generate: Unit = {
  val s = new StringPlusser+
  mark("")+
  "==DBpedia "+current+" Downloads==\n"+
  "\n"+
  "This pages provides downloads of the DBpedia datasets. The DBpedia datasets are licensed under the terms of the " +
  "((http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License Creative Commons Attribution-ShareAlike License)) " +
  "and the ((http://en.wikipedia.org/wiki/Wikipedia:Text_of_the_GNU_Free_Documentation_License GNU Free Documentation License)). " +
  "http://m.okfn.org/images/ok_buttons/od_80x15_red_green.png The downloads are provided as N-Triples and N-Quads, " +
  "where the N-Quads version contains additional provenance information for each statement. All files are ((http://www.bzip.org/ bzip2)) [[*1]] packed.\n"+
  "In addition to the RDF version of the data, we also provide a tabular version of some of the core DBpedia data sets as " +
  "CSV and JSON files. See ((DBpediaAsTables))."+
  "\n"+
  // ((Downloads36 DBpedia 3.6)), ((Downloads35 DBpedia 3.5.1)), ...
  "Older Versions: "+previous.map(version => "((Downloads"+tag(version)+" DBpedia "+version+"))").mkString(", ")+"\n"+
  "\n"+
  "See also the ((ChangeLog change log)) for recent changes and developments.\n"+
  "\n"+
  "{{ToC numerate=1 from=h2 to=h2}}\n" +
  "\n" +
  "=== Wikipedia Input Files ===\n" +
  "\n" +
  dumpDates+
  "\n" +
  include(OntologyPage)+
  include(DataC14NPage)+
  include(DataL10NPage)+
  include(LinksPage)+
  include(DescPage)+
  include(NLPPage)+
  "\n" +
  "[[#1]] Most files were packed with ((http://compression.ca/pbzip2/ pbzip2)), which generates concatenated streams. " +
  "Some older bzip2 decompressors, for example ((https://issues.apache.org/jira/browse/COMPRESS-162 Apache Commons Compress before version 1.4)), " +
  "cannot handle this format. Please make sure that you use the latest version. " +
  "((https://lists.sourceforge.net/lists/listinfo/dbpedia-discussion/ Let us know)) if you experience any problems.\n"+
  "\n" +
    mark("")
  
  write("", s.toString)
  
  ontologyPage(OntologyPage, "download-")
  datasetPages(DataC14NPage, "download-c14n-", datasets)
  datasetPages(DataL10NPage, "download-l10n-", datasets)
  datasetPages(LinksPage, "download-", List(linksets))
  descriptionPage(DescPage)
  nlpPage(NLPPage)
}
  
def ontologyPage(page: String, anchor: String): Unit = {
  val format = "owl"
    
  val file = ontology.file("", "", format, true)
  
  val s = new StringPlusser+
  mark(page)+
  "===Ontology===\n"+
  "#||\n"+
  "||**Dataset**|**"+format+"**||\n"+
  "||{{a name=\""+ontology.anchor(anchor)+"\"}}((#"+ontology.anchor()+" "+ontology.name+"))\n"+
  "|";

  link(s, file, format);

  s+
  "||\n"+
  "||#\n"+
  mark(page)
  
  write(page, s.toString)
}

def datasetPages(page: String, anchor: String, filesets: Seq[List[Fileset]]): Unit = {
  
  val s = new StringPlusser
  
  s+mark(page)
  
  page match {
    case DataC14NPage => {
      s+
      "===Canonicalized Datasets===\n"+
      "These datasets contain triples extracted from the respective Wikipedia whose subject and object resource have an equivalent English article. (("+l10nDatasetsSection+" more...))\n"+
      "\n"+
      "All DBpedia IRIs/URIs in the canonicalized datasets use the generic namespace //~http://dbpedia.org/resource/ //. For backwards compatibility, the N-Triples files (.nt, .nq) use URIs, e.g. //~http://dbpedia.org/resource/Bras%C3%ADlia //. The Turtle (.ttl) files use IRIs, e.g. //~http://dbpedia.org/resource/Brasília //.\n"
    }
    case DataL10NPage => {
      s+
      "===Localized Datasets===\n"+
      "These datasets contain triples extracted from the respective Wikipedia, including the ones whose URIs do not have an equivalent English article. (("+l10nDatasetsSection+" more...))\n"+
      "\n"+
      "The localized datasets use DBpedia IRIs (not URIs) and language-specific namespaces, e.g. //~http://pt.dbpedia.org/resource/Brasília//.\n"
    }
    case LinksPage => {
      s+
      "===Links to other datasets===\n"+
      "These datasets contain triples linking DBpedia to many other datasets.\n"+
      "\n"+
      "The URIs in these dumps use the generic namespace //~http://dbpedia.org/resource/ //.\n"
    }
  }
  
  s+
  "\n"+
  "**NOTE: You can find DBpedia dumps in "+allLanguages+" languages at our (("+dbpediaUrl+current+"/ DBpedia download server))," +
  "or alternatively at the (("+dbpediaMannheimUrl+current+"/ University of Mannheim DBpedia download server)).**\n"+
  "\n"+
  "//Click on the dataset names to obtain additional information. Click on the question mark next to a download link to preview file contents.//\n"
  
  for (subPage <- 0 until filesets.length) {
    s+include(page+subPage)
  }
  
  s+mark(page)
  
  write(page, s.toString)
  
  val langs = page match {
    case DataC14NPage => languages
    case DataL10NPage => languages.drop(1) /*drop en*/
    case LinksPage => List("links")
  }
  
  for (subPage <- 0 until filesets.length) {
    datasetPage(page, subPage, anchor, filesets(subPage), langs)
  }

}


def datasetPage(page: String, subPage: Int, anchor: String, filesets: List[Fileset], languages: Seq[String]): Unit = {
  
  val s = new StringPlusser
  
  s+
  mark(page+subPage)+
  "#||\n"+
  languages.mkString("||**Dataset**|**","**|**","**||")+"\n"
  
  for (fileset <- filesets; if fileset.pages == null || fileset.pages.contains(page)) {
    s+"||{{a name=\""+fileset.anchor(anchor)+"\"}}((#"+fileset.anchor()+" "+fileset.name.replaceChars("~", "")+"))\n"
    var first = true
    for (language <- languages) {
      
      val modifier = page match {
        case DataC14NPage => if (language == "en") "" else "-en-uris"
        case DataL10NPage => ""
        case LinksPage => ""
      }
      
      s+"|"
      for (format <- fileset.formats) {
        val file = fileset.file(language, modifier, format, false)
        if (file != null) {
          link(s, file, format);
        }
        else {
          s+"<#--#>\n"
        }
      }
      first = false
    }
    s+"||\n"
  }
  
  s+
  "||#\n"+
  mark(page+subPage)
  
  write(page+subPage, s.toString)
}

def link(s: StringPlusser, file: FileInfo, format: String): Unit = {
  s+
  "<#<small>"+
  "<a href=\""+file.downloadUrl+"\" title=\""+file.title+"\">"+format+"</a>\u00A0"+ // 00A0 is non-breaking space
  "<small><a href=\""+file.previewUrl+"\">?</a></small>"+
  "</small>#>\n"
}

def descriptionPage(page: String): Unit = {
  val s = new StringPlusser
  
  s+
  mark(page)+
  "=== Dataset Descriptions ===\n"+
  "\n"
  
  for (fileset <- ontology :: datasets.flatten ++ linksets) {
    s+
    "{{a name=\""+fileset.anchor()+"\"}}\n"+
    "==== "+fileset.name+" ====\n"+
    fileset.text+"\n"
  }
  
  s+
  mark(page)
 
  write(page, s.toString)
}

def nlpPage(page: String): Unit = {
  val s = new StringPlusser
  
  s+
  mark(page)+
  "===NLP Datasets===\n" +
  "\n"+
  "DBpedia also includes a number of ((Datasets/NLP NLP Datasets)) -- datasets specifically targeted " +
  "at supporting Computational Linguistics and Natural Language Processing (NLP) tasks. Among those, " +
  "we highlight the Lexicalization Dataset, Topic Signatures, Thematic Concepts and Grammatical Genders.\n"+
  mark(page)
 
  write(page, s.toString)
}

def mark(page: String): String = {
  "<#<!--\n\n\n"+
  "PLEASE DO NOT EDIT THIS WIKI PAGE!\n\n\n" +
  "YOUR CHANGES WILL BE LOST IN THE NEXT RELEASE!\n\n\n" +
  "Please edit CreateDownloadPage.scala instead.\n\n\n" +
  "Paste this result page of CreateDownloadPage.scala here:\n"+
  "http://oldwiki.dbpedia.org/Downloads"+tag(current)+page+"/edit\n\n\n"+
  "-->#>\n\n\n"
}

def include(page: String): String = {
  "{{Include page=/Downloads"+tag(current)+page+" nomark=1}}\n"
}

def write(page: String, content: String): Unit = {
  val writer = IOUtils.writer(new File(target, "Downloads"+tag(current)+page+".wacko"))
  try writer.write(content)
  finally writer.close()
}
  
}
