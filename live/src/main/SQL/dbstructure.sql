create table "DB"."DBA"."DBPEDIA_TRIPLES"
(
  "oaiid" INTEGER,
  "resource" VARCHAR(510),
  "content" LONG VARCHAR,
  PRIMARY KEY ("oaiid")
);


create table "DB"."DBA"."DBPEDIA_TRIPLES_DIFF_TYPE"
(
  "DIFF_TYPE_ID" SMALLINT,
  "DIFF_TYPE" VARCHAR(8),
  PRIMARY KEY ("DIFF_TYPE_ID")
);


create table "DB"."DBA"."DBPEDIA_TRIPLES_DIFF"
(
  "triple_id" INTEGER IDENTITY,
  "oaiid" INTEGER,
  "diff_type_id" SMALLINT,
  "triple" LONG VARCHAR,
  PRIMARY KEY ("triple_id")
);

ALTER TABLE "DB"."DBA"."DBPEDIA_TRIPLES_DIFF"
  ADD CONSTRAINT "oaiid_fk" FOREIGN KEY ("oaiid")
    REFERENCES "DB"."DBA"."DBPEDIA_TRIPLES" ("oaiid");

ALTER TABLE "DB"."DBA"."DBPEDIA_TRIPLES_DIFF"
  ADD CONSTRAINT "diff_type_id_fk" FOREIGN KEY ("diff_type_id")
    REFERENCES "DB"."DBA"."DBPEDIA_TRIPLES_DIFF_TYPE" ("DIFF_TYPE_ID");


create procedure DBPEDIA_UPDATE_TRIPLES_DIFF_FOR_RESOURCE(IN param_resource VARCHAR(510), IN param_added_triple LONG VARCHAR,
IN param_deleted_triple LONG VARCHAR, IN param_modified_triple LONG VARCHAR)

{

  if(strcontains(param_resource, '/File:')){
    goto done_update;
  } 

  declare resource_oaiid INTEGER;
  declare crResource cursor for SELECT oaiid FROM dbpedia_triples WHERE resource = param_resource;
  declare temp_oaiid INTEGER;


  whenever not found goto done_update;
  open crResource;
  if (1)
  {
    fetch crResource into resource_oaiid;
    if (blob_to_string(param_added_triple) = '')
    {
      DELETE FROM dbpedia_triples_diff where oaiid = resource_oaiid AND diff_type_id = 1;
    }
    else{
            declare crAddedTripleExists cursor for SELECT oaiid FROM dbpedia_triples_diff WHERE oaiid = resource_oaiid AND diff_type_id = 1;
            whenever not found goto no_added_triple;
            open crAddedTripleExists;
            if (1)
            {
                fetch crAddedTripleExists into temp_oaiid;
                UPDATE dbpedia_triples_diff SET triple = param_added_triple where oaiid = resource_oaiid AND diff_type_id = 1;
                goto done_added_triple;
            }

            no_added_triple: INSERT INTO dbpedia_triples_diff (oaiid, diff_type_id, triple) VALUES (resource_oaiid, 1, param_added_triple);
            done_added_triple: close crAddedTripleExists;
    }

    if(blob_to_string(param_deleted_triple) = '')
    {
        DELETE FROM dbpedia_triples_diff where oaiid = resource_oaiid AND diff_type_id = 2;
    }
    else{
            declare crDeletedTripleExists cursor for SELECT oaiid FROM dbpedia_triples_diff WHERE oaiid = resource_oaiid AND diff_type_id = 2;
            whenever not found goto no_deleted_triple;
            open crDeletedTripleExists;
            if (1)
            {
                fetch crDeletedTripleExists into temp_oaiid;
                UPDATE dbpedia_triples_diff SET triple = param_deleted_triple where oaiid = resource_oaiid AND diff_type_id = 2;
                goto done_deleted_triple;
            }

            no_deleted_triple: INSERT INTO dbpedia_triples_diff (oaiid, diff_type_id, triple) VALUES (resource_oaiid, 2, param_deleted_triple);
            done_deleted_triple: close crDeletedTripleExists;
    }

    if(blob_to_string(param_modified_triple) = '')
    {
        DELETE FROM dbpedia_triples_diff where oaiid = resource_oaiid AND diff_type_id = 3;
    }

    else{
        declare crModifiedTripleExists cursor for SELECT oaiid FROM dbpedia_triples_diff WHERE oaiid = resource_oaiid AND diff_type_id = 3;
        whenever not found goto no_modified_triple;
        open crModifiedTripleExists;
        if (1)
        {
            fetch crModifiedTripleExists into temp_oaiid;
            UPDATE dbpedia_triples_diff SET triple = param_modified_triple where oaiid = resource_oaiid AND diff_type_id = 3;
            goto done_modified_triple;
        }
        no_modified_triple: INSERT INTO dbpedia_triples_diff (oaiid, diff_type_id, triple) VALUES (resource_oaiid, 3, param_modified_triple);
        done_modified_triple: close crModifiedTripleExists;
    }

    --UPDATE dbpedia_triples_diff SET triple = param_added_triple where oaiid = resource_oaiid AND diff_type_id = 1;
    --UPDATE dbpedia_triples_diff SET triple = param_deleted_triple where oaiid = resource_oaiid AND diff_type_id = 2;
    --UPDATE dbpedia_triples_diff SET triple = param_modified_triple where oaiid = resource_oaiid AND diff_type_id = 3;

  }

  done_update:
  close crResource;
}

create procedure DBPEDIA_DELETE_ALL_RESOURCE_TRIPLES_FROM_DBPEDIA_TRIPLES_DIFF_TABLE(IN param_resource VARCHAR(510))
{
  declare resource_oaiid INTEGER;
  declare crResource cursor for SELECT oaiid FROM dbpedia_triples WHERE resource = param_resource;

  whenever not found goto done_delete;
  open crResource;
  if (1)
  {

    fetch crResource into resource_oaiid;
    DELETE FROM dbpedia_triples_diff where oaiid = resource_oaiid;
  }

  done_delete:
  close crResource;
}

create procedure DBPEDIA_INSERT_IN_DBPEDIA_TRIPLES_DIFF_TABLE( IN param_resource VARCHAR(510), IN param_diff_type_id SMALLINT, IN param_triple LONG VARCHAR)
{

  declare resource_oaiid INTEGER;
  declare crResource cursor for SELECT oaiid FROM dbpedia_triples WHERE resource = param_resource;

  whenever not found goto done_insert;
  open crResource;
  if (1)
  {
    fetch crResource into resource_oaiid;
    INSERT INTO dbpedia_triples_diff(oaiid, diff_type_id, triple) 
    VALUES(resource_oaiid, param_diff_type_id, param_triple);
  }

  done_insert:
  close crResource;
}
