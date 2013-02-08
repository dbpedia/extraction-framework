#!/bin/bash
#Author: Dimitris Kontokostas (jimkont [at] gmail . com)
 
# Note: CanonicalizeUris.scala works mostly like this script. Example:
# ../run CanonicalizeUris /data/dbpedia interlanguage-links-same-as .nt.gz freebase-links -el-uris .nt.bz2 en el el

# This script works with directory structure produced by dump/Download.scala and reads the  

#scripts directory, in case it is called from elsewhere
CURRENTDIR="$( cd "$( dirname "$0" )" && pwd )"

#Dump directory, where the config.properties is kept
EXTRACTOR_CONFIG="${CURRENTDIR}/../../../dump/extraction.properties"

#Get outputDir and DumpDir from config.properties
OUTPUTDIR=`sed '/^\#/d' "${EXTRACTOR_CONFIG}" | grep 'outputDir'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`
DUMPDIR=`sed '/^\#/d' "${EXTRACTOR_CONFIG}" | grep 'dumpDir'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`

#download url, this is the prefix of links.txt lines
DOWNLOAD_URL="http://downloads.dbpedia.org/current/links"

CUR_LANG=$1

if [ ! "$CUR_LANG" ]
then
	CUR_LANG=el
fi

LINKSDIR="$DUMPDIR/links"
echo -------------------------------------------------------------------------------
echo "Checking / Downloading datasets in $LINKSDIR"
echo -------------------------------------------------------------------------------

if [ ! -d "$LINKSDIR" ]; then
	mkdir $LINKSDIR
fi		

LINKS="$CURRENTDIR/links.txt"
for LINE in `cat $LINKS`;do
	DATASET="$LINKSDIR/$LINE"

	if [ ! -f "$DATASET" ]; then
		echo 'Downloading $LINE'
		wget -O - $DOWNLOAD_URL\/$LINE.bz2 | bzcat | sort -k 1b,1 -u -o $DATASET
	fi		
done

# reverse links
LINKSR="$CURRENTDIR/links-r.txt"
for LINE in `cat $LINKSR`;do
	DATASET="$LINKSDIR/$LINE"

	if [ ! -f "$DATASET" ]; then
		echo 'Downloading $LINE'
		wget -O - $DOWNLOAD_URL\/$LINE.bz2 | bzcat | awk '{print $3 " " $2 " " $1 " ."}' | sort -k 1b,1 -u -o $DATASET
	fi		
done
echo "OK..."

# TODO for lang, do actions...
echo -------------------------------------------------------------------------------
echo "Creating datasets for language '$CUR_LANG'"
echo -------------------------------------------------------------------------------


SAMEAS_ORIGINAL_FILE="$OUTPUTDIR/$CUR_LANG/sameas-en-$CUR_LANG.nt"
SAMEAS_FILE=$SAMEAS_ORIGINAL_FILE.tmp

#reorder sameas triples to get english link first
awk '{print $3 " " $1}' $SAMEAS_ORIGINAL_FILE | sort -k 1b,1 -u -o $SAMEAS_FILE

OUTLINKDIR=$OUTPUTDIR/$CUR_LANG/links
if [ ! -d "$OUTLINKDIR" ]; then
	mkdir $OUTLINKDIR
fi

#join all datasets in file
for LINE in `cat $LINKS $LINKSR`;do
	DATASET_IN=$LINKSDIR\/$LINE
	# generate dataset name with lang postfix	
	DATASET_OUT=$(echo "$OUTLINKDIR/$LINE" | sed -e "s/\.nt/_$CUR_LANG.nt/g" )
	echo "Generating $DATASET_OUT"
	if [ -f "$DATASET_OUT" ]; then
		rm $DATASET_OUT
	fi
	join $SAMEAS_FILE $DATASET_IN | awk '{print $2 " " $3" " $4 " ." }' > $DATASET_OUT
done	

#remove temp file
rm $SAMEAS_FILE
echo "OK..."

#statistcs...
echo -------------------------------------------------------------------------------
echo "Generated triples for language '$CUR_LANG'"
echo -------------------------------------------------------------------------------

wc -l $OUTLINKDIR\/*.nt

