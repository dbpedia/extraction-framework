package org.dbpedia.extraction.wikiparser

import java.time.ZonedDateTime

/**
 * Represents a revision.
 *
 * @param id The page ID
 * @param pageUri Uri of the Page
 * @param parent_Uri Uri of the parent Page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the latest contributor
 * @param contributorIP IP of the contributor
 * @param contributorName The name of the latest contributor
 * @param comment The revision comment added by the contributor
 * @param text_size Size of the revision content
 * @param minor_edit Flag if revision is minor
 * @param text_delta Difference of the text size since the last revision
 */
class RevisionNode(
                    val id: Long,
                    val pageUri: String,
                    val parent_Uri: String,
                    val timestamp: String,
                    val contributorID: String,
                    val contributorName: String,
                    val contributorIP: String,
                    val contributorDeleted: Boolean,
                    val comment: String,
                    val text_size: Int,
                    val minor_edit: Boolean,
                    val text_delta: Int
  )
  extends java.io.Serializable
{


  def this(id: String, pageUri:String, parent_Uri: String, timestamp: String, contributorID: String, contributorName: String, contributorIP: String, contributorDeleted: String, comment: String, text_size: String, minor_edit:String, text_delta: Int) = {

    this(RevisionNode.parseLong(id), pageUri, parent_Uri,timestamp, contributorID, contributorName, contributorIP, RevisionNode.parseBoolean(contributorDeleted), comment, RevisionNode.parseInt(text_size),RevisionNode.parseBoolean(minor_edit),text_delta)
  }

  def getUserIDAlt: String = {
    if(this.contributorID != "")  this.contributorID
    else if(this.contributorIP != "")  this.contributorIP
    else this.contributorName
  }

  def getYear: String = {
      ZonedDateTime.parse(this.timestamp).getYear.toString
  }

  def getYearMonth: String = {
    val year = ZonedDateTime.parse(this.timestamp).getYear.toString
    val month = ZonedDateTime.parse(this.timestamp).getMonthValue.toString
    month+"/"+year
  }
}
object RevisionNode {
  def parseBoolean(str: String): Boolean = {
   // System.out.println(">>>>>>>>>>>>>" +str)
    if (str == "false" || str.isEmpty) false
    else true
  }
  def parseInt(str: String): Int = {
    if (str == null || str.isEmpty) -1
    else str.toInt
  }


  def parseLong(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else str.toLong
  }

}