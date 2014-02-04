#!/bin/bash
#while :
#do
	php ../../maintenance/deleteOldRevisions.php --delete
	php ../../maintenance/deleteOrphanedRevisions.php
        php ../../maintenance/deleteArchivedRevisions.php --delete 
        php ../../maintenance/purgeOldText.php --purge
	echo "Reached end! Restarting in ~6 hours"
	#sleep 20000
#sleep 200000
#done
