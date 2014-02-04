#! /bin/bash

SLEEP=900 #300 # seconds (5 miutes delay is the ideal)
MAINTENANCE=600 # every X iterrations do maintenance

COUNTER=1

while :
do

	php oaiUpdate.php
	echo "Reached end!"

	if [  "$COUNTER" -gt "$MAINTENANCE" ]; 
	then
		echo "Lets do some maintenance stuff now..."
		# Every X iterations do some maintenance stuff
		php ../../maintenance/deleteOldRevisions.php --delete
#		php ../../maintenance/deleteOrphanedRevisions.php
		php ../../maintenance/deleteArchivedRevisions.php --delete 
#		php ../../maintenance/purgeOldText.php --purge
		COUNTER=1

	fi	

	let COUNTER=COUNTER+1 
	echo "Restarting in $SLEEP seconds"
	sleep $SLEEP
done
