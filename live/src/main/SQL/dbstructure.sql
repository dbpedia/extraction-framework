create table "DB"."DBA"."dbpedia_triples"
(
  "oaiid" INTEGER,
  "resource" VARCHAR(510),
  "content" LONG VARCHAR,
  PRIMARY KEY ("oaiid")
);


create table "DB"."DBA"."TRIPLE_DIFF_TYPE"
(
  "DIFF_TYPE_ID" SMALLINT,
  "DIFF_TYPE" VARCHAR(8),
  PRIMARY KEY ("DIFF_TYPE_ID")
);


create table "DB"."DBA"."dbpedia_triples_diff"
(
  "triple_id" INTEGER IDENTITY,
  "oaiid" INTEGER,
  "diff_type_id" SMALLINT,
  "triple" LONG VARCHAR,
  PRIMARY KEY ("triple_id")
);

ALTER TABLE "DB"."DBA"."dbpedia_triples_diff"
  ADD CONSTRAINT "oaiid_fk" FOREIGN KEY ("oaiid")
    REFERENCES "DB"."DBA"."dbpedia_triples" ("oaiid");

ALTER TABLE "DB"."DBA"."dbpedia_triples_diff"
  ADD CONSTRAINT "diff_type_id_fk" FOREIGN KEY ("diff_type_id")
    REFERENCES "DB"."DBA"."TRIPLE_DIFF_TYPE" ("DIFF_TYPE_ID");

