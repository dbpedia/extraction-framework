#!/bin/sh

# sort the file
LC_ALL=C sort -u -o uris.lst uris.lst 

SHACL=`rapper -i turtle ../resources/shacl-tests/*  | cut -d ' ' -f1 | grep '^<' | sed 's/.*#//;s/^<//;s/>//' | sort -u | wc -l`

echo "# Minidump Overview

This readme is generated upon creation of the minidump by running \`./createMinidump.sh\` [code](https://github.com/dbpedia/extraction-framework/blob/master/dump/src/test/bash/createMinidump.sh).

## SHACL Tests 
Total: $SHACL

TODO match shacl to URIs with a SPARQL query

" >  minidump-overview.md

echo "
## Included Articles

" > minidump-overview.md
for i in `cat uris.lst` ; do
	echo "* $i">> minidump-overview.md
done 


# detect languages
LANG=`sed 's|^https://||;s|\.wikipedia.org.*||' uris.lst | sort -u` 



for l in ${LANG} ; do
	echo "LANGUAGE $l"
	PAGES=`grep "$l.wikipedia.org" uris.lst | sed 's|wikipedia.org/wiki/|wikipedia.org/wiki/Special:Export/|' `
	# copy header
	mkdir -p "../resources/minidumps/"$l
	TARGET="../resources/minidumps/"$l"/wiki.xml"
	echo "TARGET: $TARGET"
	cp head.xml "$TARGET"
	# process pages
	for p in ${PAGES}; do
		echo "PAGE: $p"
		echo "" >> "$TARGET"
		
		echo "<page>" >> $TARGET
		curl --progress-bar -L $p \
			| xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/" -t -c "//x:page" \
			| tail -n+2 >> $TARGET
		echo "" >> "$TARGET"
	done
	echo "</mediawiki>\n" >> $TARGET
	cat "$TARGET" | lbzip2 > "$TARGET.bz2"
	rm $TARGET
	 
done 

# curl $FIRST > $TMPFOLDER/main2.xml
#xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/" -t -c "//page" main2.xml
#  xmlstarlet ed  -N x="http://www.mediawiki.org/xml/export-0.10/" --subnode "/x:mediawiki/x:siteinfo" --type elem -n "newsubnode" -v "" head.xml
