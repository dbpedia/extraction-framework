#!/bin/bash

# this script will collect all necessary statistics when a release is finished
# publish these files alongside the data and use the html example (directory wiki)
# on how to integrate the stats on the drupal wiki pages

# for more information about the parameters have a look at the classes in /statistics

../run TypeStatistics /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 .ttl.bz2 "short_abstracts" stats-instances-cd.json canonicalized donotlist donotlist
../run TypeStatistics /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 .ttl.bz2 "short_abstracts" stats-instances-ld.json localized donotlist donotlist
../run TypeStatistics /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 .ttl.bz2 "mappingbased_literals,mappingbased_objects,geo_coordinates_mappingbased,instance_types,instance_types_transitive" stats-mappingbased.json canonicalized listproperties donotlist
../run TypeStatistics /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 .ttl.bz2 "infobox_properties" stats-raw-infoboxes.json canonicalized listproperties donotlist
../run TypeStatistics /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 .ttl.bz2 "instance_types,instance_types_transitive" stats-type-statistics.json canonicalized donotlistprops listobjects

../run PostProcessinfStats /media/bigone/25TB/www/downloads.dbpedia.org/2015-10 stats-instances-ld.json stats-instances-cd.json stats-mappingbased.json stats-raw-infoboxes.json stats-type-statistics.json stats-general-stats.json stats-properties-stats.json stats-types-stats.json