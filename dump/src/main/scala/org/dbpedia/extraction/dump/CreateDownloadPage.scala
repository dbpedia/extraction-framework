package org.dbpedia.extraction.dump

import org.dbpedia.extraction.destinations.{Dataset,DBpediaDatasets}
import org.dbpedia.extraction.util.Language

/**
 * Scala version of http://dbpedia.hg.sourceforge.net/hgweb/dbpedia/dbpedia/file/default/related_apps/downloadpagecreator/downloadpagecreator.php
 * 
 * Creates WackoWiki Syntax for DBpedia download page tables.
 * The BZ2 files should be stored in one Directory per Language.
 *  
 * CAUTION: This script does most probably not count the overall
 * amount of facts correctly!!!
 * Please use a shell script to count the total number of RDF triples.
 *
 * CAUTION: This script does not produce a split table like here
 * http://dbpedia.org/Downloads36
 * Please split it yourself or change the script!
 *
 * Note that there have to be multiple pages for the download page,
 * because the WackoWiki cannot cope with wiki pages being too long.
 */

class DatasetInfo(val dataset: String, val title: String, val description: String) {
  def this(dataset: Dataset, title: String, description: String) =
    this(dataset.name, title, description)
}

object CreateDownloadPage {
  
  val version = 3
  val subversion = 8

  val linkprefix = "http://downloads.dbpedia.org/"+version+"."+subversion+"/"
  
  val filetypes= Array("nt", "nq") // possible Filetypes for Download Table
    
  def main(args: Array[String]) {
    val localDir = args(0) // Path where all the DBpedia extraction files can be found
    
    // Use all remaining args as language codes or comma or whitespace separated lists of codes
    var langs : Seq[Language] = for(arg <- args.drop(1); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield Language(lang)
  }

//Files and corresponding Titles - first Header
val filesANDtitlesCORE = Array(
    
new DatasetInfo(DBpediaDatasets.OntologyTypes, "Ontology Infobox Types", "Contains triples of the form $object rdf:type $class from the ontology-based extraction."),
new DatasetInfo(DBpediaDatasets.OntologyProperties, "Ontology Infobox Properties", "High-quality data extracted from Infoboxes using the strict ontology-based extraction. The predicates in this dataset are in the /ontology/ namespace.\nNote that this data is of much higher quality than the Raw Infobox Properties in the /property/ namespace. For example, there are three different raw Wikipedia infobox properties for the birth date of a person. In the the /ontology/ namespace, they are all **mapped onto one relation** (http://dbpedia.org/ontology/birthDate). It is a strong point of DBpedia to unify these relations."),
new DatasetInfo(DBpediaDatasets.SpecificProperties, "Ontology Infobox Properties (Specific)", "Infoboxes Data from the loose ontology-based extraction."), // TODO: loose? bullshit...

new DatasetInfo(DBpediaDatasets.Labels, "Titles", "Titles of all Wikipedia Articles in the corresponding language"),

new DatasetInfo(DBpediaDatasets.ShortAbstracts, "Short Abstracts", "Short Abstracts (max. 500 chars long) of Wikipedia Articles"),
new DatasetInfo(DBpediaDatasets.LongAbstracts, "Extended Abstracts", "Additional, extended English abstracts."),

new DatasetInfo(DBpediaDatasets.Images, "Images", "Thumbnail Links from Wikipedia Articles" ),

new DatasetInfo(DBpediaDatasets.GeoCoordinates, "Geographic Coordinates", "Geographic coordinates extracted from Wikipedia."),

new DatasetInfo(DBpediaDatasets.InfoboxProperties, "Raw Infobox Properties", "Information that has been extracted from Wikipedia infoboxes. Note that this data is in the less clean /property/ namespace. The Ontology Infobox Properties (/ontology/ namespace) should always be preferred over this data."),
new DatasetInfo(DBpediaDatasets.InfoboxPropertyDefinitions, "Raw Infobox Property Definitions", "All properties / predicates used in infoboxes."),

new DatasetInfo(DBpediaDatasets.Homepages, "Homepages", "Links to external webpages."),
new DatasetInfo(DBpediaDatasets.Persondata, "Persondata", "Information about persons (date and place of birth etc.) extracted from the English and German Wikipedia, represented using the FOAF vocabulary."),
new DatasetInfo(DBpediaDatasets.Pnd, "PND", "Dataset containing PND (Personennamendatei) identifiers."),

new DatasetInfo(DBpediaDatasets.ArticleCategories, "Articles Categories", "Links from concepts to categories using the SKOS vocabulary."),
new DatasetInfo(DBpediaDatasets.CategoryLabels, "Categories (Labels)", "Labels for Categories."),
new DatasetInfo(DBpediaDatasets.SkosCategories, "Categories (Skos)", "Information which concept is a category and how categories are related using the SKOS Vocabulary."),

new DatasetInfo(DBpediaDatasets.ExternalLinks, "External Links", "Links to external web pages about a concept."),
new DatasetInfo(DBpediaDatasets.LinksToWikipediaArticle, "Links to Wikipedia Article", "Links to corresponding Articles in Wikipedia"),

new DatasetInfo(DBpediaDatasets.PageLinks, "Wikipedia Pagelinks", "Dataset containing internal links between DBpedia instances. The dataset was created from the internal pagelinks between Wikipedia articles. The dataset might be useful for structural analysis, data mining or for ranking DBpedia instances using Page Rank or similar algorithms."),
new DatasetInfo(DBpediaDatasets.Redirects, "Redirects", "Dataset containing redirects between Articles in Wikipedia"),
new DatasetInfo(DBpediaDatasets.DisambiguationLinks, "Disambiguation Links", "Extraction from Disambiguation Templates"),
new DatasetInfo(DBpediaDatasets.PageIds, "Page IDs", "Dataset containing the Wikipedia Page IDs."),
new DatasetInfo(DBpediaDatasets.Revisions, "Revision IDs", "Dataset containing the Wikipedia Revision IDs.")
)

//Files and corresponding Titles - second Header
val filesANDtitlesLINKS = Array(
new DatasetInfo("bookmashup_", "Links to RDF Bookmashup", "Links between books in DBpedia and data about them provided by the ((http://www4.wiwiss.fu-berlin.de/bizer/bookmashup/ RDF Book Mashup)). Provided by Georgi Kobilarov. Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("dailymed_", "Links to DailyMed", "Links between DBpedia and ((http://dailymed.nlm.nih.gov/ DailyMed)). Update mechanism: unclear/copy over from previous release."), 
new DatasetInfo("dblp_", "Links to DBLP", "Links between computer scientists in DBpedia and their publications in the ((http://www.informatik.uni-trier.de/~ley/db/ DBLP)) database. Links were created manually. Update mechanism: Copy over from previous release."),
new DatasetInfo("diseasome_", "Links to Diseasome", "Links between DBpedia and ((http://diseasome.eu/ Diseasome)). Update mechanism: unclear/copy over from previous release."), 
new DatasetInfo("drugbank_", "Links to DrugBank", "Links between DBpedia and ((http://www.drugbank.ca/ DrugBank)). Update mechanism: unclear/copy over from previous release."), 
new DatasetInfo("eurostat_", "Links to Eurostat", "Links between countries and regions in DBpedia and data about them from ((http://ec.europa.eu/eurostat Eurostat)). Links were created manually. Update mechanism: Copy over from previous release."),
new DatasetInfo("factbook_", "Links to CIA Factbook", "Links between countries in DBpedia and data about them from ((https://www.cia.gov/library/publications/the-world-factbook/ CIA Factbook)). Links were created manually. Update mechanism: Copy over from previous release."),
new DatasetInfo("flickr_", "Links to flickr wrappr", "Links between DBpedia concepts and photo collections depicting them generated by the ((http://www4.wiwiss.fu-berlin.de/flickrwrappr/ flikr wrappr)). Update mechanism: script in SVN."),
new DatasetInfo("freebase_", "Links to Freebase", "Links between DBpedia and ((http://www.freebase.com/ Freebase)) (MIDs). Update mechanism: script in SVN."), 
new DatasetInfo("geonames_", "Links to Geonames", "Links between geographic places in DBpedia and data about them in the ((http://www.geonames.org/ Geonames)) database. Provided by the Geonames people. Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("gutenberg_", "Links to Project Gutenberg", "Links between writers in DBpedia and data about them from ((http://www.gutenberg.org/ Project Gutenberg)). Update mechanism: script in SVN. Since this requires manual changes of files and a D2R installation, it will be copied over from the previous DBpedia version and updated between releases by the maintainers (Piet Hensel and Georgi Kobilarov)."),
new DatasetInfo("musicbrainz_", "Links to MusicBrainz", "Links between artists, albums and songs in DBpedia and data about them from ((http://musicbrainz.org/ MusicBrainz)). Created manually using the result of SPARQL queries. Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("nyt_", "Links to New York Times", "Links between New York Times subject headings and DBpedia concepts."),
new DatasetInfo("opencyc_", "Links to Cyc", "Links between DBpedia and ((http://opencyc.org/ Cyc)) concepts. ((OpenCyc Details)). Update mechanism: awk script."),
new DatasetInfo("revyu_", "Links to Revyu", "Links to Reviews about things in ((http://revyu.com/ Revyu)). Created manually by Tom Heath. Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("sider_", "Links to SIDER", "Links between DBpedia and ((http://sideeffects.embl.de/ SIDER)). Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("tcm_", "Links to TCMGeneDIT", "Links between DBpedia and ((http://tcm.lifescience.ntu.edu.tw/ TCMGeneDIT)). Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("uscensus_", "Links to US Census", "Links between US cities and states in DBpedia and data about them from US Census. Update mechanism: unclear/copy over from previous release."),
new DatasetInfo("wikicompany_", "Links to WikiCompany", "Links between companies in DBpedia and companies in ((http://wikicompany.org/ Wikicompany)). Update mechanism: script in SVN."),
new DatasetInfo("yago_", "Links to YAGO2", "Dataset containing links between DBpedia and YAGO, YAGO type information for DBpedia resources and the YAGO class hierarchy. Currently maintained by Johannes Hoffart."),  //This data set is created by running the DBpediaLink converter available at the  ((http://www.mpi-inf.mpg.de/yago-naga/yago/downloads.html YAGO website))
new DatasetInfo("wordnet_", "WordNet Classes", "Classification links to ((http://www.w3.org/TR/wordnet-rdf/ RDF representations)) of ((http://wordnet.princeton.edu/ WordNet)) classes. Update mechanism: unclear/copy over from previous release.")
)

}
/*
function createWikiTable($header,$filelist,$filespecs, $languages) {
	global $linkprefix;
    global $version;
    global $subversion;
    //Get the DBpedia Version Number for Preview File
	preg_match('~/([0-9]+\.[0-9]+)/~',$linkprefix,$matches);
	echo "===".$header."===\n";
    echo "**NOTE: You can find DBpedia dumps in 97 languages at our ((http://downloads.dbpedia.org/".$version.".".$subversion."/ DBpedia download server)).**\n\n";
	echo "//Click on the dataset names to obtain additional information.//\n";
	echo "#||\n||**Dataset**|**" . implode('**|**', $languages) . "**||\n";
        if ($header == "Core Datasets") {
            echo "||((#dbpediaontology DBpedia Ontology)) ++(<# <a href=\"http://downloads.dbpedia.org/preview.php?file=".$version.".".$subversion."_sl_dbpedia_".$version.".".$subversion.".owl.bz2\">preview</a> #>)++|++<# <a href=\"http://downloads.dbpedia.org/".$version.".".$subversion."/dbpedia_".$version.".".$subversion.".owl.bz2\" title=\"Triples: unknown; Filesize(download): unknown; Filesize(unpacked): unknown\">owl</a> #>++|++--++|++--++|++--++|++--++|++--++|++--++|++--++|++--++|++--++|++--++|++--++||";
        }
        foreach ($filelist as $name) {
		foreach ($languages as $index => $lang) {
			if ($index === 0) {
				echo '||((#'.str_replace(" ","",strtolower($name['title'])).' '.$name['title'].')) ++(<# <a href="http://downloads.dbpedia.org/preview.php?file='.$matches[1].'_sl_'.$lang.'_sl_'.$name['file'].$lang.'.nt.bz2">preview</a> #>)++|';
			}
			
			echo '++'.lookup($name['file'],$lang,$filespecs).'++|';
		}
		echo "|\n";
	}
	echo "||# \n";
}

function lookup($file,$lang,$filespecs) {
	global $localpackdirectory;
	global $linkprefix;
	global $filetypes;
		
	foreach ($filetypes as $filetype) {
		if (is_file($localpackdirectory.$lang.'/'.$file.$lang.'.'.$filetype.'.bz2')) {
			$return.='<# <a href="'.$linkprefix.$lang.'/'.$file.$lang.'.'.$filetype.'.bz2" title="Triples: '.$filespecs[$file.$lang.'.nt']['lines'].'; Filesize(download): '.$filespecs[$file.$lang.'.'.$filetype]['bzip2'].'; Filesize(unpacked): '.$filespecs[$file.$lang.'.'.$filetype]['filesize'].'">'.$filetype.'</a> #>';
		}
	else
		$return.='-';
	}
	return $return;
}
function createDescriptions($filelist) {
	foreach ($filelist as $file) {
		echo "{{a name=\"".str_replace(" ","",strtolower($file['title']))."\"}}\n";
		echo "==== ".$file['title']." ====\n";
		echo "//".$file['description']."//\n";
	}
}

//prepare Data for Files
function getFileSpecifications($directories, $filelist, $filetypes) {
	global $localdirectory, $localpackdirectory;
		
	foreach ($directories as $dir) {
		foreach ($filelist as $file) {
			foreach ($filetypes as $filetype) {
				$name = $file['file'].$dir.'.'.$filetype;
				$plain = $localdirectory.$dir.'/'.$name;
				$pack = $localpackdirectory.$dir.'/'.$name.'.bz2';
                                if (is_file($plain) && is_file($pack))
				{
					//error_log('getting specs for ' . $name . ' and ' . $name . '.bz2');
					$resultarray[$name]=array('lines'=> getFileLines($plain), 'filesize' => getFilesize($plain), 'bzip2' => getFilesize($pack));
				}
				else
				{
					//error_log('WARNING: cannot get specs for ' . $plain . ' and ' . $pack);
				}
			}
		}
	}
	return $resultarray;
}
function getFilesize($filelocation) {
	if (is_file($filelocation)) {
		//$filesizecmd='du -b '.$filelocation;
		$filesizecmd='for %I in ("'.$filelocation.'") do @echo %~zI';

                exec($filesizecmd,$returnfilesize);
		preg_match('/^[0-9]* /',$returnfilesize[0],$matchesB);
		if ($matchesB[0] > 1024)
			if (($matchesB[0]/1024) > 1024)
				if ($matchesB[0]/(1024*1024) > 1024)
					$matchesB[0]=round($matchesB[0]/(1024*1024*1024),1).'GB';
				else
					$matchesB[0]=round($matchesB[0]/(1024*1024),1).'MB';
			else
				$matchesB[0]=round($matchesB[0]/1024,1).'KB';
		else
			$matchesB[0]=$matchesB[0].'Bytes';
		$returnfilesize=array();
		return $matchesB[0];
	}
	else
		return null;
}
function getFileLines($filelocation) {
	if (is_file($filelocation)) {
		//$filelinescmd='wc -l '.$filelocation;
                $filelinescmd='find /c " " '.$filelocation;
		exec($filelinescmd,$returnfilelines);	
		preg_match('/[0-9]*$/',$returnfilelines[1],$matchesA);
		if ($matchesA[0] > 1000)
			if (($matchesA[0]/1000) > 1000)
				$matchesA[0]=round($matchesA[0]/1000000,1).'M';
			else
				$matchesA[0]=round($matchesA[0]/1000,1).'K';
		$returnfilelines=array();	
		return $matchesA[0];
	}
	else 
		return null;
}

function countTriples($specs) {
	$count=0;
	$countpagelink=0;
	#$abk=array("K","M");
	#$cou=array("000","000000");
	foreach (array_keys($specs) as $file) {
		if (preg_match('/.\.csv/',$file))
			continue;
		else {
			if (preg_match('/K/',$specs[$file]['lines'])) {
				if (preg_match('/page_link/',$file))
					$countpagelink=$countpagelink+(str_replace("K","",$specs[$file]['lines'])*1000);
				else
					$count=$count+(str_replace("K","",$specs[$file]['lines'])*1000);
			}
			if (preg_match('/M/',$specs[$file]['lines'])) {
				if (preg_match('/page_link/',$file))
					$countpagelink=$countpagelink+(str_replace("M","",$specs[$file]['lines'])*1000000);
				else
					$count=$count+(str_replace("M","",$specs[$file]['lines'])*1000000);
			}
		}
			#$count=$count+str_replace($abk,$cou,$specs[$file]['lines']);
	}
	$countfull=array(0 => $count, 1 => $countpagelink);
	return $countfull;		
}
/*****
* 0. Prepare Directories (create all directories for languages)
* 1. read all files to get the File Specifications => stored as php file
* 2.  pack all files
* 3. create Wikitables with the File Specifications
* 4. split the resulting wikicode: Head and Core Datasets Table => Downloads*VER* ; Link Datasets Table => Downloads*VER*1 ; Descriptions => Downloads*VER*2  due to a WackoWiki Bug with all Tables in one Page
*******/
$filesANDtitlesCORE_SPECS=getFileSpecifications($languages, $filesANDtitlesCORE, $filetypes);
#include "filespecsCORE.out.php";
file_put_contents('filespecsCORE.out.php',"<?\n\$filesANDtitlesCORE_SPECS=".var_export($filesANDtitlesCORE_SPECS,true)."\n?>");

$filesANDtitlesLINKS_SPECS=getFileSpecifications(array('links'), $filesANDtitlesLINKS, array('nt'));
#include "filespecsLINKS.out.php";
file_put_contents('filespecsLINKS.out.php',"<?\n\$filesANDtitlesLINKS_SPECS=".var_export($filesANDtitlesLINKS_SPECS,true)."\n?>"); 




$counterA=countTriples($filesANDtitlesCORE_SPECS);
$counterB=countTriples($filesANDtitlesLINKS_SPECS);
$fullcount=($counterA[0]/2)+($counterA[1]/2)+$counterB[0]+$counterB[1];  // /2 because NQs are also counted
$pagelinkcount=$counterA[1]+$counterB[1];
$normalcount=$counterA[0]+$counterB[0];

echo "/***************************************************************/\n";
echo "/*Triple Count for this Extraction: ".$fullcount."*with*".$normalcount." \"Normal\"-Triples and ".$pagelinkcount." Pagelink-Triples*/\n";
echo "/***************************************************************/\n";

echo "/***************************************************************/\n";
echo "/*****PUT THIS ON Downloads - Main Page*************************/\n";
echo "/***************************************************************/\n";
echo "==DBpedia ".$version.".".$subversion." Downloads==\n
This pages provides downloads of the DBpedia datasets. The DBpedia datasets are licensed under the terms of the ((http://en.wikipedia.org/wiki/Wikipedia:Text_of_Creative_Commons_Attribution-ShareAlike_3.0_Unported_License Creative Commons Attribution-ShareAlike License)) and the ((http://en.wikipedia.org/wiki/Wikipedia:Text_of_the_GNU_Free_Documentation_License GNU Free Documentation License)). http://m.okfn.org/images/ok_buttons/od_80x15_red_green.png The downloads are provided as N-Triples and N-Quads, where the N-Quads version contains additional provenance information for each statement. All files are bz2 packed.\n

Older Versions: ((Downloads35 DBpedia 3.5.1)), ((Downloads35 DBpedia 3.5)), ((Downloads34 DBpedia 3.4)), ((Downloads33 DBpedia 3.3)), ((Downloads32 DBpedia 3.2)), ((Downloads31 DBpedia 3.1)), ((Downloads30 DBpedia 3.0)), ((Downloads30RC DBpedia 3.0RC)), ((Downloads20 DBpedia 2.0))\n

See also the ((ChangeLog change log)) for recent changes and developments.

{{ToC numerate=1 from=h2 to=h2}}\n

=== Wikipedia Input Files ===\n

The datasets were extracted from ((http://download.wikipedia.org/ Wikipedia dumps)) generated in October / November 2010. Specific dates and times:
#||
|| |**en**|**de**|**fr**|**pl**|**it**|**ja**|**es**|**nl**|**hu**|**sl**|**hr**|**el**||
|| Dump end | ++2010-10-11++ | ++2010-10-13++ | ++2010-10-17++ | ++2010-11-01++ | ++2010-10-20++ | ++2010-11-02++ | ++2010-10-23++ | ++2010-11-01++ | ++2010-10-27++ | ++2010-11-01++ | ++2010-10-30++ | ++2010-10-29++ ||
||#\n";

createWikiTable("Core Datasets",$filesANDtitlesCORE,$filesANDtitlesCORE_SPECS, $languages);

echo "{{include page=\"/Downloads".$version.$subversion."a\" nomark=\"1\"}}\n";
echo "{{include page=\"/Downloads".$version.$subversion."b\" nomark=\"1\"}}\n\n";

echo "[Note for Wiki Editors: The wiki code for this page is generated automatically. Please modify the files in <# <a href=\"http://dbpedia.svn.sourceforge.net/viewvc/dbpedia/related_apps/downloadpagecreator/\">http://dbpedia.svn.sourceforge.net/viewvc/dbpedia/related_apps/downloadpagecreator/</a>#> to make permanent changes.]\n\n\n";

echo "/***************************************************************/\n";
echo "/*****PUT THIS TABLE ON Downloads1 Page*************************/\n";
echo "/***************************************************************/\n";

createWikiTable("Extended Datasets",$filesANDtitlesLINKS,$filesANDtitlesLINKS_SPECS, array('links'));
echo "\n\n";

echo "/***************************************************************/\n";
echo "/*****PUT THIS TABLE ON Downloads2 Page*************************/\n";
echo "/***************************************************************/\n";
echo "=== Dataset Descriptions ===\n\n";
echo "{{a name=\"dbpediaontology\"}}\n";
echo "==== DBpedia Ontology ====\n";
echo "//The DBpedia ontology in OWL. See ((http://jens-lehmann.org/files/2009_dbpedia_jws.pdf our JWS paper)) for more details.//\n";

createDescriptions($filesANDtitlesCORE);
createDescriptions($filesANDtitlesLINKS);
*/