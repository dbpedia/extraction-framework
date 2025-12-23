
CREATE TABLE IF NOT EXISTS translations (
  sword VARCHAR(255),
  slang VARCHAR(255),
  spos VARCHAR(255),
  ssense VARCHAR(255),
  tword VARCHAR(255),
  tlang VARCHAR(255)
);

TRUNCATE TABLE translations;

LOAD DATA LOCAL INFILE '/home/jonas/programming/java/dbpedia.hg/wiktionary/translations.csv'
IGNORE 
INTO TABLE translations
  FIELDS TERMINATED BY ',' ENCLOSED BY '"'
  LINES TERMINATED BY '\n'
