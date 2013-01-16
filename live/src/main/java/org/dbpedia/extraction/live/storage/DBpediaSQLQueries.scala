package org.dbpedia.extraction.live.storage

/**
 * Here we keep all the SQL Queries we use for DBpedia Live.
 * Just to keep them all in one place...
 */
object DBpediaSQLQueries {

  /*
    * JSON Cache Queries (select, update, insert, delete)
    * */
  def getJSONCacheSelect: String = {
    "SELECT content FROM DBPEDIA_TRIPLES WHERE oaiid = ?"
  }

  def getJSONCacheInsert: String = {
    "INSERT INTO DBPEDIA_TRIPLES (oaiid, resource, content) VALUES ( ?, ? , ?  ) "
  }

  def getJSONCacheUpdate: String = {
    "UPDATE DBPEDIA_TRIPLES SET resource = ?, content = ? WHERE oaiid = ? "
  }

  def getJSONCacheDelete: String = {
    "DELETE FROM DBPEDIA_TRIPLES WHERE oaiid = ?"
  }

}
