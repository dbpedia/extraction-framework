#!/bin/bash

# note: the script switched to rap -> unescape unicode -> sort

if [ ! -d "tmpfolder" ]; then
    mkdir -p tmpfolder
fi

LOG=tmpfolder/logfile
echo -n "" > $LOG

for file in */src/main/databus/*/*.ttl.bz2; do
        echo "processing $file ..." >> $LOG  ;
        lbzip2 -dc $file |\
        rapper -i ntriples -O - - file 2>>$LOG |\
        ascii2uni -a U 2>>$LOG  |\
        LC_ALL=C sort --parallel=8 -u -T tmpfolder  |\
        lbzip2 > tmpfile;
        echo "finished processing $file" >> $LOG ;
        mv tmpfile $file;
done
echo "log written to $LOG"
