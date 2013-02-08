Author: Dimitris Kontokostas (jimkont [at] gmail . com)
Date: 2011-03-30

Note: ProcessInterLanguageLinks.scala does pretty much the same thing as this script, 
but for many languages at once and probably faster (~20  minutes for ~100 languages).

creates owl:sameAs / rdfs:seeAlso links between to DBpedia's.
owl:sameAs are created only when there are 2-way links between articles
rdfs:seeAlso are create only on 1-way links

-future works
when language parameters are in external files, it could read direclty from the file all available languages
the owl:sameAs links are common for both languages, maybe store them in a different directory structure

parameters the language code:
e.g. : sh interwiki_links.sh 'el' 'en'
generates links from 'el' to 'en'
