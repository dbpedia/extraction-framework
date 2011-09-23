#!/bin/bash

java -Xmx2048m -cp target/live-2.0-SNAPSHOT-jar-with-dependencies.jar org.dbpedia.extraction.live.feeder.OntologyUpdateFeeder
