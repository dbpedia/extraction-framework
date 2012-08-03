package org.dbpedia.extraction.scripts

import java.util.Locale
import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringPlusser
import org.dbpedia.extraction.scripts.IOUtils.readLines
import scala.collection.mutable.{Map,HashMap}
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * Generate Wacko Wiki source text for http://wiki.dbpedia.org/Downloads and all its sub pages.
 * 
 * Example call:
 * 
 * ../run CreateDownloadPage src/main/data/lines-bytes-packed.txt
 */
object CreateDownloadPage {
  
val current = "3.8"
  
val downloads = "http://downloads.dbpedia.org/"
 
val dumps = "http://dumps.wikimedia.org/"
  
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
  else if (bytes < 1048576) formatter.format(bytes / 1024F)+"KB"
  else if (bytes < 1073741824) formatter.format(bytes / 1048576F)+"MB"
  else formatter.format(bytes / 1073741824F)+"GB"
}

def loadTitles(file: File): Unit = {
  // read lines in this format: 
  // dbpedia_3.8.owl  lines: 4622  bytes: 811552  gzip: 85765  bzip2: 50140
  // links/revyu_links.nt  lines: 6  bytes: 1008  gzip: 361  bzip2: 441
  // af/geo_coordinates_af.nt lines: 82 bytes: 11524 gzip: 1141 bzip2: 1228
  readLines(file) { line =>
    val parts = line.split("\\s+", -1)
    if (parts.length != 9) throw new IllegalArgumentException("bad line format")
    val path = parts(0)
    val lines = parts(2).toLong
    val bytes = parts(4).toLong
    val gzip = parts(6).toLong
    val bzip2 = parts(8).toLong
    titles(path) = "Lines: "+niceDecimal(lines)+"; Filesize(download): "+niceBytes(bzip2)+"; Filesize(unpacked): "+niceBytes(bytes)
  }
}

// path: dbpedia_3.8.owl, af/geo_coordinates_af.nq, links/revyu_links.nt
class FileInfo(val path: String, val title: String) {
  
  // example: 3.8/af/geo_coordinates_af.nq.bz2
  val fullPath = current+"/"+path+zipSuffix
  
  // example: http://downloads.dbpedia.org/3.8/af/geo_coordinates_af.nq.bz2
  val downloadUrl = downloads+fullPath
  
  // example: http://downloads.dbpedia.org/preview.php?file=3.8_sl_af_sl_geo_coordinates_af.nq.bz2
  val previewUrl = downloads+"preview.php?file="+fullPath.replace("/", "_sl_")
}
  
abstract class Fileset(
  val name: String,
  val file: String,
  val text: String,
  val formats: List[String],
  val languages: Boolean
) 
{
  // null if data file didn't contain any info about this file
  def file(language: String, modifier: String, format: String) = {
    val p = path(language, modifier, format)
    val t = titles.getOrElse(p, null)
    if (t == null) null else new FileInfo(p, t)
  }
  
  protected def path(language: String, modifier: String, format: String): String
  
  val anchor = name.replace(" ", "").toLowerCase(Locale.ENGLISH)
}

class Ontology(name: String, file: String, text: String)
extends Fileset(name, file, text, List("owl"), false)
{
  // dbpedia_3.8.owl
  override protected def path(language: String, modifier: String, format: String) = file+modifier+"."+format
}

class Dataset(name: String, file: String, text: String)
extends Fileset(name, file, text, List("nt", "nq", "ttl"), true)
{  
  // example: af/geo_coordinates_af.nt
  override protected def path(language: String, modifier: String, format: String) = language+"/"+file+modifier+"_"+language+"."+format
}

class Linkset(name: String, file: String, text: String)
extends Fileset(name, file, text, List("nt"), false)
{
  // example: links/revyu_links.nt
  override protected def path(language: String, modifier: String, format: String) = "links/"+file+modifier+"_links."+format
}

def tag(version: String): String = version.replace(".", "")

val previous = List("3.7", "3.6", "3.5.1", "3.5", "3.4", "3.3", "3.2", "3.1", "3.0", "3.0RC", "2.0")

val dumpDates = "in late May / early June 2012"
  
val allLanguages = 111

// All languages that have a significant number of mapped articles.
// en must be first, we use languages.drop(1) in datasetPages()
val languages = List("en","bg","ca","cs","de","el","es","fr","hu","it","ko","pl","pt","ru","sl","tr")

val ontology =
new Ontology("DBpedia Ontology", "dbpedia_"+current, "//The DBpedia ontology in OWL. See ((http://jens-lehmann.org/files/2009/dbpedia_jws.pdf our JWS paper)) for more details.//")

// We have to split the main datasets into many small sub-pages because wacko wiki is true to its
// name and fails to display a page (or even print a proper error message) when it's too long. 
val datasets = List(
  List(
    new Dataset("Ontology Infobox Types", "instance_types", "//Contains triples of the form $object rdf:type $class from the ontology-based extraction.//"),
    new Dataset("Ontology Infobox Properties", "mappingbased_properties", "//High-quality data extracted from Infoboxes using the ontology-based extraction. The predicates in this dataset are in the /ontology/ namespace.//\n  Note that this data is of much higher quality than the Raw Infobox Properties in the /property/ namespace. For example, there are three different raw Wikipedia infobox properties for the birth date of a person. In the the /ontology/ namespace, they are all **mapped onto one relation** http://dbpedia.org/ontology/birthDate. It is a strong point of DBpedia to unify these relations."),
    new Dataset("Ontology Infobox Properties (Specific)", "specific_mappingbased_properties", "//Infobox data from the ontology-based extraction, using units of measurement more convenient for the resource type, e.g. square kilometres instead of square metres for the area of a city.//")
  ),
  List(
    new Dataset("Titles", "labels", "//Titles of all Wikipedia Articles in the corresponding language.//"),
    new Dataset("Short Abstracts", "short_abstracts", "//Short Abstracts (max. 500 chars long) of Wikipedia articles//"),
    new Dataset("Extended Abstracts", "long_abstracts", "//Additional, extended English abstracts.//"),
    new Dataset("Images", "images", "//Main image and corresponding thumbnail from Wikipedia article.//")
  ),
  List(
    new Dataset("Geographic Coordinates", "geo_coordinates", "//Geographic coordinates extracted from Wikipedia.//"),
    new Dataset("Raw Infobox Properties", "infobox_properties", "//Information that has been extracted from Wikipedia infoboxes. Note that this data is in the less clean /property/ namespace. The Ontology Infobox Properties (/ontology/ namespace) should always be preferred over this data.//"),
    new Dataset("Raw Infobox Property Definitions", "infobox_property_definitions", "//All properties / predicates used in infoboxes.//"),
    new Dataset("Homepages", "homepages", "//Links to homepages of persons, organizations etc.//")
  ),
  List(
    new Dataset("Persondata", "persondata", "//Information about persons (date and place of birth etc.) extracted from the English and German Wikipedia, represented using the FOAF vocabulary.//"),
    new Dataset("PND", "pnd", "//Dataset containing PND (Personennamendatei) identifiers.//"),
    new Dataset("Inter-Language Links", "interlanguage_links", "//Dataset linking a DBpedia resource to the same or a related resource in other languages, extracted from the ((http://en.wikipedia.org/wiki/Help:Interlanguage_links inter-language links)) of a Wikipedia article.//"),
    new Dataset("Bijective Inter-Language Links", "interlanguage_links_same_as", "//Dataset containing the bijective inter-language links between a DBpedia resource and the same resource in other languages, i.e. there is a link from a resource to the same resource in a different language and a link pointing back. When inter-language links are bijective, the Wikipedia articles are usually about the same subject.//"),
    new Dataset("Non-bijective Inter-Language Links", "interlanguage_links_see_also", "//Dataset containing the inter-language links between a DBpedia resource and related resources in other languages that are not bijective, i.e. there is a link from a resource to a related resource in a different language, but no link pointing back. When inter-language links are not bijective, the Wikipedia articles are usually not about the same subject.//")
  ),
  List(
    new Dataset("Articles Categories", "article_categories", "//Links from concepts to categories using the SKOS vocabulary.//"),
    new Dataset("Categories (Labels)", "category_labels", "//Labels for Categories.//"),
    new Dataset("Categories (Skos)", "skos_categories", "//Information which concept is a category and how categories are related using the SKOS Vocabulary.//")
  ),
  List(
    new Dataset("External Links", "external_links", "//Links to external web pages about a concept.//"),
    new Dataset("Links to Wikipedia Article", "wikipedia_links", "//Dataset linking DBpedia resource to corresponding article in Wikipedia.//"),
    new Dataset("Wikipedia Pagelinks", "page_links", "//Dataset containing internal links between DBpedia instances. The dataset was created from the internal links between Wikipedia articles. The dataset might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms.//")
  ),
  List(
    new Dataset("Redirects", "redirects", "//Dataset containing redirects between articles in Wikipedia.//"),
    new Dataset("Transitive Redirects", "redirects_transitive", "//Redirects dataset in which multiple redirects have been resolved and redirect cycles have been removed.//"),
    new Dataset("Disambiguation links", "disambiguations", "//Links extracted from Wikipedia ((http://en.wikipedia.org/wiki/Wikipedia:Disambiguation disambiguation)) pages. Since Wikipedia has no syntax to distinguish disambiguation links from ordinary links, DBpedia has to use heuristics.//"),
    new Dataset("IRI-same-as-URI links", "iri_same_as_uri", "//owl:sameAs links between the ((http://tools.ietf.org/html/rfc3987 IRI)) and ((http://tools.ietf.org/html/rfc3986 URI)) format of DBpedia resources. Only extracted when IRI and URI are actually different.//")
  ),
  List(
    new Dataset("Page IDs", "page_ids", "//Dataset linking a DBpedia resource to the page ID of the Wikipedia article the data was extracted from.//"),
    new Dataset("Revision IDs", "revision_ids", "//Dataset linking a DBpedia resource to the revision ID of the Wikipedia article the data was extracted from. Until DBpedia 3.7, these files had names like 'revisions_en.nt'. Since DBpedia 3.8, they were renamed to 'revisions_ids_en.nt' to distinguish them from the new 'revision_uris_en.nt' files.//"),
    new Dataset("Revision URIs", "revision_uris", "//Dataset linking DBpedia resource to the specific Wikipedia article revision used in this DBpedia release.//")
  )
)

val linksets = List(
  new Linkset("Links to Amsterdam Museum data", "amsterdammuseum", "//Links between DBpedia and ((http://semanticweb.cs.vu.nl/lod/am/ Amsterdam Museum)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/amsterdammuseum Silk link specification)).//"),
  new Linkset("Links to BBC Wildlife Finder", "bbcwildlife", "//Links between DBpedia and ((http://www.bbc.co.uk/wildlifefinder/ BBC Wildlife Finder)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/bbcwildlife Silk link specification)).//"),
  new Linkset("Links to RDF Bookmashup", "bookmashup", "//Links between books in DBpedia and data about them provided by the ((http://www4.wiwiss.fu-berlin.de/bizer/bookmashup/ RDF Book Mashup)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to Bricklink", "bricklink", "//Links between DBpedia and ((http://kasabi.com/dataset/bricklink Bricklink)). Links created manually.//"),
  new Linkset("Links to CORDIS", "cordis", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/cordis/ CORDIS)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/cordis Silk link specifications)).//"),
  new Linkset("Links to DailyMed", "dailymed", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/dailymed/ DailyMed)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dailymed Silk link specifications)).//"),
  new Linkset("Links to DBLP", "dblp", "//Links between computer scientists in DBpedia and their publications in the ((http://www.informatik.uni-trier.de/~ley/db/ DBLP)) database. Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dblp Silk link specification)).//"),
  new Linkset("Links to DBTune", "dbtune", "//Links between DBpedia and ((http://dbtune.org/ DBTune)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/dbtune Silk link specifications)).//"),
  new Linkset("Links to Diseasome", "diseasome", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/diseasome/ Diseasome)).  Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/diseasome Silk link specifications)).//"),
  new Linkset("Links to DrugBank", "drugbank", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/drugbank/ DrugBank)).  Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/drugbank Silk link specifications)).//"),
  new Linkset("Links to EUNIS", "eunis", "//Links between DBpedia and ((http://eunis.eea.europa.eu EUNIS)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/eunis Silk link specification)).//"),
  new Linkset("Links to Eurostat (Linked Statistics)", "eurostat_linkedstatistics", "//Links between DBpedia and ((http://eurostat.linked-statistics.org Eurostat (Linked Statistics))). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/eurostat_linkedstatistics Silk link specifications)).//"),
  new Linkset("Links to Eurostat (WBSG)", "eurostat_wbsg", "//Links between countries and regions in DBpedia and data about them from ((http://www4.wiwiss.fu-berlin.de/eurostat/ Eurostat (WBSG))). Links created manually.//"),
  new Linkset("Links to CIA World Factbook", "factbook", "//Links between DBpedia and the ((http://www4.wiwiss.fu-berlin.de/factbook/ CIA World Factbook)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/factbook Silk link specifications)).//"),
  new Linkset("Links to flickr wrappr", "flickrwrappr", "//Links between DBpedia concepts and photo collections depicting them generated by ((http://www4.wiwiss.fu-berlin.de/flickrwrappr/ flickr wrappr)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/extraction_framework/file/dump/core/src/main/scala/org/dbpedia/extraction/mappings/FlickrWrapprLinkExtractor.scala Scala extractor)).//"),
  new Linkset("Links to Freebase", "freebase", "//Links between DBpedia and ((http://www.freebase.com/ Freebase (MIDs))). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/extraction_framework/file/dump/scripts/src/main/scala/org/dbpedia/extraction/scripts/CreateFreebaseLinks.scala Scala script)).//"),
  new Linkset("Links to GADM", "gadm", "//Links between DBpedia and ((http://gadm.geovocab.org/ GADM)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to GeoNames", "geonames", "//Links between geographic places in DBpedia and data about them from ((http://www.geonames.org/ GeoNames)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/geonames Silk link specifications)).//"),
  new Linkset("Links to GeoSpecies", "geospecies", "//Links between DBpedia and ((http://lod.geospecies.org/ GeoSpecies)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/geospecies Silk link specification)).//"),
  new Linkset("Links to Project Gutenberg", "gutenberg", "//Links between writers in DBpedia and data about them from ((www4.wiwiss.fu-berlin.de/gutendata/ Project Gutenberg)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/gutenberg Silk link specification)).//"),
  new Linkset("Links to Italian Public Schools", "italian_public_schools", "//Links between DBpedia and ((http://www.linkedopendata.it/datasets/scuole Italian Public Schools)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/italian-public-schools Silk link specification)).//"),
  new Linkset("Links to LinkedGeoData", "linkedgeodata", "//Links between DBpedia and ((http://linkedgeodata.org LinkedGeoData)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/linkedgeodata Silk link specifications)).//"),
  new Linkset("Links to LinkedMDB", "linkedmdb", "//Links between DBpedia and ((http://www.linkedmdb.org LinkedMDB)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/linkedmdb Silk link specifications)).//"),
  new Linkset("Links to MusicBrainz", "musicbrainz", "//Links between artists, albums and songs in DBpedia and data about them from ((http://zitgist.com/music/ MusicBrainz)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to New York Times", "nytimes", "//Links between ((http://www.nytimes.com/ New York Times)) subject headings and DBpedia concepts. Links copied from the original data set.//"),
  new Linkset("Links to OpenCyc", "opencyc", "//Links between DBpedia and ((http://opencyc.org/ OpenCyc)) concepts. ((OpenCyc Details)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/opencyc PHP script)).//"),
  new Linkset("Links to OpenEI (Open Energy Info)", "openei", "//Links between DBpedia and ((http://en.openei.org OpenEI)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/openei Silk link specifications)).//"),
  new Linkset("Links to Revyu", "revyu", "//Links between DBpedia and ((http://revyu.com Revyu)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to SIDER", "sider", "//Links between DBpedia and ((http://www4.wiwiss.fu-berlin.de/sider/ SIDER)). Links created by ((http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/external_datasets/sider Silk link specifications)).//"),
  new Linkset("Links to RDF-TCM", "tcm", "//Links between DBpedia and ((http://code.google.com/p/junsbriefcase/wiki/RDFTCMData TCMGeneDIT)). Links copied from the original data set.//"),
  new Linkset("Links to UMBEL", "umbel", "//Links between DBpedia and ((http://umbel.org/ UMBEL)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to US Census", "uscensus", "//Links between US cities and states in DBpedia and data about them from ((http://www.rdfabout.com/demo/census/ US Census)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to WikiCompany", "wikicompany", "//Links between companies in DBpedia and companies in ((http://www4.wiwiss.fu-berlin.de/wikicompany/ WikiCompany)). Update mechanism: copy over from previous release.//"),
  new Linkset("Links to WordNet Classes", "wordnet", "//Classification links to ((http://www.w3.org/TR/wordnet-rdf/ RDF representations)) of ((http://wordnet.princeton.edu/ WordNet)) classes. Update mechanism: copy over from previous release.//"),
  new Linkset("Links to YAGO2", "yago", "//Dataset containing links between DBpedia and YAGO, YAGO type information for DBpedia resources and the YAGO class hierarchy. Currently maintained by Johannes Hoffart.//")
)

val OntologyPage = "Ontology"
val DataC14NPage = "DataC14N"
val DataI18NPage = "DataI18N"
val LinksPage = "Links"
val DescPage = "Desc"
val NLPPage = "NLP"

def main(args: Array[String]) {
  require(args != null && args.length == 1 && args(0).nonEmpty, "need 1 arg: file containing data file paths and their content numbers")
  loadTitles(new File(args(0)))
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
  "where the N-Quads version contains additional provenance information for each statement. All files are ((http://www.bzip.org/ bzip2)) packed.\n"+
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
  "The datasets were extracted from (("+dumps+" Wikipedia dumps)) generated "+dumpDates+
  " (see also all ((DumpDatesDBpedia"+tag(current)+" specific dates and times))).\n" +
  "\n" +
  include(OntologyPage)+
  include(DataC14NPage)+
  include(DataI18NPage)+
  include(LinksPage)+
  include(DescPage)+
  include(NLPPage)+
  mark("")
  
  write("", s.toString)
  
  ontologyPage(OntologyPage)
  datasetPages(DataC14NPage, datasets)
  datasetPages(DataI18NPage, datasets)
  datasetPages(LinksPage, List(linksets))
  descriptionPage(DescPage)
  nlpPage(NLPPage)
}
  
def ontologyPage(page: String): Unit = {
  val format = "owl"
    
  val file = ontology.file("", "", format)
  
  val s = new StringPlusser+
  mark(page)+
  "===Ontology===\n"+
  "#||\n"+
  "||**Dataset**|**"+format+"**||\n"+
  "||((#"+ontology.anchor+" "+ontology.name+"))\n"+
  "|"+
  "<#<small>"+
  "<a href=\""+file.downloadUrl+"\" title=\""+file.title+"\">"+format+"</a> "+
  "<small><a href=\""+file.previewUrl+"\">?</a></small>"+
  "</small>#>\n"+
  "||\n"+
  "||#\n"+
  mark(page)
  
  write(page, s.toString)
}

def datasetPages(page: String, filesets: Seq[List[Fileset]]): Unit = {
  
  val s = new StringPlusser
  
  s+mark(page)
  
  page match {
    case DataC14NPage => {
      s+
      "===Canonicalized Datasets===\n"+
      "These datasets contain triples extracted from the respective Wikipedia whose subject and object resource have an equivalent English article. ((Datasets#h18-19 more...))\n"+
      "\n"+
      "All IRIs/URIs in these dumps use the generic namespace http://dbpedia.org/ . " +
      "The N-Triples files (.nt, .nq) use URIs for English and IRIs for all other languages. The Turtle (.ttl) files use IRIs for //all// languages, even for English.\n"
    }
    case DataI18NPage => {
      s+
      "===Internationalized Datasets===\n"+
      "These datasets contain triples extracted from the respective Wikipedia, including the ones whose URIs do not have an equivalent English article. ((Datasets#h18-19 more...))\n"+
      "\n"+
      "These dumps contain IRIs using language-specific namespaces (e.g. http://el.dbpedia.org/...).\n"
    }
    case LinksPage => {
      s+
      "===Links to other datasets===\n"+
      "These datasets contain triples linking DBpedia to many other datasets.\n"+
      "\n"+
      "The URIs in these dumps use the generic namespace http://dbpedia.org/ .\n"
    }
  }
  
  s+
  "\n"+
  "**NOTE: You can find DBpedia dumps in "+allLanguages+" languages at our (("+downloads+current+"/ DBpedia download server)).**\n"+
  "\n"+
  "//Click on the dataset names to obtain additional information. Click on the question mark next to a download link to preview file contents.//\n"
  
  for (subPage <- 0 until filesets.length) {
    s+include(page+subPage)
  }
  
  s+mark(page)
  
  write(page, s.toString)
  
  val langs = page match {
    case DataC14NPage => languages
    case DataI18NPage => languages.drop(1) /*drop en*/
    case LinksPage => List("links")
  }
  
  for (subPage <- 0 until filesets.length) {
    datasetPage(page, subPage, filesets(subPage), langs)
  }

}


def datasetPage(page: String, subPage: Int, filesets: List[Fileset], languages: Seq[String]): Unit = {
  
  val s = new StringPlusser
  
  s+
  mark(page+subPage)+
  "#||\n"+
  languages.mkString("||**Dataset**|**","**|**","**||")+"\n"
  
  for (fileset <- filesets) {
    s+"||((#"+fileset.anchor+" "+fileset.name+"))\n"
    var first = true
    for (language <- languages) {
      
      val modifier = page match {
        case DataC14NPage => if (language == "en") "" else "_en_uris"
        case DataI18NPage => ""
        case LinksPage => ""
      }
      
      s+"|"
      for (format <- fileset.formats) {
        val file = fileset.file(language, modifier, format)
        if (file != null && (fileset.languages || first)) {
          s+
          "<#<small>"+
          "<a href=\""+file.downloadUrl+"\" title=\""+file.title+"\">"+format+"</a> "+
          "<small><a href=\""+file.previewUrl+"\">?</a></small>"+
          "</small>#>\n"
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

def descriptionPage(page: String): Unit = {
  val s = new StringPlusser
  
  s+
  mark(page)+
  "=== Dataset Descriptions ===\n"+
  "\n"
  
  for (fileset <- ontology :: datasets.flatten ++ linksets) {
    s+
    "{{a name=\""+fileset.anchor+"\"}}\n"+
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
  "<#<!--\n"+
  "DO NOT EDIT - generated by CreateDownloadPage.scala\n"+
  "http://wiki.dbpedia.org/Downloads"+tag(current)+page+"/edit\n"+
  "-->#>\n"
}

def include(page: String): String = {
  "{{include page=/Downloads"+tag(current)+page+" nomark=1}}\n"
}

def write(page: String, content: String): Unit = {
  val writer = IOUtils.write(new File("Downloads"+tag(current)+page+".wacko"))
  try writer.write(content)
  finally writer.close()
}
  
}