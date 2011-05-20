#!/bin/bash
#Author: Dimitris Kontokostas (jimkont [at] gmail . com)
 
# This script works with directory structure produced by dump/Download.scala and reads the  
#scripts directory, in case it is called from elsewhere
CURRENTDIR=$( cd "$( dirname "$0" )" && pwd )

#Dump directory, where the config.properties is kept
EXTR_DUMP="$CURRENTDIR/../../../dump"

#Get outputDir and DumpDir from config.properties
OUTPUTDIR=`sed '/^\#/d' $EXTR_DUMP/config.properties | grep 'outputDir'  | tail -n 1 | cut -d "=" -f2- | sed 's/^[[:space:]]*//;s/[[:space:]]*$//'`


#TODO check arguments
LANG_FROM=$1
LANG_TO=$2

#init variables

INTERWIKI_FROM="$OUTPUTDIR/$LANG_FROM/interlanguage_links_$LANG_FROM.nt"
INTERWIKI_FROM_SORTED="$OUTPUTDIR/$LANG_FROM/interlanguage_links_$LANG_FROM.nt.sorted.$LANG_TO"

INTERWIKI_TO="$OUTPUTDIR/$LANG_TO/interlanguage_links_$LANG_TO.nt"
INTERWIKI_TO_REVERSED="$OUTPUTDIR/$LANG_TO/interlanguage_links_$LANG_TO.nt.reversed.$LANG_FROM"

INTERWIKI_FROM_SAMEAS="$OUTPUTDIR/$LANG_FROM/sameas_$LANG_FROM-$LANG_TO.nt"

#check if interlanguage links files exist
if [ ! -f $INTERWIKI_FROM ]; then
    echo "$INTERWIKI_FROM not found! exiting..."
#    exit
fi

if [ ! -f $INTERWIKI_TO ]; then
    echo "$INTERWIKI_TO not found! exiting..."
#    exit
fi

GREP_LANG_FROM="http://$LANG_FROM.dbpedia.org"
GREP_LANG_TO="http://$LANG_TO.dbpedia.org"

if [ "$LANG_FROM" = "en" ]
then
	GREP_LANG_FROM="http://dbpedia.org"
fi

if [ "$LANG_TO" = "en" ]
then
	GREP_LANG_TO="http://dbpedia.org"
fi

echo -------------------------------------------------------------------------------
echo "Generating interlanguage links from $LANG_FROM to $LANG_TO"
echo -------------------------------------------------------------------------------
grep ${GREP_LANG_FROM} $INTERWIKI_TO | awk '{print $3 " " $2 " " $1 " ."}' | sort -u | cat > $INTERWIKI_TO_REVERSED

grep ${GREP_LANG_TO} $INTERWIKI_FROM | sort -u | cat > $INTERWIKI_FROM_SORTED

#owl:sameAs (http://www.w3.org/2002/07/owl#sameAs)
comm -12 $INTERWIKI_TO_REVERSED $INTERWIKI_FROM_SORTED > $INTERWIKI_FROM_SAMEAS
wc -l $INTERWIKI_FROM_SAMEAS

