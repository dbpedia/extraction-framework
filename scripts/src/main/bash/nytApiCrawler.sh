#!/bin/sh
# Crawl complete nyt api and save to bz2 file
# please note the first non-empty response is at id 8900
# I excluded some fields from the response, please compare to: https://developer.nytimes.com/semantic_api.json#/README
# Since we don't really know where the last id is, this has to be stopped manually at some point
# params:  (api-key, result file, start id, end id)
# example call: sh api-crawler.sh eb1a30284ed023b88591fc5fa119a850 nyt-crawl-4.json 400001 500000
# The result file will be compressed with bzip2 automatically and appended with .bz2
#
# Dependencies:
# curl, jq, bzip2

apikey=$1;
targetFile="$2.bz2";

from=$(($3));
if [ -z ${from+x} ]; then
	from=0;
fi

to=$(($4));
if [ -z ${to+x} ]; then
	to=100000;
fi

start=$(date +%s);
assigned=0;
all=0;

(
echo "{";
for i in $(seq $from $to); do
	if [ $i -gt $from ]; then
		echo -n ",";
	fi
	echo -n "\"$i\":";
	res=$(curl -s "http://api.nytimes.com/svc/semantic/v2/concept/id/$i.json?fields=links,taxonomy,geocodes,scope_notes,variants,pages&api-key=$apikey" | jq -r '.results[0]');

	# sometines curl returns an empty sring instead of "null", we fix this here
	if [ -z ${res+x} ]; then
		res="null";
	fi
	# counting all and assigned ids
	if [ "$res" = "null" ]; then
		all=$((all + 1));
	else
		assigned=$((assigned + 1));
		all=$((all + 1));
	fi
	echo "$res";
	# giving summary reports every 100 ids
	if [ $(($i % 100)) -eq 0 ]; then
		echo -n "$i: " >&2;
		diff=$(($(date +%s)-$start));
		echo -n "$(($diff/60)):$(($diff%60))" >&2;
		echo " - assigned ids: $assigned of $all" >&2;
	fi
done;
echo "}";
) | perl -pe 's|(^\s*,"[0-9]+":)(\s*$)|$1null\n|g' | perl -pe 's/^(\s*"concept_rule":\s*"(?:.|\n)*?[^\\]".*)//mg' | bzip2 -c > $targetFile