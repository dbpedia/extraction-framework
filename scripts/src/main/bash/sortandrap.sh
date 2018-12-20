#!/bin/bash

if [ ! -d "tmpfolder" ]; then
    mkdir -p tmpfolder
fi

for file in */src/main/databus/*/*.ttl.bz2; do
        echo "processing $file ...";
        lbzip2 -dc $file | LC_ALL=C sort --parallel=8 -u -T tmpfolder | rapper -i ntriples -O - - file | lbzip2 > tmpfile;
        echo "finished processing $file";
        mv tmpfile $file;
done
