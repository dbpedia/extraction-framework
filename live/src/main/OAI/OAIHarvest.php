<?php

/**
 * OAI-PMH update harvester extension for MediaWiki 1.6+
 *
 * Copyright (C) 2005-2006 Brion Vibber <brion@pobox.com>
 * http://www.mediawiki.org/
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 * http://www.gnu.org/copyleft/gpl.html
 *
 *
 * This initial implementation is somewhat special-purpose;
 * it doesn't support foreign repositories using Dublin Core
 * and only performs basic updates and deletions. It's suitable
 * for updating current page revisions from a master wiki and
 * logging changes.
 *
 * PHP 5's DOM extension is required.
 *
 * @todo Charset conversion for Latin-1 wikis
 */

if( !defined( 'MEDIAWIKI' ) ) {
	die();
}

global $oaiSourceRepository;
global $oaiAgentExtra;

/**
 * Set to the repository URL,
 */
$oaiSourceRepository = null;
$oaiAgentExtra = ''; // additional notes optional

class OAIError extends Exception {
	// whee
}

class OAIHarvester {
	/**
	 * @param string $baseURL
	 */
	function __construct( $baseURL ) {
		$this->_baseURL = $baseURL;
	}

	/**
	 * Query the repository for updates, and run a callback for each item.
	 * Will continue across resumption tokens until there's nothing left.
	 *
	 * @param string $from timestamp to start at (???)
	 * @param callable $callback
	 * @return mixed true on success, OAIError on failure
	 * @throws OAIError
	 */
	function listUpdates( $from, $callback ) {
		$token = false;
		do {
			sleep(5);
			if( $token ) {
				echo "-> resuming at $token\n";
				$params = array(
					'verb'           => 'ListRecords',
					'metadataPrefix' => 'mediawiki',
					'resumptionToken' => $token );
			} else {
				$params = array(
					'verb'           => 'ListRecords',
					'metadataPrefix' => 'mediawiki',
					'from'           => OAIRepo::datestamp( $from ) );
			}
			$xml = $this->callRepo( $params );

			$doc = new DOMDocument( '1.0', 'utf-8' );
			if( !$doc->loadXML( $xml ) )
				throw new OAIError( "Invalid XML returned from OAI repository." );

			$xp = new DOMXPath( $doc );
			$xp->registerNamespace( 'oai', 'http://www.openarchives.org/OAI/2.0/' );
			$errors = $this->checkResponseErrors( $xp );
			if( $errors ) {
				return $errors;
			}

			$resultSet = $xp->query( '/oai:OAI-PMH/oai:ListRecords/oai:record' );
			foreach( $resultSet as $node ) {
				// Begin of my code (enclose in try/catch)				
				try{
					$record = OAIUpdateRecord::newFromNode( $node );
					call_user_func( $callback, $record );
					unset( $record );
				}
				catch(Exception $exp){
					wfDebug($exp->getMessage());
				}
				// End of my code (close try/catch)
			}

			$tokenSet = $xp->query( '/oai:OAI-PMH/oai:ListRecords/oai:resumptionToken' );
			$token = ( $tokenSet->length )
				? $tokenSet->item( 0 )->textContent
				: false;

			unset( $tokenSet );
			unset( $resultSet );
			unset( $xp );
			unset( $doc );
			unset( $xml );
		} while( $token );
		return true;
	}

	/**
	 * Check for OAI errors, and throw a formatted exception if present.
	 *
	 * @param XPathObject $xp
	 * @throws OAIError
	 */
	function checkResponseErrors( $xp ) {
		$errors = $xp->query( '/oai:OAI-PMH/oai:error' );
		if( !$errors )
			throw new OAIError( "Doesn't seem to be an OAI document...?" );

		if( $errors->length == 0 )
			return;

		$messages = array();
		for( $i = 0; $i < $errors->length; $i++ ) {
			$messages[$i] = $this->oaiErrorMessage( $errors->item( $i ) );
		}
		throw new OAIError( implode( "\n", $messages ) );
	}

	/**
	 * Format a single OAI error response as a text message
	 * @param  DomNode $node
	 * @return string
	 */
	function oaiErrorMessage( $node ) {
		$code = $node->getAttribute( 'code' );
		$text = $node->textContent;
		return "$code: $text";
	}

	function throwOAIErrors( $errors ) {
		$message = array();
		foreach( $errors as $node ) {
			$message[] = $node->x;
		}
	}

	/**
	 * Traverse a MediaWiki-format record set, sending an associative array
	 * of data to a callback function.
	 * @param DOMNodeList $recordSet     the <records> node (???????)
	 * @param mixed   $callback
	 */
	function traverseRecords( $recordSet, $callback ) {
		foreach( $recordSet as $node ) {
		//for( $node = $recordSet->firstChild; $node = $node->nextSibling; $node ) {
			$data = $this->extractMediaWiki( $node );
			call_user_func( $callback, $data );
		}
	}

	function extractMediaWiki( $node ) {
		return array( 'everything' => 'testing' );
	}

	/**
	 * Contact the HTTP repository with a given set of parameters,
	 * and return the raw XML response data.
	 *
	 * @param  array  $params Associative array of parameters
	 * @return string         Raw XML response
	 * @throws OAIError
	 */
	function callRepo( $params ) {
		$resultCode = null;
		$url = $this->requestURL( $params );
		wfDebug( "OAIHarvest::callRepo() - calling to $url\n" );
		$xml = $this->fetchURL( $url, $resultCode );
		if( $resultCode == 200 ) {
			return $xml;
		} else {
			throw new OAIError( "Repository returned HTTP result code $resultCode" );
		}
	}

	function requestURL( $params ) {
		$sep = (strpos( $this->_baseURL, '?' ) == false) ? '?' : '&';
		return $this->_baseURL . $sep . wfArrayToCGI( $params );
	}

	function userAgent() {
		global $oaiAgentExtra;
		$agent = 'MediaWiki OAI Harvester 0.2 (http://www.mediawiki.org/)';
		if( $oaiAgentExtra ) {
			$agent .= ' ' . $oaiAgentExtra;
		}
		return $agent;
	}

	/**
	 * Fetch a resource from the web
	 * @param string $url
	 * @param int &$resultCode accepts the HTTP result code
	 * @return string fetched data, or false
	 * @throws OAIError
	 */
	function fetchURL( $url, &$resultCode ) {
		if( !ini_get( 'allow_url_fopen' ) ) {
			throw new OAIError( "Can't open URLs; must turn on allow_url_fopen" );
		}

		$uagent = ini_set( 'user_agent', $this->userAgent() );
		echo "Fetching: $url\n";
		$result = file_get_contents( $url );
		ini_set( 'user_agent', $uagent );

		# FIXME
		if( $result === false ) {
			$resultCode = 500;
		} else {
			$resultCode = 200;
		}
		return $result;
	}

	function fetchURLviaCURL( $url, &$resultCode ) {
		$fetch = curl_init( $url );
		if( defined( 'OAIDEBUG' ) ) {
			curl_setopt( $fetch, CURLOPT_VERBOSE, 1 );
		}
		# CURLOPT_TIMEOUT
		# CURLOPT_REFERER?
		curl_setopt( $fetch, CURLOPT_USERAGENT, $this->userAgent() );

		ob_start();
		$ok = curl_exec( $fetch );
		$result = ob_get_contents();
		ob_end_clean();

		$info = curl_getinfo( $fetch );
		if( !$ok ) {
			echo "Something went awry...\n";
			var_dump( $info );
			die();
		}
		curl_close( $fetch );

		$resultCode = $info['http_code']; # ????
		return $result;
	}
}

class OAIUpdateRecord {
	var $_page = array();

	function __construct( $pageData ) {
		$this->_page = $pageData;
	}

	/**
	 * @return int
	 */
	function getArticleID() {
		return IntVal( $this->_page['id'] );
	}

	/**
	 * @return bool
	 */
	function isDeleted() {
		return isset( $this->_page['deleted'] );
	}

	/**
	 * @return Title
	 */
	function getTitle() {
		return Title::newFromText( $this->_page['title'] );
	}

	function getTimestamp( $time ) {
		$matches = array();
		if( preg_match( '/^(\d\d\d\d)-(\d\d)-(\d\d)T(\d\d):(\d\d):(\d\d)Z$/', $time, $matches ) ) {
			return wfTimestamp( TS_MW,
				$matches[1] . $matches[2] . $matches[3] .
				$matches[4] . $matches[5] . $matches[6] );
		} else {
			return 0;
		}
	}

	function dump() {
		if( $this->isDeleted() ) {
			printf( "%14s %10d\n", '(deleted page)', $this->getArticleID() );
		} else {
			$title = $this->getTitle();
			if( $title ) {
				printf( "%s %10d [[%s]]\n",
					$this->getTimestamp( $this->_page['revisions'][0]['timestamp'] ),
					$this->getArticleID(),
					$title->getPrefixedText() );
			} else {
				printf( "*** INVALID TITLE on %d: \"%s\"\n",
					$this->getArticleID(),
					$this->_page['title'] );
			}
		}
	}

	/**
	 * Perform the action of this thingy
	 * @throws OAIError
	 */
	function apply() {
		if( $this->isDeleted() ) {
			$this->doDelete();
		} else {
			$this->doEdit();
		}
		$this->checkpoint();
	}

	/**
	 * Update our checkpoint timestamp with the date from the
	 * OAI header; this allows us to pick up from the correct
	 * spot in the update sequence.
	 * @throws OAIError
	 */
	private function checkpoint() {
		global $oaiSourceRepository;
		$ts = $this->_page['oai_timestamp'];

		$dbw = wfGetDB( DB_MASTER );
		$dbw->replace( 'oaiharvest',
			array( 'oh_repository' ),
			array(
				'oh_repository' => $oaiSourceRepository,
				'oh_last_timestamp' => $dbw->timestamp( $ts ),
			),
			__METHOD__ );
	}

	/**
	 * Perform an edit from the update data
	 * @throws OAIError
	 */
	function doEdit() {
		$title = $this->getTitle();
		if( is_null( $title ) ) {
			throw new OAIError( sprintf(
				"Bad title for update to page #%d; cannot apply update: \"%s\"",
				$this->getArticleID(),
				$this->_page['title'] ) );
		}

		$id = 0;
		foreach( $this->_page['revisions'] as $revision ) {
			$id = $this->applyRevision( $revision );
		}

		RefreshLinks::fixLinksFromArticle( $id );

		if( isset( $this->_page['uploads'] ) ) {
			foreach( $this->_page['uploads'] as $upload ) {
				$this->applyUpload( $upload );
			}
		}
	}

	/**
	 * Apply a revision update.
	 * @param array $data
	 */
	function applyRevision( $data ) {
		$title = $this->getTitle();
		$pageId = $this->getArticleID();
		$timestamp = $this->getTimestamp( $data['timestamp'] );

		$dbw = wfGetDB( DB_WRITE );
		$dbw->begin();

		if( $data['id'] ) {
			$conflictingRevision = Revision::newFromId( $data['id'] );
			if( $conflictingRevision ) {
				echo "SKIPPING already-applied or conflicting revision {$data['id']}\n";
				$dbw->rollback();
				return;
			}
		}

		// Take a look...
		$article = $this->prepareArticle( $dbw, $pageId, $title );

		// Insert a revision
		$revision = new Revision( array(
			'id'         => isset( $data['id'] ) ? intval( $data['id'] ) : null,
			'page'       => $pageId,
			'user'       => intval( @$data['contributor']['id'] ),
			'user_text'  => isset( $data['contributor']['username'] )
								   ? strval( $data['contributor']['username'] )
								   : strval( $data['contributor']['ip'] ),
			'minor_edit' => isset( $data['minor'] ) ? 1 : 0,
			'timestamp'  => $timestamp,
			'comment'    => strval( @$data['comment'] ),
			'text'       => strval( $data['text'] ),
		) );
		$revId = $revision->insertOn( $dbw );
		echo "UPDATING to rev $revId\n";

		// Update the page record
		$article->updateRevisionOn( $dbw, $revision );

		$dbw->commit();
		// Begin My Code
		OAIHook::updatePage($pageId, 'modify');
		// End of my code

	}

	/**
	 * @param $db DatabaseBase
	 * @param $pageId int
	 * @param $title Title
	 * @return Article
	 */
	function prepareArticle( $db, $pageId, $title ) {
		$fname = 'OAIUpdateRecord::prepareArticle';

		$article = new Article( $title );
		$foundId = $article->getId();

		if( $foundId == $pageId ) {
			return $article;
		}

		if( $foundId != 0 ) {
			$this->hideConflictingPage( $db, $foundId, $title );
		}

		// Check to see if the page exists under a different title
		$foundTitle = Title::newFromId( $pageId );
		if( $foundTitle ) {
			// Rename it...
			echo "RENAMING page record\n";
			$db->update( 'page',
				array(
					'page_namespace' => $title->getNamespace(),
					'page_title'     => $title->getDBkey() ),
				array( 'page_id' => $pageId ),
				$fname );
		} else {
			// FIXME: prefer to use Article::insertOn here, but it doesn't provide
			// currently for overriding the page ID.
			echo "INSERTING page record\n";
			$db->insert( 'page', array(
				'page_id'           => $pageId,
				'page_namespace'    => $title->getNamespace(),
				'page_title'        => $title->getDBkey(),
				'page_counter'      => 0,
				'page_restrictions' => '', # fixme?
				'page_is_redirect'  => 0, # Will set this shortly...
				'page_is_new'       => 1,
				'page_random'       => wfRandom(),
				'page_touched'      => $db->timestamp(),
				'page_latest'       => 0, # Fill this in shortly...
				'page_len'          => 0, # Fill this in shortly...
			), $fname );
		}

		$title->resetArticleID( -1 );
		return new Article( $title );
	}

	/**
	 * Rename a conflicting page record
	 * @param Database $db
	 * @param int $existing
	 * @param Title $title
	 */
	function hideConflictingPage( $db, $existing, $title ) {
			echo "Hiding existing page [[" . $title->getPrefixedText() .
				"]] at id $existing\n";
			$db->update( 'page',
				array( 'page_title' => ' hidden@' . $existing ),
				array( 'page_id' => $existing ),
				'OAIUpdateRecord::hideConflictingPage' );
	}

	/**
	 * Update an image record
	 * @param array $upload
	 * @throws OAIError
	 */
	function applyUpload( $upload ) {
		$fname = 'OAIUpdateRecord::applyUpload';

		# FIXME: validate these files...
		if( strpos( $upload['filename'], '/' ) !== false
			|| strpos( $upload['filename'], '\\' ) !== false
			|| $upload['filename'] == ''
			|| $upload['filename'] !== trim( $upload['filename'] ) ) {
			throw new OAIError( 'Invalid filename "' . $upload['filename'] . '"' );
		}

		$dbw = wfGetDB( DB_MASTER );
		$data = array(
			'img_name'        => $upload['filename'],
			'img_size'        => IntVal( $upload['size'] ),
			'img_description' => $upload['comment'],
			'img_user'        => IntVal( @$upload['contributor']['id'] ),
			'img_user_text'   => isset( $upload['contributor']['username'] )
								   ? strval( $upload['contributor']['username'] )
								   : strval( $upload['contributor']['ip'] ),
			'img_timestamp'   => $dbw->timestamp( $this->getTimestamp( $upload['timestamp'] ) ),
			'img_metadata'    => serialize( array() ) );

		$dbw->begin();
		echo "REPLACING image row\n";
		$dbw->replace( 'image', array( 'img_name' ), $data, $fname );
		$dbw->commit();

		$this->downloadUpload( $upload );
	}

	/**
	 * Fetch a file to go with an updated image record
	 * $param array $upload update info data
	 * @throws OAIError
	 */
	function downloadUpload( $upload ) {
		global $wgEnableUploads;
		if( !$wgEnableUploads ) {
			echo "Uploads disabled locally: NOT fetching URL '" .
				$upload['src'] . "'.\n";
			return;
		}

		# We assume the filename has already been validated by code above us.
		echo "File updating temporarily broken on 1.11, sorry!\n";
		return;

		/*
		$timestamp = wfTimestamp( TS_UNIX, $this->getTimestamp( $upload['timestamp'] ) );
		if( file_exists( $filename )
			&& filemtime( $filename ) == $timestamp
			&& filesize( $filename ) == $upload['size'] ) {
			echo "Local file $filename matches; skipping download.\n";
			return;
		}

		if( !preg_match( '!^http://!', $upload['src'] ) )
			throw new OAIError( 'Invalid image source URL "' . $upload['src'] . "'." );

		$input = fopen( $upload['src'], 'rb' );
		if( !$input ) {
			unlink( $filename );
			throw new OAIError( 'Could not fetch image source URL "' . $upload['src'] . "'." );
		}

		if( file_exists( $filename ) ) {
			unlink( $filename );
		}
		wfMkdirParents( dirname( $filename ), null, __METHOD__ );
		if( !( $output = fopen( $filename, 'xb' ) ) ) {
			throw new OAIError( 'Could not create local image file "' . $filename . '" for writing.' );
		}

		echo "Fetching " . $upload['src'] . " to $filename: ";
		while( !feof( $input ) ) {
			$buffer = fread( $input, 65536 );
			fwrite( $output, $buffer );
			echo ".";
		}
		fclose( $input );
		fclose( $output );

		touch( $filename, $timestamp );
		echo " done.\n";
		*/
	}

	/**
	 * Delete the page record.
	 */
	function doDelete() {
		$dbw = wfGetDB( DB_MASTER );
		$dbw->begin();

		$title = Title::newFromId( $this->getArticleID() );
		if( $title ) {
			$article = new Article( $title );

			echo "DELETING\n";
			$article->doDeleteArticle( 'deleted from parent repository' );
		} else {
			echo "DELETING (not present)\n";
		}

		$dbw->commit();
		// Begin of my code
		OAIHook::updatePage($this->getArticleID(), 'delete');
		//End of my code 


		return true;
	}

	/**
	 * @param DomNode $node
	 */

	static function newFromNode( $node ) {
		$pageData = OAIUpdateRecord::readRecord( $node );
		return new OAIUpdateRecord( $pageData );
	}

	/**
	 * Collect page info out of an OAI record node containing export data.
	 * @param DOMNode $node
	 * @return array
	 * @throws OAIError
	 */
	function readRecord( $node ) {
		/*
		<record>
		  <header>
			<identifier>
			<datestamp>
		  <metadata>
			<mediawiki>
			  <page>
			  <title>
			  <id>
			  <restrictions>
			  <revision>
				<timestamp>
				<contributor>
				  <ip>
				  <id>
				  <username>
				<comment>
				<text>
				<minor>
		*/
		$header = oaiNextChild( $node, 'header' );

		if( $header->getAttribute( 'status' ) == 'deleted' ) {
			$pagedata = OAIUpdateRecord::grabDeletedPage( $header );
		} else {
			$metadata = oaiNextSibling( $header, 'metadata' );
			$mediawiki = oaiNextChild( $metadata, 'mediawiki' );
			$page = oaiNextChild( $mediawiki, 'page' );
			$pagedata = OAIUpdateRecord::grabPage( $page );
		}

		// We'll also need the OAI datestamp to ensure
		// update stream continuity...
		$datestamp = oaiNextChild( $header, 'datestamp' );
		$pagedata['oai_timestamp'] = wfTimestamp( TS_MW, $datestamp->textContent );

		return $pagedata;
	}

	/**
	 * Extract deleted page information from the OAI record
	 * @param DOMNode $header
	 * @return array
	 * @throws OAIError
	 */
	function grabDeletedPage( $header ) {
		/*
			<header status="deleted">
				<identifier>oai:en.wikipedia.org:enwiki:1581436</identifier>
				<datestamp>2005-03-08T07:07:36Z</datestamp>
			</header>
		*/
		$identifier = oaiNextChild( $header, 'identifier' );

		$ident = $identifier->textContent;
		$bits = explode( ':', $ident );
		$id = intval( $bits[count( $bits ) - 1] );
		if( $id <= 0 )
			throw new OAIError( "Couldn't understand deleted page identifier '$ident'" );

		return array(
			'id' => $id,
			'deleted' => true );
	}

	/**
	 * Extract non-deleted page information from the OAI record
	 * @param DOMNode $page
	 * @return array
	 * @throws OAIError
	 */
	function grabPage( $page ) {
		$data = array();
		foreach( $page->childNodes as $node ) {
			if( $node->nodeType == XML_ELEMENT_NODE ) {
				switch( $element = $node->nodeName ) {
				case 'title':
				case 'id':
				case 'restrictions':
					$data[$element] = $node->textContent;
					break;
				case 'revision':
					$data['revisions'][] = OAIUpdateRecord::grabRevision( $node );
					break;
				case 'upload':
					$data['uploads'][] = OAIUpdateRecord::grabUpload( $node );
					break;
				default:
					wfDebug( "Unexpected page element <$element>" );
				}
			}
		}
		return $data;
	}

	/**
	 * @param DOMNode $revision
	 * @return \map
	 */
	function grabRevision( $revision ) {
		return oaiNodeMap( $revision, array(
			'id',
			'timestamp',
			'comment',
			'minor',
			'text',
			'contributor' => array( 'OAIUpdateRecord', 'grabContributor' ) ) );
	}

	function grabUpload( $upload ) {
		return oaiNodeMap( $upload, array(
			'timestamp',
			'comment',
			'filename',
			'src',
			'size',
			'contributor' => array( 'OAIUpdateRecord', 'grabContributor' ) ) );
	}

	function grabContributor( $node ) {
		return oaiNodeMap( $node, array(
			'id',
			'ip',
			'username' ) );
	}
}

/**
 * Run through a DOM node's child elements grabbing selected text
 * values into a structure. The map array can contain a combination
 * of entries indicating text import and callbacks for fancier stuff.
 *
 * Unexpected child nodes will print a warning, but won't halt.
 *
 * @param DOMNode $parent
 * @param array $map
 * @return map
 */
function oaiNodeMap( $parent, $map ) {
	$callMap = array();
	$textMap = array();
	foreach( $map as $key => $value ) {
		if( is_int( $key ) ) {
			$textMap[$value] = true;
		} else {
			$callMap[$key] = $value;
		}
	}

	$data = array();
	foreach( $parent->childNodes as $node ) {
		if( $node->nodeType == XML_ELEMENT_NODE ) {
			$name = $node->nodeName;
			if( isset( $callMap[$name] ) ) {
				$data[$name] = call_user_func( $callMap[$name], $node );
			} elseif( isset( $textMap[$name] ) ) {
				$data[$name] = $node->textContent;
			} else {
				// fixme
				echo "Unexpected element <$name>\n";
			}
		}
	}

	return $data;
}

/**
 * Returns the first element node of a given tag name within the set of
 * the start node and its subsequent siblings.
 * If no such tag is found, an error object is returned.
 * @param DomNode $startNode
 * @param string  $element Optionally ignore other node types.
 * @return DomNode|OAIError
 */
function oaiNextElement( $startNode, $element = null ) {
	for( $node = $startNode;
		 $node;
		 $node = $node->nextSibling )
		if( $node->nodeType == XML_ELEMENT_NODE
		 && ( is_null( $element ) || $node->nodeName == $element ) )
			return $node;

	return new OAIError(
		is_null( $element )
			? "No more elements"
			: "Couldn't locate <$element>" );
}

function oaiNextChild( $parentNode, $element = null ) {
	if( !is_object( $parentNode ) ) {
		throw new OAIError( 'oaiNextChild given bogus node' );
	}
	return oaiNextElement( $parentNode->firstChild, $element );
}

function oaiNextSibling( $oneesan, $element = null ) {
	if( !is_object( $oneesan ) ) {
		throw new OAIError( 'oaiNextSibling given bogus node' );
	}
	return oaiNextElement( $oneesan->nextSibling, $element );
}


