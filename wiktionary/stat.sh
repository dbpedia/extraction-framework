#!/bin/sh

FILE=$1

TARGET=stats_results
SENSEFILE=$TARGET/sense.csv
SENSECOUNTFILE=$TARGET/sense_count.csv
PROPERTYCOUNTFILE=$TARGET/property_count.csv

mkdir -p $TARGET
rm -I $TARGET/*

echo "General Dump Statistics for file $FILE"
echo "---------"
DATE=`date`
echo "Date: $DATE"
MDSUM=`md5sum $FILE | cut -f1 -d ' '`
echo "md5sum: $MDSUM"
TRIPLECOUNT=`wc -l $FILE | cut -f1 -d ' '`
TOTAL=`wc -l $FILE | cut -f1 -d ' '`
echo "$TRIPLECOUNT triples"

LEXICALWORDCOUNT=`grep hasLangUsage $FILE | cut -f1 -d '>' | sed 's/<//;s/>//' | sort -u | wc -l | cut -f1 -d ' '`
echo "$LEXICALWORDCOUNT unique words parsed (at least a language usage detected)"
AVGTRIPELSPERWORD=`echo "scale = 2; $TRIPLECOUNT / $LEXICALWORDCOUNT" | bc -l `
echo "$AVGTRIPELSPERWORD triples/word"
echo "---------"

SUBJECTCOUNT=`cat $FILE  | cut -f1 -d '>' | sed 's/<//;s/>//' | sort -u | wc -l | cut -f1 -d ' '`
OBJECTCOUNT=`cat $FILE  | cut -f3 -d '>' | sed 's/<//;s/>//' | sort -u | wc -l | cut -f1 -d ' '`
RESOURCECOUNT=`echo "$SUBJECTCOUNT+$OBJECTCOUNT" | bc -l `
echo "$RESOURCECOUNT unique resources used."
echo "---------"


cat $FILE  | cut -f2 -d '>' | sed 's/<//;s/>//' | awk '{count[$1]++}END{for(j in count) print "<" j ">" "\t"count[j]}' > $PROPERTYCOUNTFILE
PREDICATECOUNT=`wc -l $PROPERTYCOUNTFILE | cut -f1 -d ' '`
echo "$PREDICATECOUNT unique predicates used. statistic saved to $PROPERTYCOUNTFILE"
echo "---------"

if [ $# -eq 2 ]
then
  FACET1=$2
  echo "Sense Statistics for $FACET1"
  grep hasSense $FILE | grep '\-'"$FACET1" |  cut -f1 -d '>' | sed 's/<//;s/>//'  > $SENSEFILE
  echo "all resources with type $FACET1 saved to $SENSEFILE (non-unique)"
else
  if [ $# -eq 3 ]
  then
    FACET1=$2
    FACET2=$3
    echo "Sense Statistics for $FACET1 $FACET2"
    grep hasSense $FILE | grep '\-'"$FACET1"'\-'"$FACET2" |  cut -f1 -d '>' | sed 's/<//;s/>//'  > $SENSEFILE
    echo "all resources with type $FACET1 $FACET2 saved to $SENSEFILE (non-unique)"
  else
    echo "Sense Statistics"
    grep hasSense $FILE |  cut -f1 -d '>' | sed 's/<//;s/>//'  > $SENSEFILE
    echo "all resources saved to $SENSEFILE (non-unique, one per sense)"
  fi
fi
TOTAL=`wc -l $SENSEFILE | cut -f1 -d ' '`
UNIQUE=`sort -u $SENSEFILE | wc -l | cut -f1 -d ' '` 
AVG=`echo "scale = 2; $TOTAL / $UNIQUE" | bc -l `

cat $SENSEFILE | awk  '{count[$1]++}END{for(j in count) print "<" j ">" "\t"count [j]}'  > $SENSECOUNTFILE
echo "count of senses saved to $SENSECOUNTFILE"

echo "Total $TOTAL / Unique $UNIQUE / Average $AVG "


