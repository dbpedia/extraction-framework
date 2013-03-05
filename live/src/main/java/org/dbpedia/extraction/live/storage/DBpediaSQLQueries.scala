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
    "SELECT timesUpdated, json, subjects FROM DBPEDIALIVE_CACHE WHERE pageID = ? "
  }

  def getJSONCacheInsert: String = {
    "INSERT INTO DBPEDIALIVE_CACHE (pageID, title, updated, timesUpdated, json, subjects, diff) VALUES ( ?, ? , now() , ? , ? , ? , ?  ) "
  }

  def getJSONCacheUpdate: String = {
    "UPDATE DBPEDIALIVE_CACHE SET title = ?, updated = now(), timesUpdated = ?, json = ?, subjects = ?, diff = ? WHERE pageID = ? "
  }

  def getJSONCacheDelete: String = {
    "DELETE FROM DBPEDIALIVE_CACHE WHERE pageID = ?"
  }

  def getJSONCacheUnmodified: String = {
    "SELECT pageID, updated FROM DBPEDIALIVE_CACHE WHERE datediff(updated,now()) <= ? ORDER BY updated ASC LIMIT ? "
  }

}
