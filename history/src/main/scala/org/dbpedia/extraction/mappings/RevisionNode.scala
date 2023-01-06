package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.wikiparser.Node
/**
 * Represents a page.
 *
 * @param id The page ID
 * @param parentId Id the the parent revision
 * @param timestamp The timestamp of the revision, in milliseconds since 1970-01-01 00:00:00 UTC
 * @param contributorID The ID of the contributor
 * @param contributorName The name of the contributor
 * @param contributorIP IP of the contributor
 * @param comment The revision comment added by the contributor
 * @param text_size Size of the revision content
 * @param minor_edit Flag if revision is minor
 */
class RevisionNode(
                   val id: Long,
                   val parentId: Long,
                   val timestamp: Long,
                   val contributorID: Long,
                   val contributorName: String,
                   val contributorIP: String,
                   val comment: String,
                   val text_size: Long,
                  val minor_edit: Boolean)
  extends Node(List.empty, 0)
  {

    def toWikiText = children.map(_.toWikiText).mkString

    def toPlainText = children.map(_.toPlainText).mkString

    override def equals(obj: scala.Any) = obj match {
      case otherRevisionNode: RevisionNode => (otherRevisionNode.id == id &&  otherRevisionNode.parentId == parentId && otherRevisionNode.timestamp == timestamp
        && otherRevisionNode.contributorID == contributorID && otherRevisionNode.contributorName == contributorName )
      case _ => false
    }

}