package org.dbpedia.extraction.wikiparser

import org.wikidata.wdtk.datamodel.json.jackson.JacksonTermedStatementDocument

/**
 * @param wikiPage wikidata json page
 * @param wikiDataDocument wikidata toolkit ItemDocument
 */

class JsonNode  (
                  val wikiPage : WikiPage,
                  val wikiDataDocument : JacksonTermedStatementDocument
                  )
  extends Node {
  def toPlainText: String = ""
  def toWikiText: String = ""

  override def children = List()

  override val line = 0

  override def getNodeRecord = this.root.getNodeRecord
}