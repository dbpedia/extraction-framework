---
name: Data Issue
about: Problem with the data of DBpedia
title: ''
labels: 'data'
assignees: ''

---

# Issue still valid?
> DBpedia updates frequently in this order: 1. DIEF software (extracts data from wikidata), 2. monthly dumps, 3. online services loaded from dumps.
> We update http://dief.tools.dbpedia.org/server/extraction/ on a daily basis from the git and it reflects the current state. 
> 
> **Disclaimer:** The public SPARQL endpoints (e.g., http://dbpedia.org/sparql) and other applications build based on DBpedia's data are not in sync yet with the latest monthly extracted data. 
>
> Therefore, you can use this tool to extract an example page and check if the error persists in the latest software version, and add the link you used for verification, e.g., http://dief.tools.dbpedia.org/server/extraction/en/extract?title=United+States


Not sure what you want me to validate here. You can validate the issue using the "execute query" link below.



# Source
> Where did you find the data issue? Pick one, remove the others.

### Web / SPARQL 
> State the service (e.g. http://dbpedia.org/sparql) and the SPARQL query  
> give a link to the web / linked data pages (e.g. http://dbpedia.org/resource/Berlin)

### Release Dumps
> DBpedia provides monthly release dumps, cf. release-dashboard.dbpedia.org
> provide artifact & version or download link

### Running the DBpedia Extraction (DIEF) software 
> Please include all necessary information.


# Classification
> If you have some familiarity with DBpedia, please use the classification tags at (link) to correctly file this issue.  Otherwise skip this step. 



### Error Description
> Please state the nature of your technical emergency: 


### Error specification
> Pick the appropriate:

- Affected extraction artifacts (Databus artifact version or file identifiers):
	- https://databus.dbpedia.org/dbpedia/mappings/mappingbased-objects/mappingbased-objects_lang=en_disjointDomain.ttl.bz2
	- 
- Example DBpedia resource URL(s) having the error (one full IRI per line): 
	- http://dbpedia.org/resource/Leipzig 
	- 
- Erroneous triples RDF snippet (NTRIPLES): 
  ``` 
  
  ``` 
- Expected / corrected RDF outcome snippet (NTRIPLES): 
  ``` 
  
  ```

### Additional context
> Add any other context about the problem here.
