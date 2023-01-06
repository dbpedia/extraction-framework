#!/bin/sh
n=10;
file="page_lang=ro_ids.ttl"
lang="ro"
while getopts n:l:f: flag
do
    case "${flag}" in
        n) n=${OPTARG};;
        l) lang=${OPTARG};;
        f) file=${OPTARG};;
       
    esac
done


echo "========================="
echo "n: $n";
echo "file: $file";
echo "========================="

grep -v "resource\/\w*\:" $file > temp.txt
shuf -n $n temp.txt | grep -oP "<http:\/\/${lang}\.dbpedia\.org\/resource\/\K(.*)> <" | sed "s/> <//g" | while read line; do echo "https://${lang}.wikipedia.org/wiki/$line"; done > uri_sample_random_${lang}_${n}.lst
#rm -f temp.txt