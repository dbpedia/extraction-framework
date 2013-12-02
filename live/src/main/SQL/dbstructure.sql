--
-- Table structure for table `DBPEDIALIVE_CACHE`
--

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
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;