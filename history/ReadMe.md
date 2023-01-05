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
But as a first step, we didn't want to impact the core module.

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

* Test it with `mvn test`, if you cold start you may have to artificially create a `frwiki-[YYYYMMDD]-download-complete` empty file into the `base-dir` defined into the `extraction-properties` file

### Triple extracted

Given this little wikipedia page : [Hôtes_de_passage](https://fr.wikipedia.org/wiki/H%C3%B4tes_de_passage)

-> The HistoryPageExtractor.scala extractor will produce : 
```
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/prov#Entity> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/prov#Revision> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://www.w3.org/ns/prov#qualifiedRevision> <http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://dbpedia.org/ontology/wikiPageRevisionID> "36815850"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://purl.org/dc/terms/created> "2009-01-06T16:56:57Z"^^<http://www.w3.org/2001/XMLSchema#dateTime> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://www.w3.org/ns/prov#wasRevisionOf> <http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=&ns=0> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://purl.org/dc/terms/creator> <http://fr.dbpedia.org/resource/Hôtes_de_passage__creator__82.244.44.195> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__creator__82.244.44.195> <http://rdfs.org/sioc/ns#ip_address> "82.244.44.195" .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://rdfs.org/sioc/ns#note> "Nouvelle page : ''Hôtes de passage'' appartient est une partie de ''La Corde et les souris'' consignée dans ''Le Miroir des limbes''.Malraux y mène des entretiens avec entre autre Senghor et un p..." .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://dbpedia.org/ontology/wikiPageLength> "214"^^<http://www.w3.org/2001/XMLSchema#nonNegativeInteger> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://dbpedia.org/ontology/wikiPageLengthDelta> "214"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage?oldid=36815850&ns=0> <http://dbpedia.org/ontology/isMinorRevision> "false"^^<http://www.w3.org/2001/XMLSchema#boolean> .
.....
```

-> HistoryStatsExtractor.scala extractor will produce : 
```
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/prov#Entity> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbUniqueContrib> "9"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2014> <http://purl.org/dc/elements/1.1/date> "2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "2"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2013"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2013> <http://purl.org/dc/elements/1.1/date> "2013"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2013> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2015> <http://purl.org/dc/elements/1.1/date> "2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "2"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2011"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2011> <http://purl.org/dc/elements/1.1/date> "2011"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2011> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2021"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2021> <http://purl.org/dc/elements/1.1/date> "2021"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2021> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "2"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2009> <http://purl.org/dc/elements/1.1/date> "2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerYear__2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "3"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2015> <http://purl.org/dc/elements/1.1/date> "10/2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__9/2013"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__9/2013> <http://purl.org/dc/elements/1.1/date> "9/2013"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__9/2013> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__1/2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__1/2009> <http://purl.org/dc/elements/1.1/date> "1/2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__1/2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "2"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__12/2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__12/2014> <http://purl.org/dc/elements/1.1/date> "12/2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__12/2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2014> <http://purl.org/dc/elements/1.1/date> "10/2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__3/2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__3/2015> <http://purl.org/dc/elements/1.1/date> "3/2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__3/2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__7/2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__7/2009> <http://purl.org/dc/elements/1.1/date> "7/2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__7/2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2011"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2011> <http://purl.org/dc/elements/1.1/date> "10/2011"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__10/2011> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "1"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/nbRevPerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__11/2021"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__11/2021> <http://purl.org/dc/elements/1.1/date> "11/2021"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__nbRevPerMonth__11/2021> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "2"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2014> <http://purl.org/dc/elements/1.1/date> "2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "591"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2013"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2013> <http://purl.org/dc/elements/1.1/date> "2013"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2013> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "467"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2015> <http://purl.org/dc/elements/1.1/date> "2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "645"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2011"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2011> <http://purl.org/dc/elements/1.1/date> "2011"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2011> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "434"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2021"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2021> <http://purl.org/dc/elements/1.1/date> "2021"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2021> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "723"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerYear> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2009> <http://purl.org/dc/elements/1.1/date> "2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerYearAvgSize__2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "338"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2015> <http://purl.org/dc/elements/1.1/date> "10/2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "669"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__9/2013"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__9/2013> <http://purl.org/dc/elements/1.1/date> "9/2013"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__9/2013> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "467"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__1/2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__1/2009> <http://purl.org/dc/elements/1.1/date> "1/2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__1/2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "305"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__12/2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__12/2014> <http://purl.org/dc/elements/1.1/date> "12/2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__12/2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "602"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2014"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2014> <http://purl.org/dc/elements/1.1/date> "10/2014"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2014> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "581"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__3/2015"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__3/2015> <http://purl.org/dc/elements/1.1/date> "3/2015"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__3/2015> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "621"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__7/2009"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__7/2009> <http://purl.org/dc/elements/1.1/date> "7/2009"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__7/2009> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "404"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2011"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2011> <http://purl.org/dc/elements/1.1/date> "10/2011"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__10/2011> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "434"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.wikipedia.org/wiki/Hôtes_de_passage> <http://dbpedia.org/ontology/avgRevSizePerMonth> "http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__11/2021"^^<http://www.w3.org/2001/XMLSchema#integer> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__11/2021> <http://purl.org/dc/elements/1.1/date> "11/2021"^^<http://www.w3.org/2001/XMLSchema#date> .
<http://fr.dbpedia.org/resource/Hôtes_de_passage__revPerMonthAvgSize__11/2021> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> "723"^^<http://www.w3.org/2001/XMLSchema#integer> .
```
