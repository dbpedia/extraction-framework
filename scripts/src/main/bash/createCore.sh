#this script creates the core folder of a release which is loaded into the endpoint
#please make sure of selected languages and dataset every time in case something has changed
#execute this script in the root folder of the release on the dl server

cd $1;

#link all link files from the link folder
find $1/links/* -type f \( -iname "*.ttl.bz2" \) | env LC_ALL=C sort | while read path ; do
file=${path##*/} ;
ln -s $path core/$file;
done

#for languages specified (but not en): link three files
for lang in ar de es fr it ja nl pl pt ru zh; do
for file in short_abstracts_en_uris long_abstracts_en_uris labels_en_uris ; do
f=$file"_$lang.ttl.bz2" ;
echo "./core-i18n/$lang/$f";
ln -s $1/core-i18n/$lang/$f core/$f;
done
done


#for all the dataset names specified: link them from the english language folder
for file in  article_categories_en category_labels_en disambiguations_en external_links_en freebase_links_en geo_coordinates_en geo_coordinates_mappingbased_en geonames_links_en homepages_en images_en infobox_properties_en infobox_property_definitions_en instance_types_en instance_types_transitive_en interlanguage_links_chapters_en labels_en long_abstracts_en mappingbased_literals_en mappingbased_objects_en page_ids_en persondata_en revision_ids_en revision_uris_en short_abstracts_en skos_categories_en specific_mappingbased_properties_en transitive_redirects_en uri_same_as_iri_en wikipedia_links_en ; do
f="$file.ttl.bz2" ;
ln -s $1/core-i18n/en/$f core/$f;
done