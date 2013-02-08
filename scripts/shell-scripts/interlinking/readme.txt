Author: Dimitris Kontokostas (jimkont [at] gmail . com)
Date: 2010-12-13

Note: CanonicalizeUris.scala works mostly like this script. Example:
../run CanonicalizeUris /data/dbpedia interlanguage-links-same-as .nt.gz freebase-links -el-uris .nt.bz2 en el el

Generates the linking datasets for a non-english languages using the extracted triples form the sameAs Extractor
this is needed mainly for languages extracting with the original name article (IRI/URI) and also for others to filter the triples

*the dump and output directory are read from the dump/config.properties file
*the datasets are read from links.txt

the script downloads the datasets in $dumpdir/links (it is IMPORTANT that they are downloaded from the script because they are also sorted)
then every dataset is joined with the same as triples and saved in outputdir/$lang/links

parameters the language code:
e.g. : sh interlinking.sh 'el'
