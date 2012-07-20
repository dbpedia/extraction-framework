#!/bin/sh
mvn package
mkdir ~/dbpedia-wiktionary/
cp target/wiktionary-2.0-SNAPSHOT-jar-with-dependencies.jar ~/dbpedia-wiktionary/
mkdir ~/dbpedia-wiktionary/wiktionaryDump/
cp wiktionaryDump/test-pages-articles.xml ~/dbpedia-wiktionary/wiktionaryDump//
cp config*.xml ~/dbpedia-wiktionary/
cp config.properties ~/dbpedia-wiktionary/
cd ~
rm dbpedia-wiktionary.zip
zip -r dbpedia-wiktionary dbpedia-wiktionary/*
rm -rf ~/dbpedia-wiktionary/
