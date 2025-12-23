#!/usr/bin/env bash

#TODO compare with generic, wikidata
set -e

rootDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
mvnLogs=/data/extraction/logs/mvn
currentDate=$(date +%Y-%m-%d)
extraFrameW=/home/extractor/extraction-framework/
dataPlugDir=/data/extraction/databus-maven-plugin/dbpedia/mappings/

function download-xml(){
  cd $extraFrameW/dump;
  ../run download download.mappings.properties \
  > $mvnLogs/$currentDate-mappingbased.download.out \
  2> $mvnLogs/$currentDate-mappingbased.download.err;
}

function download-ontology(){
  cd $extraFrameW/dump;
  ../run download-ontology;
}

function download-mappings(){
  cd $extraFrameW/dump;
  ../run download-mappings;
}

function extraction(){
  cd $extraFrameW/dump;
  ../run extraction extraction.mappings.properties \
  > $mvnLogs/$currentDate-mappingbased.extraction.out \
  2> $mvnLogs/$currentDate-mappingbased.extraction.err;
}

function setNewestVersion(){
  cd $dataPlugDir
  mvn versions:set -DnewVersion=$(ls * | grep '^[0-9]\{4\}.[0-9]\{2\}.[0-9]\{2\}$' | sort -u  | tail -1)
}

function package(){
  cd $dataPlugDir
  mvn package
}

function deploy(){
  cd $dataPlugDir
  mvn deploy
}


function main(){
  download-xml;
  download-ontology;
  download-mappings;

  extraction;

  # sep. conf.
  $rootDir/extractionToPlugin.sh;

  setNewestVersion;
  package;
  deploy;
}

main
