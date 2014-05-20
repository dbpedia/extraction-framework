--
-- Summary table of last-edit timestamp of every page in the wiki.
-- This includes entries for pages which are deleted, so they can
-- be cleanly kept track of.
--
DROP TABLE IF EXISTS /*wgDBprefix*/updates;

CREATE TABLE /*$wgDBprefix*/updates (
  up_page int(10) unsigned NOT NULL,
  up_action enum('modify','create','delete') NOT NULL default 'modify',
  up_timestamp char(14) NOT NULL default '',

  -- For clean paging
  up_sequence int(10) unsigned NOT NULL auto_increment,

  -- Exactly one entry per page
  PRIMARY KEY up_page(up_page),

  -- We routinely pull things based on timestamp.
  KEY up_timestamp(up_timestamp),
  KEY up_sequence(up_sequence,up_timestamp)
) /*$wgDBTableOptions*/;

--
-- Initialize the table from the current state.
-- This will not list any _prior_ deletions, unfortunately.
-- New deletions can be kept track of as the happen through
-- updates from the extension hooks.
--
-- INSERT INTO /*$wgDBprefix*/updates (up_page, up_action, up_timestamp)
-- SELECT cur_id, IF(cur_is_new, 'create', 'modify'), cur_timestamp
-- FROM /*$wgDBprefix*/cur
-- ORDER BY cur_timestamp;

INSERT INTO /*$wgDBprefix*/updates (up_page, up_action, up_timestamp)
SELECT page_id, IF(page_is_new, 'create', 'modify'), rev_timestamp
FROM /*$wgDBprefix*/page, /*$wgDBprefix*/revision
WHERE page_latest=rev_id
ORDER BY page_latest;
