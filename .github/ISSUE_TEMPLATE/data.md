---
name: Data Issue
about: Problem with the data of DBpedia
title: ''
labels: 'data'
assignees: ''

---

# Issue validity
> Some explanation: DBpedia Snapshot is produced every three months, see [Release Frequency & Schedule](https://www.dbpedia.org/blog/snapshot-2021-06-release/#anchor1), which is loaded into http://dbpedia.org/sparql . During these three months, Wikipedia changes and also the DBpedia Information Extraction Framework receives patches. At http://dief.tools.dbpedia.org/server/extraction/en/  we host a daily updated extraction web service that can extract one Wikipedia page at a time. To check whether your issue is still valid, please enter the article name, e.g. `Berlin` or `Joe_Biden` here: http://dief.tools.dbpedia.org/server/extraction/en/
> If the issue persists, please post the link from your browser here: 

# Error Description
> Please state the nature of your technical emergency: 

# Pinpointing the source of the error
> Where did you find the data issue? Non-exhaustive options are:
* Web/SPARQL, e.g. http://dbpedia.org/sparql or http://dbpedia.org/resource/Berlin, please **provide query or link**
* Dumps: dumps are managed by the Databus. Please **provide artifact & version or download link**
* DIEF: you ran the software and the error occured then, please **include all necessary information such as the extractor or log**. If you had problems running the software use [another issue template](https://github.com/dbpedia/extraction-framework/issues/new/choose)

# Details
> please post the details

> Wrong triples RDF snippet 
  ``` 
  
  ``` 
> Expected / corrected RDF outcome snippet 
  ``` 
  
  ```
>Example DBpedia resource URL(s)
```

```
> Other
