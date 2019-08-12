#!/bin/sh

LIST=uris.lst
URIS=`cat $LIST`
TARGET="../resources/minibenchmark.xml"

# copy header
cp head.xml "$TARGET"

for i in $URIS ; do
	echo "" >> "$TARGET"
	curl $i | xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/"  -t -c "//x:page" >> $TARGET
	echo "" >> "$TARGET"
done

echo "</mediawiki>\n" >> $TARGET

cat "$TARGET" | lbzip2 > "$TARGET.bz2"
rm $TARGET
# curl $FIRST > $TMPFOLDER/main2.xml
#xmlstarlet sel -N x="http://www.mediawiki.org/xml/export-0.10/" -t -c "//page" main2.xml
#  xmlstarlet ed  -N x="http://www.mediawiki.org/xml/export-0.10/" --subnode "/x:mediawiki/x:siteinfo" --type elem -n "newsubnode" -v "" head.xml
