DROP TABLE IF EXISTS /*$wgDBprefix*/oaiharvest;

CREATE TABLE /*$wgDBprefix*/oaiharvest (
  -- URL of active repository
  oh_repository VARCHAR(255),

  -- Last update timestamp we successfully applied;
  -- start here by default on next run.
  oh_last_timestamp VARCHAR(14),

  UNIQUE KEY (oh_repository)
) ENGINE=InnoDB;
