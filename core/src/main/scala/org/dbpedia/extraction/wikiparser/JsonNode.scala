package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.sources.WikiPage
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument

/*enum to express type of node returned for parser to extractor in
 for each parser to deal with it's returned node
 todo : implement this using the AST
  */




/**
 * it's a class which returns string triples in a Map in order to bypass that AST can't describe all triples
 *
 * the subject will be provided to the Extractor
 * Holder for two kinds of triples
 * 1- Uritriples : in which the key will be the property and the value will be the list of URI values for the property
 * 2- valueTriples : in which the key will be the property and the value will be the Map of values for the property in the form Value:lang
 * to store languages also beside the values
 *
 * when building the extractor user should know which kind of triples he is expecting values or uri ones
 * or handle both cases if it's unexpectable
 *
 * todo: fix the extraction framework to accept new dataypes
 *
 * @param uriTriples  the list of properties and objects for specific subject page
 *
 * @param children The contents of this page
 */

class JsonNode  (
  val wikiPage : WikiPage,
  val wikiDataItem : ItemDocument
) 
extends Node(List.empty, 0) {
  def toPlainText: String = ""
  def toWikiText: String = ""
}