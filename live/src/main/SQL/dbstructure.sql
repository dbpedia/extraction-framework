--
-- Table structure for table `DBPEDIALIVE_CACHE`
--

DROP TABLE IF EXISTS `DBPEDIALIVE_CACHE`;
CREATE TABLE IF NOT EXISTS `DBPEDIALIVE_CACHE` (
  `pageID` int(11) NOT NULL DEFAULT '0',
  `title` varchar(512) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `timesUpdated` smallint(6) NOT NULL DEFAULT '0',
  `json` longtext COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `subjects` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `diff` text COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `error` SMALLINT NOT NULL DEFAULT '0',

  PRIMARY KEY (`pageID`),
  KEY `updated_index` (`updated`)
) DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;