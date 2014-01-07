--
-- Table structure for table `DBPEDIALIVE_CACHE`
--

SET SESSION innodb_file_per_table=1;
SET SESSION innodb_file_format=Barracuda;

DROP TABLE IF EXISTS `DBPEDIALIVE_CACHE`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_CACHE` (
  `pageID` int(11) NOT NULL DEFAULT '0' COMMENT 'The wikipedia page ID',
  `title` varchar(512) COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'The wikipedia page title',
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'timestamp of when the page was updated',
  `timesUpdated` smallint(6) NOT NULL DEFAULT '0' COMMENT 'Total times the page was updated', -- Fot future use: complete update after e.g. 10 updates
  `json` longtext COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'The latest extraction in JSON format',
  `subjects` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Distinct subjects extracted from the current page (might be more than one)',
  `diff` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '' COMMENT 'Keeps the latest triple diff (not implemented yet)',
  `error` SMALLINT NOT NULL DEFAULT '0' COMMENT 'If there was an error the last time the page was updated',

  PRIMARY KEY (`pageID`),
  KEY `updated_index` (`updated`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ENGINE = InnoDB ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4;

-- We use innodb_file_per_table=1; innodb_file_format=Barracuda; ROW_FORMAT=COMPRESSED KEY_BLOCK_SIZE=4
-- because in English Wikipedia the cache can reach up to 200GB!!!
-- This way we reduce I/O and space a lot. It makes the db a little slower but it is also easier to recover
-- when tables are stored in separate files.