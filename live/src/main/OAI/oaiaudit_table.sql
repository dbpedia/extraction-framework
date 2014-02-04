DROP TABLE IF EXISTS /*$wgDBprefix*/oaiaudit;

CREATE TABLE /*$wgDBprefix*/oaiaudit (
  oa_id INTEGER NOT NULL AUTO_INCREMENT,
  oa_client INTEGER,
  oa_timestamp VARCHAR(14),
  oa_dbname VARCHAR(32),

  oa_response_size INTEGER,
  oa_ip VARCHAR(32),
  oa_agent TEXT,

  oa_request TEXT,

  PRIMARY KEY (oa_id),
  KEY (oa_client,oa_timestamp),
  KEY (oa_timestamp,oa_client),
  KEY (oa_agent(20))
) ENGINE=InnoDB;
