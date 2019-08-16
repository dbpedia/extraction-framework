#!/bin/sh

LANG="en fr"

for l in ${LANG} ; do
	TARGET="../resources/minidumps/"$l"/wiki.xml"
	# copy header
	cp head.xml "$TARGET"
    for u in `cat "uris-$l.lst"` ; do
		echo "" >> "$TARGET"
		echo $TARGET
		curl $u  | xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/"  -t -c "//x:page" >> $TARGET
		echo "" >> "$TARGET"
	done
	echo "</mediawiki>\n" >> $TARGET
	cat "$TARGET" | lbzip2 > "$TARGET.bz2"
	rm $TARGET
done

# curl $FIRST > $TMPFOLDER/main2.xml
#xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/" -t -c "//page" main2.xml
#  xmlstarlet ed  -N x="http://www.mediawiki.org/xml/export-0.10/" --subnode "/x:mediawiki/x:siteinfo" --type elem -n "newsubnode" -v "" head.xml
