# DBPEDIA HISTORY

DBpedia History enables the history of a Wikipedia chapter to be extracted into an RDF format 


## Previous work

This DBpedia App is a Scala/Java version of the first work conducted by the French Chapter, <https://github.com/dbpedia/Historic/>.

Fabien Gandon, Raphael Boyer, Olivier Corby, Alexandre Monnin. Wikipedia editing history in DBpedia: extracting and publishing the encyclopedia editing activity as linked data. IEEE/WIC/ACM International Joint Conference on Web Intelligence (WI' 16), Oct 2016, Omaha, United States. <hal-01359575>
https://hal.inria.fr/hal-01359575

Fabien Gandon, Raphael Boyer, Olivier Corby, Alexandre Monnin. Materializing the editing history of Wikipedia as linked data in DBpedia. ISWC 2016 - 15th International Semantic Web Conference, Oct 2016, Kobe, Japan. <http://iswc2016.semanticweb.org/>. <hal-01359583>
https://hal.inria.fr/hal-01359583

## A first working prototype

This prototype is not optimized. During its development, we were faced with the WikiPage type-checking constraints that are checked in almost every module of the DBpedia pipeline.
We basically copy/pasted and renamed all the classes and objects we needed for running the extractors.
This conception could be easily improved by making `WikiPage` and `WikiPageWithRevision` objects inherit from the same abstract object.
But as a first step, we wanted to touch the less possible DBpedia core module.

Some other improvements that could be made:
* Scala version
* Enabling use of a historic namespace, taking into account the DBpedia chapter language
* Enabling following when a revision impacts content of an `infobox`

## Main Class

* [WikipediaDumpParserHistory.java](src/main/java/org/dbpedia/extraction/sources/WikipediaDumpParserHistory.java) — for parsing the history dumps
* [RevisionNode.scala](src/main/scala/org/dbpedia/extraction/wikiparser/RevisionNode.scala) — define revision of node object
* [WikiPageWithRevision](src/main/scala/org/dbpedia/extraction/wikiparser/WikiPageWithRevisions.scala) — define `wikipage` with revision list object 

## Extractors 

* [HistoryPageExtractor.scala](src/main/scala/org/dbpedia/extraction/mappings/HistoryPageExtractor.scala) — Extract all revisions of every Wikipedia page
* [HistoryStatsExtractor.scala](src/main/scala/org/dbpedia/extraction/mappings/HistoryStatsExtractor.scala) — Extract statistics about revision activity for every page of Wikipedia

## How to run it ? 

### Download 

* configure the [download.properties](download.properties) file 
* and run  ```../run download download.properties```

### Extraction

* configure the [extraction.properties](extraction.properties) file
* and run  ```../run run extraction.properties```

* Test it with `mvn test` (need to have a containing file, `frwiki-[YYYYMMDD]-download-complete` empty flag file into the `base-dir` defined into the `extraction-properties` file)