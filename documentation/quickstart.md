# DBpedia Extraction-Framework QuickStart:

## Requirements:

1. Maven **3.2+**
2. Java **1.8+** ( Did not compile with OpenJdk-10 & 11, just use OpenJdk-8 )
3. rapper version **2.14**
4. Git
5. A Text Editor (e.g vim/nano)
6. A data directory, further called `DATADIR`, a log directory, now further called `LOGDIR` and an export directory, called `ÈXPORTDIR`.

## 1. Setting everything up:

In this section the extraction-framework gets downloaded from the official repository, configured to work on your own system and installed with maven.

1. `git clone https://github.com/dbpedia/extraction-framework`
3. Configure `core/src/main/resources/universal.properties`:
    - `base-dir`: Path to `DATADIR`
    - `log-dir`: Path to `LOGDIR`
4. Configure your execution properties in `dump/*.properties`
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
5. Edit dump/pom.xml and change memory settings for extraction launcher `<jvmArg>-Xmx16G</jvmArg>` if needed.
6. `mvn install`

## 2. Resource-Downloads

This section describes the gathering of the different input-files for the extraction-framework.

1. **The XML-Dumps:**

The Wikimedia-XML-Dumps are the main source for the extraction. The data is distributed in one file per language per dump-release. The extraction-framework can download these itself with the download script. You also can manually download the files from [here](https://dumps.wikimedia.org/backup-index-bydb.html).

```
cd dump
../run download download.10000.properties
```

(Check if everything is downloaded, if not: start again!)

2. **The Ontology Files:**

This script downloads the latest version of the ontology file used to build valid rdf data.

```
cd core
../run download-ontology
```

3. **The Mapping Files:**

This script downloads the latest versions of the mappings from the mappings-wiki.

```
cd core
../run download-mappings
```

4. **The wikidata-r2r Mappings**

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
1. **Default-Extraction:**

This is the default way of starting the extraction. The extractor classes that get used, and with that the datasets that get produced, can be configured in the `.properties` file used.
```
cd dump`
../run extraction extraction.default.properties
```

2. **Spark-Based Extraction:**

The spark-based extraction is using Apache Spark to speed up the extraction of basic datasets. This means it works with every `Extractor` except `MappingExtractor`, `ImageExtractor` and the NIF-Extraction.

```
cd dump
../run sparkextraction extraction.spark.properties
```

3. **Mappings-based Extraction:**

The mapping-based extraction produces higher quality data through the usage of community-made mapping files. Due to the complexity of this task, the mapping-extraction is currently not supported by the spark-based extraction.

```
cd dump
../run extraction extraction.mappings.properties
```

4. **Wikidata-based Extraction:**

```
cd dump
../run extraction extraction.wikidata.properties
```
    
## 4. Postprocessing

1. **Wikidata Postprocessing:**

    The wikidata files have their own dedicated postprocessing scripts, that should be executed before publishing the data.
```cd scripts
../run ResolveTransitiveLinks DATADIR redirects transitive-redirects .ttl.bz2 wikidata
../run MapObjectUris DATADIR transitive-redirects .ttl.bz2 mappingbased-objects-uncleaned,raw -redirected .ttl.bz2 wikidata
../run WikidataSubClassOf process.wikidata.subclassof.properties
../run TypeConsistencyCheck type.consistency.check.properties
```

2. **Preparation for export to the Download-Server & Databus:**

This script moves the data to the `ÈXPORTDIR` and renames the files according to the naming scheme used by the [databus-maven-plugin](https://github.com/dbpedia/databus-maven-plugin). Replace `DUMPDATE` with the current date in the format `dd.mm.yyyy`.

```
mkdir EXPORTDIR
cd DATADIR`
extraction-framework/scripts/src/main/bash/databusPreparation.sh EXPORTDIR src/main/databus/DUMPDATE
```

3. **Datacleaning (sort -u & rapper):**

This script sorts, deduplicates and parses your data with the rapper tool.

```
cd DATADIR
extraction-framework/scripts/src/main/bash/sortandrap.sh
```

