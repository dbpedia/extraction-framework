package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.sources.WikiPage
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument


/**
 * @param wikiPage wikidata json page
 * @param wikiDataItem wikidata toolkit ItemDocument
 */

class JsonNode  (
                  val wikiPage : WikiPage,
                  val wikiDataItem : ItemDocument
                  )
  extends Node(List.empty, 0) {
  def toPlainText: String = ""
  def toWikiText: String = ""
}