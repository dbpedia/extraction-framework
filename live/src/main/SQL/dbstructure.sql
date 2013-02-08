DROP   TABLE "DB"."DBA"."DBPEDIALIVE_CACHE";
CREATE TABLE "DB"."DBA"."DBPEDIALIVE_CACHE"
(
  "pageID" INTEGER, -- the pageID of the wikipedia entry
  "title" VARCHAR(510), -- the title of the wikipedia entry
  "updated" DATETIME, -- timestamp of the last cache update
  "timesUpdated" DECIMAL, -- how many times it was updated (every X times we'll do a clean update')
  "json" LONG VARCHAR, -- the JSON cache
  "subjects" LONG VARCHAR, -- the list of distinct subject a wikipedia entry generated (for clean update / delete)
  "diff" LONG VARCHAR, -- the latest diff (added, deleted, unmodified) for debugging

  PRIMARY KEY ("pageID")
);

-- This index helps in the unmodified pages feeder
CREATE INDEX updated_index
ON "DB"."DBA"."DBPEDIALIVE_CACHE" (updated);
