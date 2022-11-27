package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.util.{RecordEntry, RecordSeverity}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.wikiparser.RevisionNode.parseInt
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.Elem

/**
 * Represents a revision.
 *
 * @param id The page ID
 * @param revision The revision of this page
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the latest contributor
 * @param contributorName The name of the latest contributor
 * @param children The contents of this page
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
                    val format: String,
                    val text_size: Int,
                    val minor_edit: Boolean,
                    val text_delta: Int
  )
  extends java.io.Serializable
{


  def this(id: String, pageUri:String, parent_Uri: String, timestamp: String, contributorID: String, contributorName: String, contributorIP: String, contributorDeleted: String, comment: String, format: String, text_size: String, minor_edit:String, text_delta: Int) = {

    this(RevisionNode.parseLong(id), pageUri, parent_Uri,timestamp, contributorID, contributorName, contributorIP, RevisionNode.parseBoolean(contributorDeleted), comment, format,RevisionNode.parseInt(text_size),RevisionNode.parseBoolean(minor_edit),text_delta)
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

  def formatInt(id: Int): String = {
    if (id < 0) ""
    else id.toString
  }

  def parseLong(str: String): Long = {
    if (str == null || str.isEmpty) -1
    else str.toLong
  }

  def formatLong(id: Long): String = {
    if (id < 0) ""
    else id.toString
  }

}