# Extraction Process Documentation
## 1. Downloads
This first section contains the commands used to download all necessary data to run the different extractions.
### 1.1. The XML-Dumps
The Wikipedia-XML-Dumps are the main source of the DBpedia Extraction. They contain all the wikipedia articles in the XML format and are found here: https://dumps.wikimedia.org/.
The DBpedia Extraction-Framework has a function that helps downloading all dumps that are needed. It can be configured in the `$extraction-framework/dump/download.10000.properties` file. To run the dump-download run the following commands:
- `cd $extraction-framework/dump`
- `../run download download.10000.properties`
### 1.2. The Ontology Files
In addition to the XML-Dumps the extraction-framework needs the ontology files to run. They are downloaded using the following command.
- `cd $extraction-framework/dump`
- `..run download-ontology`
### 1.3. The wikidata-r2r Mappings
Used by the wikidata-extraction, this file needs to be up-to-date, which can be achieved using the following commands: 
- `cd $EXTRACT_DIR/core/src/main/resources && curl https://raw.githubusercontent.com/dbpedia/extraction-framework/master/core/src/main/resources/wikidatar2r.json > wikidatar2r.json`
If the extraction-framework is already up-to-date, then this step can be skipped.
## 2. Generic-Spark-Extraction
The generic spark extraction is using Apache Spark to speed up the production of the basic datasets. This works with every extractor except the MappingExtractor, the ImageExtractor and the NifExtractor. The source code for this extraction can be found here: https://github.com/Termilion/extraction-framework.
- `cd $extraction-framework/dump`
- edit: `$extraction-framework/dump/extraction.spark.properties`
- `../run sparkextraction extraction.spark.properties`

## 3. Mappings-Extraction 
The Mappings-Extraction produces better data than the generic-spark extraction using community-made mapping-files. Due to the complexity of this task, the mapping-extraction is currently run with the non-Apache Spark version of the extraction-framework:
- `cd $extraction-framework/dump`
- edit: `$extraction-framework/dump/extraction.mapping.properties`
- `../run extraction extraction.mapping.properties`

## 4. Wikidata-Extraction
### 4.1 Extract the Data
 - `cd $extraction-framework/dump`
 - `../run extraction extraction.wikidata.properties`
### 4.2 Post-Processing
- `cd $extraction-framework/scripts`
- `../run ResolveTransitiveLinks $BASE_DIR redirects transitive-redirects .ttl.bz2 wikidata`
- `../run MapObjectUris $BASE_DIR transitive-redirects .ttl.bz2 mappingbased-objects-uncleaned,raw -redirected .ttl.bz2 wikidata`
- `../run WikidataSubClassOf process.wikidata.subclassof.properties`
- `../run TypeConsistencyCheck type.consistency.check.properties`

## 5. Preparation for Databus
The extraction-framework output and the databus-maven-plugin input have different formats, to transfer the extracted data to the new format, just run this in the base-directory of your extracted data.
- `cd $BASE_DIR`
- `$extraction-framework/scripts/src/main/bash/databusPreparation.sh $RELEASE_DIR src/main/databus/input`

## 6. Run the Databus-Maven-Plugin
