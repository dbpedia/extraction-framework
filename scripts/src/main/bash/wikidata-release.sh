#!/usr/bin/env bash

#TODO compare with mappingbased, generic
set -e

TIME_DATE="2019-07-01"  #$(date +%Y-%m-%d)

EXTRACT_DIR=/home/extractor/extraction-framework
MVN_LOGS=/data/extraction/logs/mvn
#DATA_DIR=`awk -F= '/base-dir/{print $NF}' $EXTRACT_DIR/core/src/main/resources/universal.properties | head -n1`
DATA_DIR=/data/extraction/wikidumps/
#WWW_DIR=/var/www/html/wikidata

function download-ontology(){
 cd $EXTRACT_DIR/core;
 ../run download-ontology;
}

function recompile(){
 cd $EXTRACT_DIR;
 mvn clean install;
}

function download-r2r-mapping(){
 cd $EXTRACT_DIR/core/src/main/resources && curl https://raw.githubusercontent.com/dbpedia/extraction-framework/master/core/src/main/resources/wikidatar2r.json > wikidatar2r.json
}

function download-xml-dump(){
 cd $EXTRACT_DIR/dump;
 ../run download download.wikidata.properties \
 > $MVN_LOGS/$TIME_DATE-wikidata.download.out \
 2> $MVN_LOGS/$TIME_DATE-wikidata.download.err;
}

function raw-extractor(){
 cd $EXTRACT_DIR/dump;
 #Run only .WikidataRawExtractor
 ../run extraction extraction.wikidataraw.properties;
}

function subclassof-script(){
 cd $EXTRACT_DIR/scripts;
 ../run WikidataSubClassOf process.wikidata.subclassof.properties;
}

function all-other-extractors(){
 cd $EXTRACT_DIR/dump;
 # Run all other extractors
 ../run extraction extraction.wikidataexceptraw.properties
}

function all-extractors(){
 cd $EXTRACT_DIR/dump;
 # Run all extractors to run extraction
 ../run extraction extraction.wikidata.properties;
# > $MVN_LOGS/$TIME_DATE-wikidata.extraction.out \
# 2> $MVN_LOGS/$TIME_DATE-wikidata.extraction.err;

}

function post-processing(){
 cd $EXTRACT_DIR/scripts;
 ../run ResolveTransitiveLinks $DATA_DIR redirects transitive-redirects .ttl.bz2 wikidata
 ../run MapObjectUris $DATA_DIR transitive-redirects .ttl.bz2 mappingbased-objects-uncleaned,raw -redirected .ttl.bz2 wikidata
}

function type-consistency-check(){
 cd $EXTRACT_DIR/scripts;
 ../run TypeConsistencyCheck type.consistency.check.properties;
}

function sync-with-www(){
 rsync -avz $DATA_DIR/wikidatawiki/ $WWW_DIR/;

 #We don't need index.html
 find $WWW_DIR/ | grep index.html | xargs rm -rf;
}

function databus-preparation(){
  cd $DATA_DIR;
  bash ~/databusPrep.sh $WWW_DIR/ src/main/databus;
}

function delete-old-extractions(){
 #Delete extractions older than 1 month, i.e. keep 1-2 results in www.
 find $WWW_DIR/ -type d -ctime +20 | xargs rm -rf;

 #Remove everything in Dump dir, do we need to keep them?
 rm -rf $DATA_DIR/wikidatawiki/*;
}

function remove-date-from-files(){
 #Go to the last changed directory
 cd "$(\ls -1dt $WWW_DIR/*/ | head -n 1)";

 #Remove date (numbers) from files
 for i in *; do  mv "$i" "`echo $i| sed 's/[0-9]..//g'`"; done;
}

function main() {
 #delete-old-extractions; #to have some space for new extraction

# touch download.process;

 download-ontology;
 download-r2r-mapping;
 download-xml-dump;
 recompile;
 all-extractors;

 post-processing;
 type-consistency-check;

 cd /data/extraction/wikidumps;
 ./prep.sh;

 cd /data/extraction/databus-maven-plugin/dbpedia/wikidata;
 mvn package;
 mvn databus:deploy;

#----
# below not configured yet
#----

 ##Result of subclassof-script is used in next extraction.
 #subclassof-script;
 #databus-preparation;
 #Sync extraction with www
 #sync-with-www
 #remove-date-from-files

#This was the previous extraction process. Now we don't need to run rawextractor separately
# raw-extractor;
# subclassof-script;
# all-other-extractors;
# post-processing;
}

main
