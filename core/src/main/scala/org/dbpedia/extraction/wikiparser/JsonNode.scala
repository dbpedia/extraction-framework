package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.annotations.WikiNodeAnnotation
import org.dbpedia.extraction.config.provenance.NodeRecord
import org.dbpedia.extraction.util.StringUtils.escape
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.iri.IRI
import org.wikidata.wdtk.datamodel.json.jackson.JacksonTermedStatementDocument

/**
 * @param wikiPage wikidata json page
 * @param wikiDataDocument wikidata toolkit ItemDocument
 */

@WikiNodeAnnotation(classOf[JsonNode])
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