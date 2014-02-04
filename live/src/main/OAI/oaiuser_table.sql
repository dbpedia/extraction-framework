DROP TABLE IF EXISTS /*$wgDBprefix*/oaiuser;

CREATE TABLE /*$wgDBprefix*/oaiuser (
  ou_id INTEGER NOT NULL AUTO_INCREMENT,
  ou_name VARCHAR(255),
  ou_password_hash TINYBLOB,

  PRIMARY KEY (ou_id),
  UNIQUE KEY (ou_name)
) ENGINE=InnoDB;
