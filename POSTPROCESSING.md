# Post-Processing 
A loose list of post processing procedures
See the current state of integration into the official DBpedia Release at https://git.informatik.uni-leipzig.de/dbpedia-assoc/marvin-config/-/blob/master/functions.sh#L36

## ResolveTransitiveLinks
The script replaces all subjects/objects in a dataset by their transitive closure. All triples must use the same property. Cycles are removed. Example:
* Before: A -> B , B -> C 
* After:  A -> C , B -> C  

Wikipedia has a redirect mechanism if articles names change, to support stability. However, in wikitext this change is not reflected. The goal of this and the next step is to canoncialize the occurence of these IRIs by picking a representant (the last element in the redirection chain/path). This step computes the redirect chain (transitive links).

Links:
* Usage and parameters:[Code](https://github.com/dbpedia/extraction-framework/blob/master/scripts/src/main/scala/org/dbpedia/extraction/scripts/ResolveTransitiveLinks.scala) 
* Deployment: [MARVIN](https://git.informatik.uni-leipzig.de/dbpedia-assoc/marvin-config/-/blob/master/functions.sh#L41) 
* Datasets: [Generic/redirects](https://databus.dbpedia.org/dbpedia/generic/redirects), [Wikidata/redirects](https://databus.dbpedia.org/dbpedia/wikidata/redirects), (and as an internal step in mappings)


## MapObjectURIs
For a usage example and parameter docu, see the [Code](https://github.com/dbpedia/extraction-framework/blob/master/scripts/src/main/scala/org/dbpedia/extraction/scripts/MapObjectUris.scala) 

Performs canonicalization of all object IRIs replacing them by their (transitive) redirects, i.e. `http://dbpedia.org/resource/Barack_Obama_Jr` will be replaced by `http://dbpedia.org/resource/Barack_Obama`. This step depends on the calculation of transitive redirects.

## Type Consistency Check
For a usage example and parameter docu, see the [Code](https://github.com/dbpedia/extraction-framework/blob/master/scripts/src/main/scala/org/dbpedia/extraction/scripts/TypeConsistencyCheck.scala)

Takes the mapping-based properties dataset and the assigned rdf types and tries to classify them in correct or wrong statements by checking domain and range according to the definition of the properties in the [DBpedia ontology](https://databus.dbpedia.org/dbpedia/ontology/dbo-snapshots). In order to work correctly it is required the object IRIs are normalized (MapObjectURIs post processing).

Statements with predicate *p* for which the subject resource is from a different type than specified in `rdfs:domain` of *p* are passed to  `_disjointDomain` files, whereas statements with an object resource disjoint from `rdfs:range` will be passed `_disjointRange`  files. Statements where the types match or are subtypes of the expected ones are passed to the regular dataset files (without content variant). See [code](https://github.com/dbpedia/extraction-framework/blob/master/scripts/src/main/scala/org/dbpedia/extraction/scripts/TypeConsistencyCheck.scala) for more details.
