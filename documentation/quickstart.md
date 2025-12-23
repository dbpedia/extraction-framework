# The DBpedia Extraction Framework QuickStart Guide

TODO: requires further extension with info from: https://github.com/dbpedia/extraction-framework/blob/master/documentation/extraction-process.md

## Requirements:

* **Maven 3.2+** - used for project management and build automation. Get it from: http://maven.apache.org/.
* **Java 1.8.x JDK** - use JDK version 1.8 ( Java 8)  The framework does not compile with Java 10 & 11
* **rapper** version 2.14+ - get it from http://librdf.org/raptor/rapper.html
* **Git** 
* Your favourite IDE for coding. There are a number of options:
  * **IntelliJ IDEA** - currently the most stable IDE for developing with Java/Scala (preferred). To get the most recent Scala Plugin get the current early access version and install the Scala plugin from the official repository.
    * Please follow the [DBpedia & IntelliJ Quick Start Guide](Setting-up-IntelliJ-IDEA)
  * **Eclipse** - please follow the [DBpedia & Eclipse Quick Start Guide](Setting up Eclipse).
  * **Netbeans** - also offers a Scala plugin.

## How to install Java 1.8 
You can install Java 1.8 with sdkman. In command line write next commands:

`curl -s "https://get.sdkman.io" | bash`

open a new shell and check for available versions

`sdk list java | grep open | grep 8.`

pick one release and install with

`sdk install java 8.y.z-open`

For example

`sdk install java 8.0.282-open`

check whether correct java version is set to default
with

`mvn -v`

It should look similar to this
```
mvn -v
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_282, vendor: Oracle Corporation, runtime: /home/dbpedia/.sdkman/candidates/java/8.0.282-open/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "4.19.0-14-amd64", arch: "amd64", family: "unix"
```
## 1. Setting Up

* Clone the latest version of the framework from the `master` branch.

`git clone https://github.com/dbpedia/extraction-framework`

* Configure the framework

TODO: missing docu with in-depth description of the configuration options, being prepared at https://github.com/dbpedia/extraction-framework/tree/configDocu

`core/src/main/resources/universal.properties`:
  - `base-dir`: Path to `DATADIR` -> the data directory
  - `log-dir`: Path to `LOGDIR` -> the log directory

* Configure your execution properties in `dump/*.properties`
    - already prepared configurations:
        - `extraction.default.properties`: the default extraction
        - `extraction.mappings.properties`: only extracts mapping datasets
        - `extraction.spark.properties`: configuration options for the spark-extraction. Executes all spark-supported extractors. (no mapping, no NIF, no images)
        - `extraction.wikidata.properties`: extraction on wikidata datasets
        - `download.10000.properties`: pre-configured to download all dumps with more than 10.000 articles
    - configuration options include:
        - `require-download-complete`: checks directories for the download-complete file
        - `languages`: list of languages, used for the extraction
        - `extractors`: list of extractor classes to be used in the extraction
        - `extractors.language-code`: additional extractor classes used for this language
        -  `spark-master` (*sparkextraction only*): value for the spark-master. For local mode, you can configure your number of processor-threads to be used with `local[X]`. Optimum seems to be reached at 32 Threads with the current implementation.
        -  `spark-local-dir` (*sparkextraction only*): location for sparks temporary files, incase `/tmp/` is too small.

* Edit `dump/pom.xml` and change memory settings for extraction launcher `<jvmArg>-Xmx16G</jvmArg>`, if needed.

* Compile the framework

`mvn install`

## 2. Download Input Data

There are several input datasets required for the framework.

* **Wikipedia XML Dumps:**

The Wikimedia XML dumps are the main source for the extraction.
The data is distributed as one file per language per dump-release.
The extraction framework downloads these files the download script (see below).
You can also manually download the files from [here](https://dumps.wikimedia.org/backup-index-bydb.html), but this very tedious task.

```
cd dump
../run download download.10000.properties
```

Note: check if everything has been downloaded. If not, run the download command again!

* **DBpedia Ontology**

Download the latest version of the DBpedia ontology using the script.

```
cd core
../run download-ontology
```

* **DBpedia Mappings**

Download the latest versions of the mappings from the [mappings wiki](http://mappings.dbpedia.org).

```
cd core
../run download-mappings
```

* **Wikidata-r2r Mappings**

This is only needed for the Wikidata-Extraction and downloads the latest version of the additional r2r mappings from the official repository.

```
cd core/src/main/resources && curl https://raw.githubusercontent.com/dbpedia/extraction-framework/master/core/src/main/resources/wikidatar2r.json > wikidatar2r.json
cd ../../../../ && mvn clean install # again to ensure new wikidatar2r.json is loaded correctly
``` 

## 3A Running per-entity ad hoc extraction / Deploying Ad hoc Extraction Server
run `redeploy-server` script 
go to http://localhost:9999/server/extraction/
you can also use this script as cronjob for one instance per machine / docker container to redeploy the ad hoc extraction daily using latest master source code + ontology + mappings

```
0 3 * * * /bin/bash -c "cd /home/gfs/extraction-framework && ./redeploy-server"
```

## 3B Running the Dump Extraction

* **Default Extraction**

This is the default way of starting the extraction.
The extractor classes that get used, and with that the datasets that get produced, can be configured in the `.properties` file used.
```
cd dump`
../run extraction extraction.default.properties
```

* **Mappings Based Extraction**

The mapping-based extraction produces higher quality data through the usage of community-made mapping files.
Due to the complexity of this task, the mapping-extraction is currently not supported by the spark-based extraction.

```
cd dump
../run extraction extraction.mappings.properties
```

TODO: missing info on generic extraction

* **Wikidata based Extraction**

```
cd dump
../run extraction extraction.wikidata.properties
```

* **Apache Spark Based Extraction**

The Spark based extraction is using Apache Spark to speed up the extraction process.
It currently works with every `Extractor` except `MappingExtractor`, `ImageExtractor` and the `Text` extraction.

```
cd dump
../run sparkextraction extraction.spark.properties
```

TODO: extend with info from https://github.com/dbpedia/extraction-framework/blob/master/documentation/extraction-process.md#2-generic-spark-extraction

## 4. Post-Processing

* **Wikidata Post-processing**

TODO: extend with https://github.com/dbpedia/extraction-framework/blob/master/documentation/extraction-process.md#4-wikidata-extraction

The wikidata files have their own dedicated postprocessing scripts, that should be executed before publishing the data.
```cd scripts
../run ResolveTransitiveLinks DATADIR redirects transitive-redirects .ttl.bz2 wikidata
../run MapObjectUris DATADIR transitive-redirects .ttl.bz2 mappingbased-objects-uncleaned,raw -redirected .ttl.bz2 wikidata
../run WikidataSubClassOf process.wikidata.subclassof.properties
../run TypeConsistencyCheck type.consistency.check.properties
```
## 5. Release Deployment on the Databus (ONLY for official releases, not for local)

* **Preparation for export to the downloads server & release deployment on the [DBpedia Databus](https://databus.dbpedia.org)**

This script moves the data to the `EXPORTDIR` and renames the files according to the naming scheme used by the [databus-maven-plugin](https://github.com/dbpedia/databus-maven-plugin). Replace `DUMPDATE` with the current date in the format `dd.mm.yyyy`.

```
mkdir EXPORTDIR
cd DATADIR`
extraction-framework/scripts/src/main/bash/databusPreparation.sh EXPORTDIR src/main/databus/DUMPDATE
```

* **Data cleansing (sort -u & rapper)**

Sort, remove duplicates and parse the data with the rapper tool.

```
cd DATADIR
extraction-framework/scripts/src/main/bash/sortandrap.sh
```
