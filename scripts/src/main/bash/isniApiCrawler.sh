#!/bin/sh
# TODO DOCUMENTATION override!!!
# Crawl complete nyt api and save to bz2 file
# please note the first non-empty response is at id 8900
# I excluded some fields from the response, please compare to: https://developer.nytimes.com/semantic_api.json#/README
# Since we don't really know where the last id is, this has to be stopped manually at some point
# params:  (api-key, result file, start id, end id)
# example call: sh api-crawler.sh eb1a30284ed023b88591fc5fa119a850 nyt-crawl-4.json 300001 400000
# The result file will be compressed with bzip2 automatically and appended with .bz2
#
# Dependencies:
# curl, jq, bzip2

sourceCSV=$1;
targetFile="$2.bz2";

from=$(($3));
if [ -z ${from+x} ]; then
	from=0;
fi

to=$(($4));
if [ -z ${to+x} ]; then
	to=100000;
fi

proxy=$5;

start=$(date +%s);
all=0;
(
echo '<?xml version="1.0" encoding="UTF-8" ?>';
echo '<ISNIRecords>';
while read line; do
    all=$((all + 1));
    if [ $all -lt $from ] || [ $all -gt $to ]; then
        continue
    fi

    uri=$(echo "$line" | perl -pe 's|^\s*"([^"]+)".*|http://www.isni.org/isni/$1|');

    if [ -z ${proxy+x} ]; then
	    curl -s "$uri" | perl -pe "s/^<\!DOCTYPE.*\n//";
	    echo "no proxy";
    else
	    curl -s -x "$proxy" "$uri" | perl -pe "s/^<\!DOCTYPE.*\n//";
	    echo "with proxy";
    fi

	# giving summary reports every 100 ids
	if [ $(( $all % 100 )) -eq 0 ]; then
		echo -n "$all: " >&2;
		diff=$(($(date +%s)-$start));
		echo "$(($diff/60)):$(($diff%60))" >&2;
	fi
done < "$sourceCSV";
echo "</ISNIRecords>";
) | bzip2 -c > $targetFile