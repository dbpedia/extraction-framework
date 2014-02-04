<?php

/**
 * OAI-PMH repository extension for MediaWiki 1.4+
 *
 * Copyright (C) 2005 Brion Vibber <brion@pobox.com>
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
 * @todo Check update hooks for all actions
 * @todo Make sure identifiers are correct format
 * @todo Configurable bits n pieces
 * @todo Test for conformance & error conditions
 */

if( !defined( 'MEDIAWIKI' ) ) {
	die();
}

/**
 * To limit access to specific user-agents
 */
$oaiAgentRegex = false;

/**
 * To use HTTP authentication for clients in the oaiuser database
 */
$oaiAuth = false;

/**
 * Log accesses into the oaiaudit table
 */
$oaiAudit = false;

/**
 * The oaiaudit and oaiusers tables are used on the connection
 * in load group 'oai', or the primary database if none is set.
 *
 * If the tables are not in the default database for that connection,
 * put it here so the system knows where to find them.
 */
$oaiAuditDatabase = false;

/**
 * Number of records to return in each ListRecords or ListIdentifiers request.
 * Additional records will be available by making another request using the
 * ResumptionToken returned.
 */
$oaiChunkSize = 50;

$wgExtensionCredits['specialpage'][] = array(
	'path'           => __FILE__,
	'name'           => 'OAIRepository',
	'author'         => 'Brion Vibber',
	'url'            => 'https://www.mediawiki.org/wiki/Extension:OAIRepository',
	'descriptionmsg' => 'oai-desc',
);

$dir = dirname(__FILE__) . '/';
$wgExtensionMessagesFiles['OAIRepository'] = $dir . 'OAIRepo.i18n.php';
$wgExtensionMessagesFiles['OAIRepositoryAlias'] = $dir . 'OAIRepo.alias.php';
$wgAutoloadClasses['SpecialOAIRepository'] = $dir . 'OAIRepo_body.php';
$wgAutoloadClasses['OAIRepo'] = $dir . 'OAIRepo_body.php';
$wgAutoloadClasses['OAIHarvester'] = $dir . 'OAIHarvest.php';
$wgAutoloadClasses['OAIHook'] = $dir . 'OAIHooks.php';
$wgSpecialPages['OAIRepository'] = 'SpecialOAIRepository';

/* Add update hooks */
$wgHooks['ArticleSaveComplete'  ][] = 'OAIHook::updateSave';
$wgHooks['ArticleDelete'        ][] = 'OAIHook::updateDeleteSetup';
$wgHooks['ArticleDeleteComplete'][] = 'OAIHook::updateDelete';
$wgHooks['TitleMoveComplete'    ][] = 'OAIHook::updateMove';
$wgHooks['ParserTestTables'     ][] = 'OAIHook::testTables';
$wgHooks['ArticleUndelete'      ][] = 'OAIHook::updateUndelete';
$wgHooks['LoadExtensionSchemaUpdates'][] = 'OAIHook::updateSchemaHook';

$oaiDeleteIds = array();
