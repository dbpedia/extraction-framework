package org.dbpedia.extraction.scripts

import java.util.Locale
import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.scripts.IOUtils.readLines
import scala.collection.mutable.HashMap

abstract class Fileset(
  val name: String,
  val file: String,
  val text: String,
  val formats: List[String],
  val languages: Boolean
) {
  def path(language: String, suffix: String)
  val anchor = name.replace(" ", "").toLowerCase(Locale.ENGLISH)
}

class Dataset(name: String, file: String, text: String)
extends Fileset(name, file, text, List("nt", "nq"), true)
{  
  override def path(language: String, suffix: String) = "/"+language+"/"+file+"_"+language+"."+suffix+".bz2"
}

class Linkset(name: String, file: String, text: String)
extends Fileset(name, file, text, List("nt"), false)
{
  override def path(language: String, suffix: String) = "/links/"+file+"_links."+suffix+".bz2"
}

class Ontology(name: String, file: String, text: String)
extends Fileset(name, file, text, List("owl"), false)
{
  override def path(language: String, suffix: String) = "/"+file+"."+suffix+".bz2"
}

class Numbers(val name: String, val lines: Int, val bytes: Long, val gzip: Long, val bzip2: Long)

object CreateDownloadPage {
  
val current = "3.8"
  
val previous = Array("3.7", "3.6", "3.5.1", "3.5", "3.4", "3.3", "3.2", "3.1", "3.0", "3.0RC", "2.0")

val dumpDates = "in late May / early June 2012"
  
val languages = Array("en","de","fr","nl","it","pl","es","ru","ja","pt","zh","sv","vi","uk","ca","no","fi","cs","hu","ko")

val downloads = "downloads.dbpedia.org"
 
val dumps = "dumps.wikimedia.org"
  
val ontology = List(
new Ontology("DBpedia Ontology", "dbpedia_"+current, "//The DBpedia ontology in OWL. See ((http://jens-lehmann.org/files/2009/dbpedia_jws.pdf our JWS paper)) for more details.//")
)

val datasets = List(
new Dataset("Ontology Infobox Types", "instance_types", "//Contains triples of the form $object rdf:type $class from the ontology-based extraction.//"),
new Dataset("Ontology Infobox Properties", "mappingbased_properties", "//High-quality data extracted from Infoboxes using the ontology-based extraction. The predicates in this dataset are in the /ontology/ namespace.//\n  Note that this data is of much higher quality than the Raw Infobox Properties in the /property/ namespace. For example, there are three different raw Wikipedia infobox properties for the birth date of a person. In the the /ontology/ namespace, they are all **mapped onto one relation** http://dbpedia.org/ontology/birthDate. It is a strong point of DBpedia to unify these relations."),
new Dataset("Ontology Infobox Properties (Specific)", "specific_mappingbased_properties", "//Infobox data from the ontology-based extraction, using units of measurement more convenient for the resource type, e.g. square kilometres instead of square metres for the area of a city.//"),
new Dataset("Titles", "labels", "//Titles of all Wikipedia Articles in the corresponding language.//"),
new Dataset("Short Abstracts", "short_abstracts", "//Short Abstracts (max. 500 chars long) of Wikipedia articles//"),
new Dataset("Extended Abstracts", "long_abstracts", "//Additional, extended English abstracts.//"),
new Dataset("Images", "images", "//Main image and corresponding thumbnail from Wikipedia article.//"),
new Dataset("Geographic Coordinates", "geo_coordinates", "//Geographic coordinates extracted from Wikipedia.//"),
new Dataset("Raw Infobox Properties", "infobox_properties", "//Information that has been extracted from Wikipedia infoboxes. Note that this data is in the less clean /property/ namespace. The Ontology Infobox Properties (/ontology/ namespace) should always be preferred over this data.//"),
new Dataset("Raw Infobox Property Definitions", "infobox_property_definitions", "//All properties / predicates used in infoboxes.//"),
new Dataset("Homepages", "homepages", "//Links to homepages of persons, organizations etc.//"),
new Dataset("Persondata", "persondata", "//Information about persons (date and place of birth etc.) extracted from the English and German Wikipedia, represented using the FOAF vocabulary.//"),
new Dataset("PND", "pnd", "//Dataset containing PND (Personennamendatei) identifiers.//"),
new Dataset("Articles Categories", "article_categories", "//Links from concepts to categories using the SKOS vocabulary.//"),
new Dataset("Categories (Labels)", "category_labels", "//Labels for Categories.//"),
new Dataset("Categories (Skos)", "skos_categories", "//Information which concept is a category and how categories are related using the SKOS Vocabulary.//"),
new Dataset("External Links", "external_links", "//Links to external web pages about a concept.//"),
new Dataset("Links to Wikipedia Article", "wikipedia_links", "//Dataset linking DBpedia resource to corresponding article in Wikipedia.//"),
new Dataset("Wikipedia Pagelinks", "page_links", "//Dataset containing internal links between DBpedia instances. The dataset was created from the internal links between Wikipedia articles. The dataset might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms.//"),
new Dataset("Redirects", "redirects", "//Dataset containing redirects between articles in Wikipedia.//"),
new Dataset("Transitive Redirects", "redirects_transitive", "//Redirects dataset in which multiple redirects have been resolved and redirect cycles have been removed.//"),
new Dataset("Disambiguation links", "disambiguations", "//Links extracted from Wikipedia ((http://en.wikipedia.org/wiki/Wikipedia:Disambiguation disambiguation)) pages. Since Wikipedia has no syntax to distinguish disambiguation links from ordinary links, DBpedia has to use heuristics.//"),
new Dataset("Page IDs", "page_ids", "//Dataset linking a DBpedia resource to the page ID of the Wikipedia article the data was extracted from.//"),
new Dataset("Revision IDs", "revision_ids", "//Dataset linking a DBpedia resource to the revision ID of the Wikipedia article the data was extracted from.//"),
new Dataset("Revision URIs", "revision_uris", "//Dataset linking DBpedia resource to the specific Wikipedia article revision used in this DBpedia release.//"),
new Dataset("Inter-Language Links", "interlanguage_links", "//Dataset linking a DBpedia resource to the same or a related resource in other languages, extracted from the ((http://en.wikipedia.org/wiki/Help:Interlanguage_links inter-language links)) of a Wikipedia article.//"),
new Dataset("Bijective Inter-Language Links", "interlanguage_links_same_as", "//Dataset containing the bijective inter-language links between a DBpedia resource and the same resource in other languages, i.e. there is a link from a resource to the same resource in a different language and a link pointing back. When inter-language links are bijective, the Wikipedia articles are usually about the same subject.//"),
new Dataset("Non-bijective Inter-Language Links", "interlanguage_links_see_also", "//Dataset containing the inter-language links between a DBpedia resource and related resources in other languages that are not bijective, i.e. there is a link from a resource to a related resource, but no link pointing back. When inter-language links are not bijective, the Wikipedia articles are usually not about the same subject.//")
)

val linksets = List(
new Linkset("Links to Amsterdam Museum data", "amsterdammuseum", "//Links to((http://semanticweb.cs.vu.nl/lod/am/ Amsterdam Museum data)). Update mechanism: TODO.//"),
new Linkset("Links to BBC Wildlife", "bbcwildlife", "//Links to ((http://www.bbc.co.uk/nature/wildlife BBC Wildlife)). Update mechanism: TODO.//"),
new Linkset("Links to RDF Bookmashup", "bookmashup", "//Links between books in DBpedia and data about them provided by the ((http://www4.wiwiss.fu-berlin.de/bizer/bookmashup/ RDF Book Mashup)). Provided by Georgi Kobilarov. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to Bricklink", "bricklink", "//Links between DBpedia and ((http://kasabi.com/dataset/bricklink Bricklink)).//"),
new Linkset("Links to CORDIS", "cordis", "//Links to ((http://cordis.europa.eu/home_en.html CORDIS)). Update mechanism: TODO.//"),
new Linkset("Links to DailyMed", "dailymed", "//Links between DBpedia and ((http://dailymed.nlm.nih.gov/ DailyMed)). Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to DBLP", "dblp", "//Links between computer scientists in DBpedia and their publications in the ((http://www.informatik.uni-trier.de/~ley/db/ DBLP)) database. Links were created manually. Update mechanism: Copy over from previous release.//"),
new Linkset("Links to DBTune", "dbtune", "//Links to ((http://dbtune.org/ DBTune)). Update mechanism: TODO.//"),
new Linkset("Links to Diseasome", "diseasome", "//Links between DBpedia and ((http://diseasome.eu/ Diseasome)). Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to DrugBank", "drugbank", "//Links between DBpedia and ((http://www.drugbank.ca/ DrugBank)). Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to EUnis", "eunis", "//TODO//"),
new Linkset("Links to Eurostat at Linked Statistics", "eurostat_linkedstatistics", "//Links to ((http://eurostat.linked-statistics.org/ Eurostat at Linked Statistics)). Update mechanism: TODO.//"),
new Linkset("Links to Eurostat WBSG", "eurostat_wbsg", "//Links between countries and regions in DBpedia and data about them from ((http://ec.europa.eu/eurostat Eurostat)). Links were created manually. Update mechanism: Copy over from previous release.//"),
new Linkset("Links to CIA Factbook", "factbook", "//Links between countries in DBpedia and data about them from ((https://www.cia.gov/library/publications/the-world-factbook/ CIA Factbook)). Links were created manually. Update mechanism: Copy over from previous release.//"),
new Linkset("Links to flickr wrappr", "flickrwrappr", "//Links between DBpedia concepts and photo collections depicting them generated by the ((http://www4.wiwiss.fu-berlin.de/flickrwrappr/ flickr wrappr)). Update mechanism: Scala script in DBpedia source code repository.//"),
new Linkset("Links to Freebase", "freebase", "//Links between DBpedia and ((http://www.freebase.com/ Freebase)) (MIDs). Update mechanism: Scala script in Mercurial.//"),
new Linkset("Links to GADM", "gadm", "//Links between places in DBpedia and ((http://gadm.geovocab.org/ GADM)).//"),
new Linkset("Links to Geonames", "geonames", "//Links between geographic places in DBpedia and data about them in the ((http://www.geonames.org/ Geonames)) database. Provided by the Geonames people. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to GeoSpecies", "geospecies", "//Links between species in DBpedia and ((http://lod.geospecies.org/ GeoSpecies)).//"),
new Linkset("Links to Project Gutenberg", "gutenberg", "//Links between writers in DBpedia and data about them from ((http://www.gutenberg.org/ Project Gutenberg)). Update mechanism: script in Mercurial. Since this requires manual changes of files and a D2R installation, it will be copied over from the previous DBpedia version and updated between releases by the maintainers (Piet Hensel and Georgi Kobilarov).//"),
new Linkset("Links to Italian Public Schools", "italian_public_schools", "//Links between DBpedia and ((http://www.linkedopendata.it/datasets/scuole Italian Public Schools)).//"),
new Linkset("Links to LinkedGeoData", "linkedgeodata", "//Links to ((http://linkedgeodata.org/ LinkedGeoData)). Update mechanism: TODO.//"),
new Linkset("Links to LinkedMDB", "linkedmdb", "//TODO//"),
new Linkset("Links to MusicBrainz", "musicbrainz", "//Links between artists, albums and songs in DBpedia and data about them from ((http://musicbrainz.org/ MusicBrainz)). Created manually using the result of SPARQL queries. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to New York Times", "nytimes", "//Links between New York Times subject headings and DBpedia concepts.//"),
new Linkset("Links to Cyc", "opencyc", "//Links between DBpedia and ((http://opencyc.org/ Cyc)) concepts. ((OpenCyc Details)). Update mechanism: awk script.//"),
new Linkset("Links to OpenEI", "openei", "//Links to ((http://en.openei.org/datasets/ OpenEI)). Update mechanism: TODO.//"),
new Linkset("Links to Revyu", "revyu", "//Links to Reviews about things in ((http://revyu.com/ Revyu)). Created manually by Tom Heath. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to SIDER", "sider", "//Links between DBpedia and ((http://sideeffects.embl.de/ SIDER)). Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to TCMGeneDIT", "tcm", "//Links between DBpedia and ((http://tcm.lifescience.ntu.edu.tw/ TCMGeneDIT)). Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to Umbel", "umbel", "//TODO//"),
new Linkset("Links to US Census", "uscensus", "//Links between US cities and states in DBpedia and data about them from US Census. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to WikiCompany", "wikicompany", "//Links between companies in DBpedia and companies in ((http://wikicompany.org/ Wikicompany)). Update mechanism: script in Mercurial.//"),
new Linkset("Links to WordNet", "wordnet", "//Classification links to ((http://www.w3.org/TR/wordnet-rdf/ RDF representations)) of ((http://wordnet.princeton.edu/ WordNet)) classes. Update mechanism: unclear/copy over from previous release.//"),
new Linkset("Links to YAGO2", "yago", "//Dataset containing links between DBpedia and YAGO, YAGO type information for DBpedia resources and the YAGO class hierarchy. Currently maintained by Johannes Hoffart.//")
)

var numbers: Map[String, Numbers] = null

def readNumbers(file: File): Unit = {
  
  val numbers = new HashMap
  
  // read lines in this format: 
  // ontology.owl  lines: 4622  bytes: 811552  gzip: 85765  bzip2: 50140
  // frwiki/20120601/frwiki-20120601-persondata.nq  lines: 2  bytes: 64  gzip: 61  bzip2: 85
  // enwiki/20120601/links/revyu_links.nt  lines: 6  bytes: 1008  gzip: 361  bzip2: 441
  readLines(file) { line =>
    
  }
}

def main(args: Array[String]) {
  require(args != null && args.length == 1 && args(0).nonEmpty, "need 1 arg: file containing lines, unpacked and packed sizes of data files")
  readNumbers(new File(args(0)))
  print(main)
  print(f)
}

def main: String = {
val short = current.replace(".", "")

"==DBpedia "+current+" Downloads==\n"+
"\n"+
"This pages provides downloads of the DBpedia datasets. The DBpedia datasets are licensed under the terms of the " +
"((http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License Creative Commons Attribution-ShareAlike License)) " +
"and the ((http://en.wikipedia.org/wiki/Wikipedia:Text_of_the_GNU_Free_Documentation_License GNU Free Documentation License)). " +
"http://m.okfn.org/images/ok_buttons/od_80x15_red_green.png The downloads are provided as N-Triples and N-Quads, " +
"where the N-Quads version contains additional provenance information for each statement. All files are bz2 packed.\n"+
"\n"+
// ((Downloads36 DBpedia 3.6)), ((Downloads35 DBpedia 3.5.1)), ...
"Older Versions: "+previous.map(v => "((Downloads"+v.replace(".", "")+" DBpedia "+v+"))").mkString(", ")+"\n"+
"\n"+
"See also the ((ChangeLog change log)) for recent changes and developments.\n"+
"\n"+
"{{ToC numerate=1 from=h2 to=h2}}\n" +
"\n" +
"=== Wikipedia Input Files ===\n" +
"\n" +
"The datasets were extracted from ((http://"+dumps+"/ Wikipedia dumps)) generated " +
dumpDates+" (see also all ((DumpDatesDBpedia"+current.replace(".", "")+" specific dates and times))).\n" +
"\n" +
include("f")+
include("a")+
include("c")+
include("d")+
include("nlp")
}
  
def include(ext: String): String = {
"{{include page=/Downloads"+current.replace(".", "")+ext+" nomark=1}}\n\n"
}
  
def f: String = {
"===Core Datasets===\n" +
"**NOTE: You can find DBpedia dumps in 97 languages at our ((http://"+downloads+"/"+current+"/ DBpedia download server)).**\n" +
"\n" +
"//Click on the dataset names to obtain additional information.//\n" +
"#||\n" +
languages.mkString("||**Dataset**|**","**|**","**||")+"\n" +
"||#\n" +
"\n" +
include("h")+
include("g")
}

}