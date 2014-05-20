<?php

if( !class_exists( 'DOMAttr' ) ) {
	echo
		"Requires PHP 5 with the DOM module enabled.\n" .
		"\n" .
		"Although enabled by default in most PHP configurations, this module\n" .
		"is sometimes shipped in a separate package by Linux distributions.\n" .
		"\n" .
		"Fedora 6 users, please try:\n" .
		"    yum install php-xml\n" .
		"\n";
	exit( 1 );
}

$base = dirname( dirname( dirname( __FILE__ ) ) );
require_once( "$base/maintenance/commandLine.inc" );

/**
 * Persistent data:
 * - Source repo URL
 * - Last seen update timestamp
 */
$harvester = new OAIHarvester( $oaiSourceRepository );

if( isset( $options['from'] ) ) {
	$lastUpdate = wfTimestamp( TS_MW, $options['from'] );
} else {
	$dbr = wfGetDB( DB_MASTER );

	$checkpoint = $dbr->selectField( 'oaiharvest', 'oh_last_timestamp',
	 	array( 'oh_repository' => $oaiSourceRepository ) );

	$highest = $dbr->selectField( 'revision', 'MAX(rev_timestamp)' );

	if( $checkpoint ) {
		$lastUpdate = wfTimestamp( TS_MW, $checkpoint );
		echo "Starting at last checkpoint: " .
			wfTimestamp( TS_DB, $lastUpdate ) . "\n";
	} elseif( $highest ) {
		$lastUpdate = wfTimestamp( TS_MW, $highest );
		echo "No local update checkpoint; starting at last-updated page: " .
			wfTimestamp( TS_DB, $lastUpdate ) . "\n";
	} else {
		# Starting from an empty database!
		echo "Empty database; starting at epoch: 1970-01-01 00:00:00\n";
		$lastUpdate = '19700101000000';
	}
}

if( isset( $options['debug'] ) ) {
	$callback = 'debugUpdates';
	function debugUpdates( $record ) {
		$record->dump();
		var_dump( $record );
	}
} elseif( isset( $options['dry-run'] ) ) {
	$callback = 'showUpdates';
	function showUpdates( $record ) {
		$record->dump();
	}
} else {
	$callback = 'applyUpdates';
	function applyUpdates( $record ) {
		$record->dump();
		$record->apply();
	}
}


$result = $harvester->listUpdates( $lastUpdate, $callback );
