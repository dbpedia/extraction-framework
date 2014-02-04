<?php
class OAIHook {
	static function updatePage( $id, $action ) {
		$dbw = wfGetDB( DB_MASTER );
		$dbw->replace( 'updates',
			array( 'up_page' ),
			array( 'up_page'      => $id,
				'up_action'    => $action,
				'up_timestamp' => $dbw->timestamp(),
				'up_sequence'  => null ), # FIXME
			'oaiUpdatePage' );
		return true;
	}

	/**
	 * @param $article Article
	 * @param $user User
	 * @param $reason
	 * @return bool
	 */
	static function updateDelete( $article, $user, $reason ) {
		global $oaiDeleteIds;
		$title = $article->getTitle()->getPrefixedText();
		if( isset( $oaiDeleteIds[$title] ) ) {
			self::updatePage( $oaiDeleteIds[$title], 'delete' );
		}
		return true;
	}

	/**
	 * @param DatabaseUpdater $updater
	 * @return bool
	 */
	static function updateSchemaHook( DatabaseUpdater $updater ) {
		$updater->addExtensionTable( 'updates', dirname( __FILE__ ) . '/update_table.sql' );
		$updater->addExtensionTable( 'oaiuser', dirname( __FILE__ ) . '/oaiuser_table.sql' );
		$updater->addExtensionTable( 'oaiharvest', dirname( __FILE__ ) . '/oaiharvest_table.sql' );
		$updater->addExtensionTable( 'oaiaudit', dirname( __FILE__ ) . '/oaiaudit_table.sql' );
		return true;
	}

	/**
	 * @param $article Article
	 * @param $user
	 * @param $text
	 * @param $summary
	 * @param $isminor
	 * @param $iswatch
	 * @param $section
	 * @param $flags
	 * @param $revision
	 * @return bool
	 */
	static function updateSave( $article, $user, $text, $summary, $isminor, $iswatch, $section, $flags, $revision ) {
		if( $revision ) {
			// Only save a record for real updates, not null edits.
			$id = $article->getID();
			self::updatePage( $id, 'modify' );
		}
		return true;
	}

	/**
	 * @param $article Article
	 * @param $user User
	 * @param $reason string
	 * @return bool
	 */
	static function updateDeleteSetup( $article, $user, $reason ) {
		global $oaiDeleteIds;
		$title = $article->getTitle()->getPrefixedText();
		$oaiDeleteIds[$title] = $article->getID();
		return true;
	}

	static function updateMove( $from, $to, $user, $fromid, $toid ) {
		self::updatePage( $fromid, 'modify' );
		self::updatePage( $toid, 'modify' );
		return true;
	}

	static function testTables( &$tables ) {
		$tables[] = 'updates';
		return true;
	}

	/**
	 * @param $title Title
	 * @param $isnewid
	 * @return bool
	 */
	static function updateUndelete( $title, $isnewid ) {
		$article = new Article($title);
		$id = $article->getID();
		self::updatePage( $id, 'modify' );
		return true;
	}
}