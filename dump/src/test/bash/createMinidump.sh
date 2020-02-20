#!/bin/sh


# sort the file
LC_ALL=C sort -u -o uris.lst uris.lst 

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
		
		curl --progress-bar -L $p  | xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/"  -t -c "//x:page" >> $TARGET
		echo "" >> "$TARGET"
	done
	echo "</mediawiki>\n" >> $TARGET
	cat "$TARGET" | lbzip2 > "$TARGET.bz2"
	rm $TARGET
	 
done 

# curl $FIRST > $TMPFOLDER/main2.xml
#xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/" -t -c "//page" main2.xml
#  xmlstarlet ed  -N x="http://www.mediawiki.org/xml/export-0.10/" --subnode "/x:mediawiki/x:siteinfo" --type elem -n "newsubnode" -v "" head.xml
