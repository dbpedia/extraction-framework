<?php
if( !defined( 'MEDIAWIKI' ) ) {
	die();
}

class SpecialOAIRepository extends UnlistedSpecialPage {
	function __construct() {
		parent::__construct( 'OAIRepository' );
	}

	function setHeaders() {
		// NOP
	}

	function execute( $par ) {
		$this->getOutput()->disable();

		# FIXME: Replace the DB error handler
		header( 'Content-type: text/xml; charset=utf-8' );

		$repo = new OAIRepo( $this->getRequest() );
		$repo->respond();
	}
}

/* repo notes:
	302 -- failover server?
	503 - service unavailable, include a Retry-After!
*/

/**
 * @param string $element
 * @param array $attribs Name=>value pairs. Values will be escaped.
 * @param bool $contents NULL to make an open tag only
 * @return string
 */
function oaiTag( $element, $attribs, $contents = null) {
	$out = '<' . $element;
	foreach( $attribs as $name => $val ) {
		$out .= ' ' . $name . '="' . xmlsafe( $val ) . '"';
	}
	if( is_null( $contents ) ) {
		$out .= '>';
	} elseif( $contents == '' ) {
		$out .= '/>';
	} else {
		$out .= '>';
		$out .= xmlsafe( $contents );
		$out .= "</$element>";
	}
	return $out;
}

class OAIRepo {
	function __construct( $request ) {
		$this->_db = wfGetDB( DB_SLAVE );
		$this->_errors = array();
		$this->_clientId = 0;
		$this->_request = $this->initRequest( $request );
	}

	/**
	 * @return string
	 */
	static function datestamp( $timestamp, $granularity = 'YYYY-MM-DDThh:mm:ssZ' ) {
		$formats = array(
			'YYYY-MM-DD'           => '$1-$2-$3',
			'YYYY-MM-DDThh:mm:ssZ' => '$1-$2-$3T$4:$5:$6Z' );
		if( !isset( $formats[$granularity] ) ) {
			wfDebugDieBacktrace( 'oaiFormatDate given illegal output format' );
		}
		return preg_replace(
			'/^(....)(..)(..)(..)(..)(..)$/',
			$formats[$granularity],
			wfTimestamp( TS_MW, $timestamp ) );
	}

	function addError( $code, $message ) {
		$this->_errors[] = array( $code, $message );
	}

	/**
	 * @return bool
	 */
	function errorCondition() {
		return !empty( $this->_errors );
	}

	/**
	 * @param $request WebRequest
	 * @return array
	 */
	function initRequest( $request ) {
		/* Legal verbs and their parameters */
		$verbs = array(
			'GetRecord' => array(
				'required'  => array( 'identifier', 'metadataPrefix' ) ),
			'Identify' => array(),
			'ListIdentifiers' => array(
				'exclusive' =>        'resumptionToken',
				'required'  => array( 'metadataPrefix' ),
				'optional'  => array( 'from', 'until', 'set' ) ),
			'ListMetadataFormats' => array(
				'optional'  => array( 'identifier' ) ),
			'ListRecords' => array(
				'exclusive' =>        'resumptionToken',
				'required'  => array( 'metadataPrefix' ),
				'optional'  => array( 'from', 'until', 'set' ) ),
			'ListSets' => array(
				'exclusive' => 'resumptionToken' ) );

		$req = array();
		$verb = $request->getVal( 'verb' );
		if( isset( $verbs[$verb] ) ) {
			$req['verb'] = $verb;
			$params = $verbs[$verb];

			/* If an exclusive parameter is set, it's the only one we'll see */
			if( isset( $params['exclusive'] ) ) {
				$exclusive = $request->getVal( $params['exclusive'] );
				if( !is_null( $exclusive ) ) {
					# FIXME: complain if other values found
					$req[$params['exclusive']] = $exclusive;
					return $req;
				}
			}

			/* Required parameters must all be present if no exclusive was found */
			if( isset( $params['required'] ) ) {
				foreach( $params['required'] as $name ) {
					$val = $request->getVal( $name );
					if( is_null( $val ) ) {
						$this->addError( 'badArgument', "Missing required argument '" . $name . "'." );
					} else {
						$req[$name] = $val;
					}
				}
			}

			/* Optionals are, well, optional. */
			if( isset( $params['optional'] ) ) {
				foreach( $params['optional'] as $name ) {
					$val = $request->getVal( $name );
					if( !is_null( $val ) ) {
						$req[$name] = $val;
					}
				}
			}
		} else {
			$this->addError( 'badVerb', 'Unrecognized or no verb provided.' );
		}
		return $req;
	}

	function validateMetadata( $var ) {
		if( isset( $this->_request[$var] ) ) {
			$prefix = $this->_request[$var];
			$formats = $this->metadataFormats();
			if( isset( $formats[$prefix] ) ) {
				return $this->_request[$var];
			} else {
				$this->addError( 'cannotDisseminateFormat', 'Requested unsupported metadata format.' );
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * @param $var
	 * @return Mixed|null
	 */
	function validateDatestamp( $var ) {
		if( isset( $this->_request[$var] ) ) {
			$time = $this->_request[$var];
			$matches = array();
			if( preg_match( '/^(\d\d\d\d)-(\d\d)-(\d\d)$/', $time, $matches ) ) {
				return wfTimestamp( TS_UNIX,
					$matches[1] . $matches[2] . $matches[3] . '000000' );
			} elseif( preg_match( '/^(\d\d\d\d)-(\d\d)-(\d\d)T(\d\d):(\d\d):(\d\d)Z$/', $time, $matches ) ) {
				return wfTimestamp( TS_UNIX,
					$matches[1] . $matches[2] . $matches[3] .
					$matches[4] . $matches[5] . $matches[6] );
			} else {
				$this->addError( 'badArgument', "Illegal timestamp format in '$var'" );
			}
		}
		return null;
	}

	/**
	 * Ensure the client is authorized to access the OAI feed.
	 * Restrictions are optional; a default install is unrestricted.
	 * HTTP headers may be sent as a side effect for unauthorized clients.
	 *
	 * Currently two restrictions are allowed:
	 * - $oaiAgentRegex whitelists clients matching the User-Agent header
	 * - $oaiAuth uses HTTP authentication to match usernames and passwords
	 *   against the oaiuser database.
	 *
	 * @return bool true if ok, false if not
	 */
	private function authorize() {
		global $oaiAgentRegex, $oaiAuth;

		if( $oaiAgentRegex == '' && !$oaiAuth ) {
			// No authorization required.
			return true;
		}

		if( $oaiAgentRegex != ''
			&& isset( $_SERVER['HTTP_USER_AGENT'] )
			&& preg_match( $oaiAgentRegex, $_SERVER['HTTP_USER_AGENT'] ) ) {
			// Agent whitelist bypasses users for compatibility
			return true;
		}

		if( $oaiAuth ) {
			if( isset( $_SERVER['PHP_AUTH_USER'] )
				&& isset( $_SERVER['PHP_AUTH_PW'] )
				&& $this->authenticateUser( $_SERVER['PHP_AUTH_USER'],
					$_SERVER['PHP_AUTH_PW'] ) ) {
				return true;
			}

			header( 'WWW-Authenticate: Basic realm="OAIRepository"' );
			header( 'HTTP/1.x 401 Unauthorized' );
		} else {
			header( 'HTTP/1.x 403 Unauthorized' );
		}
		header( 'Content-Type: text/html; charset=utf-8' );
		echo "<p>Sorry, this resource is presently restricted-access.</p>";
		return false;
	}

	/**
	 * Attempt to authenticate the username and password against
	 * the repo user table (oaiuser)
	 * @param string $username
	 * @param string $password
	 * @return bool
	 */
	private function authenticateUser( $username, $password ) {
		$db = $this->getAuditDatabase();
		$id = $db->selectField(
			$this->auditTableName( 'oaiuser' ),
			'ou_id',
			array(
				'ou_name' => $username,
				'ou_password_hash' => md5( $password ),
			),
			__METHOD__ );
		if( $id ) {
			$this->_clientId = intval( $id );
			return true;
		} else {
			$this->_clientId = 0;
			return false;
		}
	}

	/**
	 * @param $responseSize int
	 */
	private function logRequest( $responseSize ) {
		global $oaiAudit, $wgDBname, $wgRequest;
		if( $oaiAudit ) {
			$db = $this->getAuditDatabase();
			$db->insert(
				$this->auditTableName( 'oaiaudit' ),
				array(
					'oa_client' => $this->_clientId,
					'oa_timestamp' => $db->timestamp(),
					'oa_ip' => $wgRequest->getIP(),
					'oa_agent' => @$_SERVER['HTTP_USER_AGENT'],
					'oa_dbname' => $wgDBname,
					'oa_response_size' => $responseSize,
					'oa_request' => wfArrayToCGI( $this->_request ),
				),
				__METHOD__ );
		}
	}

	/**
	 * Return a database connection to the repo authentication and
	 * audit logging database.
	 * @return DatabaseBase
	 */
	private function getAuditDatabase() {
		if( !isset( $this->mAuditDb ) ) {
			global $oaiAuditDatabase;
			$lb = wfGetLB( $oaiAuditDatabase );
			$this->mAuditDb = $lb->getConnection( DB_MASTER, 'oaiAudit', $oaiAuditDatabase );
		}
		return $this->mAuditDb;
	}

	/**
	 * Would be nice to offload this to the Database class
	 * in a safe, consistent manner.
	 * To avoid duplicate connections which confuse something
	 * in configuration, possibly reuse an existing connection...
	 * @param $table string
	 * @return string prefixed table name
	 */
	private function auditTableName( $table ) {
		global $oaiAuditDatabase;
		if( $oaiAuditDatabase ) {
			// Shared db between wikis?
			return "`$oaiAuditDatabase`.`$table`";
		} else {
			return $table;
		}
	}

	function respond() {
		if( !$this->authorize() ) {
			return;
		}

		// We want to record the size of requests for auditing's sake.
		// We'd like compressed size, but that doesn't seem happy. :(
		ob_start();

		header( 'Content-type: text/xml; charset=utf-8' );
		echo '<' . '?xml version="1.0" encoding="UTF-8" ?' . ">\n";
		echo oaiTag( 'OAI-PMH', array(
			'xmlns'              => 'http://www.openarchives.org/OAI/2.0/',
			'xmlns:xsi'          => 'http://www.w3.org/2001/XMLSchema-instance',
			'xsi:schemaLocation' => 'http://www.openarchives.org/OAI/2.0/ ' .
									'http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd' ) )
			. "\n";
		echo $this->responseDate();
		echo $this->regurgitateRequest();
		if( !$this->errorCondition() ) {
			$this->doResponse( $this->_request['verb'] );
		}
		$this->showErrors();
		echo "</OAI-PMH>\n";

		$size = intval( ob_get_length() );
		$this->logRequest( $size );
	}

	/**
	 * @return string
	 */
	function responseDate() {
		$date = OAIRepo::datestamp( time(), $this->timeGranularity() );
		return "<responseDate>$date</responseDate>\n";
	}

	/**
	 * @return string
	 */
	function regurgitateRequest() {
		return oaiTag( 'request', $this->_request, '' ) . "\n";
	}

	function showErrors() {
		foreach( $this->_errors as $err ) {
			echo $this->errorMessage( $err[0], $err[1] );
			echo "\n";
		}
	}

	/**
	 * @param $code
	 * @param $message
	 * @return string
	 */
	function errorMessage( $code, $message ) {
		return oaiTag( 'error',
			array( 'code' => $code ),
			$message );
	}

	/**
	 * @param $verb
	 */
	function doResponse( $verb ) {
		switch( $verb ) {
		case 'Identify':
			$this->Identify();
			break;
		case 'ListIdentifiers':
		case 'ListRecords':
			$this->listRecords( $verb );
			break;
		case 'ListSets':
			$this->addError( 'noSetHierarchy', "This repository doesn't support sets." );
			break;
		case 'ListMetadataFormats':
			$this->listMetadataFormats();
			break;
		case 'GetRecord':
			$this->GetRecord();
			break;
		default:
			# This shouldn't happen
			wfDebugDieBacktrace( 'Verb not implemented' );
		}
	}

	function Identify() {
		echo "<Identify>\n";
		foreach( $this->identifyInfo() as $field => $val ) {
			echo oaiTag( $field, array(), $val ) . "\n";
		}
		echo "</Identify>\n";
	}

	function listMetadataFormats() {
		if( isset( $this->_request['identifier'] ) ) {
			# We have the same formats for all records...
			# If given an identifier, just check it for existence.
			$row = $this->getRecordItem( $this->_request['identifier'], '' );
			if( $this->errorCondition() ) {
				return;
			}
		}

		$formats = $this->metadataFormats();
		echo "<ListMetadataFormats>\n";
		foreach( $formats as $prefix => $format ) {
			echo "<metadataFormat>\n";
			echo oaiTag( 'metadataPrefix', array(), $prefix ) . "\n";
			echo oaiTag( 'schema', array(), $format['schema'] ) . "\n";
			echo oaiTag( 'metadataNamespace', array(), $format['namespace'] ) . "\n";
			echo "</metadataFormat>\n";
		}
		echo "</ListMetadataFormats>\n";
	}

	function validateToken( $var ) {
		if( !isset( $this->_request[$var] ) ) {
			return null;
		}
		$matches = array();
		if( preg_match( '/^([a-z_]+):(\d+)(?:|:(\d{14}))$/', $this->_request[$var], $matches ) ) {
			$token = array();
			$token['metadataPrefix'] = $matches[1];
			$token['resume']         = IntVal( $matches[2] );
			$token['until']          = isset( $matches[3] )
										 ? wfTimestamp( TS_MW, $matches[3] )
										 : null;
			$formats = $this->metadataFormats();
			if( isset( $formats[$token['metadataPrefix']] ) ) {
				return $token;
			}
		}
		$this->addError( 'badResumptionToken', 'Invalid resumption token.' );
	}

	function listRecords( $verb ) {
		$withData = ($verb == 'ListRecords');

		$startToken = $this->validateToken( 'resumptionToken' );
		if( $this->errorCondition() ) {
			return;
		}
		if( $startToken ) {
			$metadataPrefix = $startToken['metadataPrefix'];
			$resume         = $startToken['resume'];
			$from           = null;
			$until          = $startToken['until'];
		} else {
			$metadataPrefix = $this->validateMetadata( 'metadataPrefix' );
			$resume         = null;
			$from           = $this->validateDatestamp( 'from' );
			$until          = $this->validateDatestamp( 'until' );
			if( isset( $this->_request['set'] ) ) {
				$this->addError( 'noSetHierarchy', 'This repository does not support sets.' );
			}
			if( $this->errorCondition() ) {
				return;
			}
		}

		# Fetch one extra row to check if we need a resumptionToken
		$resultSet = $this->fetchRows( $from, $until, $this->chunkSize() + 1, $resume );
		$count = min( $resultSet->numRows(), $this->chunkSize() );
		if( $count ) {
			echo "<$verb>\n";
			// buffer everything up
			$rows = array();
			$this->_lastSequence = null;
			for( $i = 0; $i < $count; $i++ ) {
				$row = $resultSet->fetchObject();
				$rows[] = $row;
				$this->_lastSequence = $row->up_sequence;
			}
			$row = $resultSet->fetchObject();
			if( $row ) {
				$limit = wfTimestamp( TS_MW, $until );
				if( $until )
					$nextToken = "$metadataPrefix:$row->up_sequence:$limit";
				else
					$nextToken = "$metadataPrefix:$row->up_sequence";
			}
			$resultSet->free();
			// init writer
			$writer = $this->makeWriter($metadataPrefix,$rows);
			// render
			foreach( $rows as $row ) {
				$item = new WikiOAIRecord( $row, $writer );
				if( $withData ) {
					echo $item->renderRecord( $metadataPrefix, $this->timeGranularity() );
				} else {
					echo $item->renderHeader( $this->timeGranularity() );
				}
			}
			if( isset( $nextToken ) ) {
				echo oaiTag( 'resumptionToken', array(), $nextToken ) . "\n";
			}
			echo "</$verb>\n";
		} else {
			$this->addError( 'noRecordsMatch', 'No records available match the request.' );
		}
	}

	function getRecord() {
		$metadataPrefix =  $this->validateMetadata( 'metadataPrefix' );
		if( !$this->errorCondition() ) {
			$row = $this->getRecordItem( $this->_request['identifier']);
			if( !$this->errorCondition() ) {
				$writer = $this->makeWriter($metadataPrefix,array($row));
				$item = new WikiOAIRecord( $row, $writer );
				echo "<GetRecord>\n";
				echo $item->renderRecord( $metadataPrefix, $this->timeGranularity() );
				echo "</GetRecord>\n";
			}
		}
	}

	function getRecordItem( $identifier) {
		$pageid = $this->stripIdentifier( $identifier );
		if( $pageid ) {
			$resultSet = $this->fetchRecord( $pageid);
			$row = $resultSet->fetchObject();
			$resultSet->free();
			if( $row ) {
				return $row;
			}
		}
		$this->addError( 'idDoesNotExist', 'Requested identifier is invalid or does not exist.' );
		return null;
	}

	static function identifierPrefix() {
		static $prefix = false;
		if ( !$prefix ) {
			global $wgServer, $wgDBname;
			wfSuppressWarnings();
			$prefix = "oai:" . parse_url( $wgServer, PHP_URL_HOST ) . ":$wgDBname:";
			wfRestoreWarnings();
		}
		return $prefix;
	}

	function stripIdentifier( $identifier ) {
		$prefix = self::identifierPrefix();
		if( substr( $identifier, 0, strlen( $prefix ) ) == $prefix ) {
			$pageid = substr( $identifier, strlen( $prefix ) );
			if( preg_match( '/^\d+$/', $pageid ) ) {
				return IntVal( $pageid );
			}
		}
		return false;
	}

	function timeGranularity() {
		return 'YYYY-MM-DDThh:mm:ssZ';
	}

	function chunkSize() {
		global $oaiChunkSize;
		return $oaiChunkSize;
	}

	function baseUrl() {
		$title = SpecialPage::getTitleFor( 'OAIRepository' );
		return $title->getCanonicalUrl();
	}

	function earliestDatestamp() {
		$updates = $this->_db->tableName( 'updates' );
		$result = $this->_db->query( "SELECT MIN(up_timestamp) AS min FROM $updates" );
		$row = $this->_db->fetchObject( $result );
		if( $row ) {
			$this->_db->freeResult( $result );
			return $row->min;
		} else {
			wfDebugDieBacktrace( 'Bogus result.' );
		}
	}

	function makeWriter($metadataPrefix, $rows) {
		if($metadataPrefix == 'lsearch'){
			$res = $this->fetchReferenceData($rows);
			$writer = new OAILSearchWriter($res);
			$res->free();
			return $writer;
		} else
			return new OAIDumpWriter;
	}

	function fetchRecord( $pageid ) {
		$db = $this->_db;

		$tables = $this->getTables();
		$fields = $this->getFields();
		$conds = array();
		$options = array();
		$join_conds = $this->getJoinConds();

		$conds['up_page'] = $pageid;

		$options['LIMIT'] = 1;

		wfRunHooks( 'OAIFetchRecordQuery', array( &$tables, &$fields, &$conds,
						&$options, &$join_conds ) );

		return $db->select( $tables, $fields, $conds, __METHOD__,
					$options, $join_conds );
	}

	/**
	 * @return array
	 */
	private function getFields() {
		global $wgContentHandlerUseDB;

		$fields = array( 'page_namespace', 'page_title', 'old_text', 'old_flags',
			'rev_id', 'rev_deleted', 'rev_comment', 'rev_user',
			'rev_user_text', 'rev_timestamp', 'page_restrictions',
			'rev_minor_edit', 'rev_len', 'page_is_redirect', 'up_sequence',
			'page_id', 'up_timestamp', 'up_action', 'up_page',
			'page_len', 'page_touched', 'page_counter', 'page_latest',
		);

		if ( $wgContentHandlerUseDB ) {
			$fields[] = 'rev_content_model';
			$fields[] = 'rev_content_format';
		}

		return $fields;
	}

	/**
	 * @return array
	 */
	private function getJoinConds() {
		return array( 'page' => array( 'LEFT JOIN', 'page_id=up_page' ),
			'revision' => array( 'LEFT JOIN', 'page_latest=rev_id' ),
			'text' => array( 'LEFT JOIN', 'rev_text_id=old_id' )
		);
	}

	/**
	 * @return array
	 */
	private function getTables() {
		return array( 'updates', 'page', 'revision', 'text' );
	}

	function fetchRows( $from, $until, $chunk, $token = null ) {
		$db = $this->_db;
		$tables = $this->getTables();
		$fields = $this->getFields();
		$conds = array();
		$options = array();
		$join_conds = $this->getJoinConds();

		if( $token ) {
			$conds[] = 'up_sequence>=' . $db->addQuotes( $token );
			$options['ORDER BY'] = 'up_sequence';
		} else {
			$options['ORDER BY'] = 'up_timestamp';
		}
		if( $from ) {
			$conds[] = 'up_timestamp>='.$db->addQuotes( $db->timestamp( $from ) );
		}
		if( $until ) {
			$conds[] = 'up_timestamp<=' .$db->addQuotes( $db->timestamp( $until ) );
		}

		$options['LIMIT'] = $chunk;

		wfRunHooks( 'OAIFetchRowsQuery', array( &$tables, &$fields, &$conds,
			&$options, &$join_conds ) );

		return $db->select( $tables, $fields, $conds, __METHOD__,
			$options, $join_conds );
	}

	function fetchReferenceData( $rows ) {
		$page_ids = array();
		foreach( $rows as $row ){
			$page_ids[] = $row->up_page;
		}

		$res = $this->_db->select(
			array( 'u' => 'updates', 'p' => 'page', 'r' => 'redirect', 'rp' => 'page' ),
			array(
				'up_page,up_sequence',
				'rp.page_namespace AS page_namespace',
				'rp.page_title AS page_title'
			),
			array(
				'u.up_page=p.page_id',
				'p.page_namespace=r.rd_namespace',
				'p.page_title=r.rd_title',
				'r.rd_from=rp.page_id',
				'up_page' => $page_ids
			),
			__METHOD__
		);

		return $this->_db->resultObject( $res );
	}

	function identifyInfo() {
		global $wgSitename, $wgEmergencyContact;
		return array(
			'repositoryName' => $wgSitename,
			'baseURL' => $this->baseUrl(),
			'protocolVersion' => '2.0',
			'adminEmail' => $wgEmergencyContact,
			'earliestDatestamp' => self::datestamp(
				$this->earliestDatestamp(), $this->timeGranularity() ),
			'deletedRecord' => 'persistent',
			'granularity' => $this->timeGranularity(),

			# Optional
			'compression' => 'gzip',
			#'description'
			);
	}

	function metadataFormats() {
		return array(
			'oai_dc' => array(
				'namespace' => 'http://www.openarchives.org/OAI/2.0/oai_dc/',
				'schema'    => 'http://www.openarchives.org/OAI/2.0/oai_dc.xsd' ),
			'mediawiki' => array(
				'namespace'	=> 'http://www.mediawiki.org/xml/export-0.3/',
				'schema'    => 'http://www.mediawiki.org/xml/export-0.3.xsd' ) ,
			'lsearch' => array(
				'namespace'	=> 'http://www.mediawiki.org/xml/lsearch-0.1/',
				'schema'    => 'http://www.mediawiki.org/xml/lsearch-0.1.xsd' ) );
	}
}

class OAIRecord {
	function renderRecord( $format, $datestyle ) {
		$header = $this->renderHeader( $datestyle );
		$metadata = $this->isDeleted()
			? ''
			: $this->renderMetadata( $format );
		$about = $this->isDeleted()
			? ''
			: $this->renderAbout();
		return "<record>\n$header$metadata$about</record>\n";
	}

	function renderHeader( $datestyle ) {
		$tag = $this->isDeleted()
			? 'header status="deleted"'
			: 'header';
		$ident = xmlsafe( $this->getIdentifier() );
		$date = OAIRepo::datestamp( $this->getDatestamp(), $datestyle );
		return "<$tag>\n" .
			"  <identifier>$ident</identifier>\n" .
			"  <datestamp>$date</datestamp>\n" .
			"</header>\n";
	}

	function renderMetadata( $format ) {
		wfDebugDieBacktrace( 'Abstract' );
	}

	function renderAbout() {
		# Not supported yet
		return '';
	}

	/**
	 * Return the date and time when this record was last modified,
	 * created or deleted. This is needed for the header output.
	 * Override this...
	 *
	 * @return int UNIX timestamp (or other wfTimestamp()-compatible)
	 * @abstract
	 */
	function getDatestamp() {
		wfDebugDieBacktrace( 'Abstract OAIRecord::getDatestamp() called.' );
	}

	/**
	 * Return the record's unique OAI identifier.
	 * This is needed for the header output.
	 * Override this...
	 *
	 * @return string
	 * @abstract
	 */
	function getIdentifier() {
		wfDebugDieBacktrace( 'Abstract OAIRecord::getIdentifier() called.' );
	}

	/**
	 * True if this is a deleted record, false otherwise.
	 * Override if your repository supports marking deleted records.
	 *
	 * @return bool
	 */
	function isDeleted() {
		return false;
	}
}

class WikiOAIRecord extends OAIRecord {
	/**
	 * @param $row database row
	 * @param $writer XMLWriter
	 */
	function __construct( $row, $writer ) {
		$this->_id        = $row->up_page;
		$this->_timestamp = $row->up_timestamp;
		$this->_deleted   = is_null( $row->page_title );
		$this->_row       = $row;
		$this->_writer    = $writer;

		$this->_title          = null;
		$this->_contentModel   = null;
		$this->_contentHandler = null;
		$this->_contentFormat  = null;
	}

	/**
	 * @return bool
	 */
	function isDeleted() {
		return $this->_deleted;
	}

	/**
	 * @return string
	 */
	function getIdentifier() {
		return OAIRepo::identifierPrefix() . $this->_id;
	}

	function getDatestamp() {
		return $this->_timestamp;
	}

	function renderMetadata( $format ) {
		switch( $format ) {
		case 'oai_dc':
			$data = $this->renderDublinCore();
			break;
		case 'mediawiki':
			$data = $this->renderMediaWiki();
			break;
		case 'lsearch':
			$data = $this->renderLSearch();
			break;
		default:
			wfDebugDieBacktrace( 'Unsupported metadata format.' );
		}
		return "<metadata>\n$data</metadata>\n";
	}

	/**
	 * @return Title page's title
	 */
	function getTitle() {
		if ( $this->_title === null ) {
			$this->_title = Title::makeTitle( $this->_row->page_namespace, $this->_row->page_title );
		}

		return $this->_title;
	}

	/**
	 * @return String the content model ID
	 */
	function getContentModel() {
		if ( $this->_contentModel === null ) {
			if ( isset( $this->_row->rev_content_model ) ) {
				$this->_contentModel = $this->_row->rev_content_model;
			} else {
				$this->_contentModel = $this->getTitle()->getContentModel();
			}
		}

		return $this->_contentModel;
	}

	/**
	 * @return ContentHandler the appropriate content handler
	 */
	function getContentHandler() {
		if ( $this->_contentHandler === null ) {
			$this->_contentHandler = ContentHandler::getForModelID( $this->getContentModel() );
		}

		return $this->_contentHandler;
	}

	/**
	 * @return String the revision's serialization format
	 */
	function getContentFormat() {
		if ( $this->_contentFormat === null ) {
			if ( isset( $this->_row->rev_content_format ) ) {
				$this->_contentFormat = $this->_row->rev_content_format;
			} else {
				$this->_contentFormat = $this->getContentHandler()->getDefaultFormat();
			}
		}

		return $this->_contentFormat;
	}

	/**
	 * Returns whether this record's content is textual
	 *
	 * @return bool
	 */
	function hasTextContent() {
		//XXX: need ContentHandler::isTextContentModel( $id ) would be handy
		$handler = $this->getContentHandler();
		return ( $handler instanceof TextContentHandler );
	}

	/**
	 * Note: old versions that worked on MW 1.4 included the page text as
	 * the dc:description field. Then it was broken for a long time. :)
	 * I'm now stripping out the text, as it's not really appropriate
	 * for the description field.
	 *
	 * This allows the use of oai_dc format to grab metadata about the pages
	 * without fetching the actual page content, which should be more useful
	 * for those simply wanting a set of page update notifications.
	 */
	function renderDublinCore() {
		// See DCMI Type Vocabulary.
		$type = $this->hasTextContent() ? 'Text' : 'Dataset';
		$title = $this->getTitle();

		$out = oaiTag( 'oai_dc:dc', array(
			'xmlns:oai_dc'       => 'http://www.openarchives.org/OAI/2.0/oai_dc/',
			'xmlns:dc'           => 'http://purl.org/dc/elements/1.1/',
			'xmlns:xsi'          => 'http://www.w3.org/2001/XMLSchema-instance',
			'xsi:schemaLocation' => 'http://www.openarchives.org/OAI/2.0/oai_dc/ ' .
									'http://www.openarchives.org/OAI/2.0/oai_dc.xsd' ) ) . "\n" .
			oaiTag( 'dc:title',       array(), $title->getPrefixedText() ) . "\n" .
			oaiTag( 'dc:language',    array(), $title->getPageLanguage()->getCode() ) . "\n" .
			oaiTag( 'dc:type',        array(), $type ) . "\n" .
			oaiTag( 'dc:format',      array(), $this->getContentFormat() ) . "\n" .
			oaiTag( 'dc:identifier',  array(), $title->getCanonicalUrl() ) . "\n" .
			oaiTag( 'dc:contributor', array(), $this->_row->rev_user_text ) . "\n" .
			oaiTag( 'dc:date',        array(), OAIRepo::datestamp( $this->getDatestamp() ) ) . "\n" .
			"</oai_dc:dc>\n";
		return $out;
	}

	function renderMediaWiki() {
		$out = $this->_writer->openStream().$this->_writer->openPage($this->_row).
			$this->_writer->writeRevision($this->_row);

		if( $this->_row->page_namespace === NS_IMAGE ) {
			$out .= $this->renderUpload();
		}

		$out .= $this->_writer->closePage().$this->_writer->closeStream();

		return $out;
	}

	function renderLSearch() {
		$row = $this->_row;

		// Non-text content needs to be converted to plain text for search
		// XXX: maybe we should *always* convert? For text content, that would be a no-op...
		if ( !$this->hasTextContent() && isset( $this->_row->old_text ) ) {
			$handler = $this->getContentHandler();
			$text = Revision::getRevisionText( $row );
			$content = $handler->unserializeContent( $text, $this->getContentFormat() );

			// fetch synthetic text provided especially for search engines
			$row->old_text = $content->getTextForSearchIndex();
			$row->old_flags = 0; // clear flags, old_text is plain text now
			$row->rev_len = strlen( $row->old_text );

			// content is plain text now
			$row->rev_content_model = CONTENT_MODEL_TEXT;
			$row->rev_content_format = CONTENT_FORMAT_TEXT;
		}

		$out = $this->_writer->openStream().$this->_writer->openPage($row).
			$this->_writer->writeRedirects($row).
			$this->_writer->writeRevision($row);

		if( $this->_row->page_namespace === NS_IMAGE ) {
			$out .= $this->renderUpload();
		}

		$out .= $this->_writer->closePage().$this->_writer->closeStream();

		return $out;
	}

	function renderUpload() {
		$fname = 'WikiOAIRecord::renderUpload';
		$db = wfGetDB( DB_SLAVE );
		$imageRow = $db->selectRow( 'image',
			array( 'img_name', 'img_size', 'img_description',
				'img_user', 'img_user_text', 'img_timestamp' ),
			array( 'img_name' => $this->_row->page_title ),
			$fname );
		if( $imageRow ) {
			$file = wfFindFile( $imageRow->img_name );
			if ( !$file ) {
				wfDebug( 'Invalid image row retrieved. Image name: ' . $imageRow->img_name );
				return '';
			}
			$url = $file->getUrl();

			if( $url{0} == '/' ) {
				global $wgServer;
				$url = $wgServer . $url;
			}
			return implode( "\n", array(
				"<upload>",
				oaiTag( 'timestamp', array(), wfTimestamp( TS_ISO_8601, $imageRow->img_timestamp ) ),
				$this->renderContributor( $imageRow->img_user, $imageRow->img_user_text ),
				oaiTag( 'comment',   array(), $imageRow->img_description ),
				oaiTag( 'filename',  array(), $imageRow->img_name ),
				oaiTag( 'src',       array(), $url ),
				oaiTag( 'size',      array(), $imageRow->img_size ),
				"</upload>\n" ) );
		} else {
			return '';
		}
	}

	function renderContributor( $id, $text ) {
		if( $id ) {
			$tag = oaiTag( 'username', array(), $text ) .
				oaiTag( 'id', array(), $id );
		} else {
			$tag = oaiTag( 'ip', array(), $text );
		}
		return '<contributor>' . $tag . '</contributor>';
	}
}

/** For the very first page output siteinfo, else same sa XmlDumpWriter  */
class OAIDumpWriter extends XmlDumpWriter {

	function __construct(){
		$this->isFirst = true;
	}

	function siteInfo() {
		if($this->isFirst){
			$info = array(
				$this->sitename(),
				$this->homelink(),
				$this->generator(),
				$this->caseSetting(),
				$this->namespaces() );
			$this->isFirst = false;

			return "  <siteinfo>\n    " .
			implode( "\n    ", $info ) .
			"\n  </siteinfo>\n";
		} else
			return "";
	}
}

/**
 * Extends the MW import/export format with the lsearch syntax,
 * i.e. schema lsearch-0.1
 */
class OAILSearchWriter extends OAIDumpWriter {

	function __construct($resultSet){
		parent::__construct();
		$this->_redirects = array();
		for($i = 0 ; $i < $resultSet->numRows(); $i++){
			$row = $resultSet->fetchObject();
			$this->_redirects[$row->up_page][] = $row;
		}
	}

	function openStream() {
		global $wgContLanguageCode;
		$ver = "0.1";
		return Xml::element( 'mediawiki', array(
			'xmlns'              => "http://www.mediawiki.org/xml/lsearch-$ver/",
			'xmlns:xsi'          => "http://www.w3.org/2001/XMLSchema-instance",
			'xsi:schemaLocation' => "http://www.mediawiki.org/xml/lsearch-$ver/ " .
									"http://www.mediawiki.org/xml/lsearch-$ver.xsd",
			'version'            => $ver,
			'xml:lang'           => $wgContLanguageCode ),
			null ) .
			"\n" .
			$this->siteInfo();
	}

	function openPage( $row ) {
		$out = parent::openPage( $row );
		if(isset($row->num_page_ref))
			$out .= '    ' . Xml::element( 'references', array(), strval( $row->num_page_ref ) ) . "\n";
		return $out;
	}

	function writeRedirects($row){
		$out = '';
		if(isset($this->_redirects[$row->up_page])){
			foreach($this->_redirects[$row->up_page] as $row){
				$title = Title::makeTitle( $row->page_namespace, $row->page_title );
				$out .= "    <redirect>\n";
				$out .= '    ' . Xml::elementClean( 'title', array(), $title->getPrefixedText() ) . "\n";
				if(isset($row->num_page_ref))
					$out .= '    ' . Xml::element( 'references', array(), strval( $row->num_page_ref ) ) . "\n";
				$out .= "    </redirect>\n";
			}
		}
		return $out;
	}
}
