#!/usr/bin/env bash

#TODO compare with mappingbased, wikidata
set -e

EXTRACTIONFRAMEWORK="/home/extractor/extraction-framework"

DATABUSMAVENPLUGIN="/data/extraction/databus-maven-plugin/dbpedia/generic"

LOGS=

downloadOntology() {
	echo " (date) | extraction-framework | start download ontology" >&2;
	cd $EXTRACTIONFRAMEWORK/core;
	../run download-ontology;
}

downloadMappings() {
	echo "$(date) | extraction-framework | start download mappings" >&2;
	cd $EXTRACTIONFRAMEWORK/core;
	../run download-mappings;
}

downloadDumps() {
	echo "$(date) | extraction-framework | start download dumps" >&2;
	cd $EXTRACTIONFRAMEWORK/dump;
	../run download download.spark.properties;
}

buildExtractionFramework() {
	echo "$(date) | extraction-framework | mvn clean install" >&2;
	cd $EXTRACTIONFRAMEWORK;
	mvn clean install;
}

runExtraction() {
	echo "$(date) | extraction-framework | start extraction" >&2;
	cd $EXTRACTIONFRAMEWORK/dump;
	../run sparkextraction extraction.spark.properties;
}

prepareRelease() {
	echo "$(date) | databus-maven-plugin | collect extracted datasetes" >&2;
	cd $DATABUSMAVENPLUGIN;
	./collectExtraction.sh;
}

deployRelease() {
	echo "$(date) | databus-maven-plugin | mvn package" >&2;
	cd $DATABUSMAVENPLUGIN;
	mvn package;
	echo "$(date) | databus-maven-plugin | mvn databus:deploy" >&2;
	mvn databus:deploy;
}

main() {

	echo "--------------------"
	echo " Generic Extraction "
	echo "--------------------"

	downloadOntology;
	downloadMappings;
	downloadDumps;

	buildExtractionFramework;
	runExtraction;

	prepareRelease;
	deployRelease;
}
