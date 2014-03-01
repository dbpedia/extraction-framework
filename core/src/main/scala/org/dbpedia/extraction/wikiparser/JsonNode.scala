package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.sources.WikiPage

/*enum to express type of node returned for parser to extractor in
 for each parser to deal with it's returned node
 todo : implement this using the AST
  */
object JsonNode {
  type NodeType = String

  val LanguageLinks = "LanguageLinks"
  val Labels = "Labels"

  val Facts = "Facts"   //for normal Wikidata dump extractor , data weren't adapted for MappedDBpedia dump

  val MappedFacts = "MappedFacts"

  val CoordinatesFacts = "MappedCoordinates"
  val TimeFacts = "TimeFacts"
  val StringFacts = "StringFacts"
  val CommonMediaFacts = "CommonMediaFacts"

  val NotImportant = "NotImportant"

}





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
  val uriTriples: collection.mutable.Map[String, List[String]],
  val valueTriples: collection.mutable.Map[String, collection.mutable.Map[String,String]],
  val NodeType : JsonNode.NodeType,
  children: List[Node]
) 
extends Node(children, 0)
{
    def getUriTriples : collection.mutable.Map[String,List[String]] = uriTriples
    def getValueTriples : collection.mutable.Map[String,collection.mutable.Map[String,String]] = valueTriples
    def toPlainText : String = getUriTriples.mkString+getValueTriples.mkString
    def toWikiText : String = getUriTriples.mkString+getValueTriples.mkString


    def this (
             wikiPage : WikiPage,
             uriTriples: collection.mutable.Map[String, List[String]] = collection.mutable.Map.empty,
             valueTriples: collection.mutable.Map[String, collection.mutable.Map[String,String]]= collection.mutable.Map.empty,
             NodeType : JsonNode.NodeType = JsonNode.NotImportant
             ) = this(wikiPage,
      if(uriTriples==null) collection.mutable.Map.empty else uriTriples,
      if(valueTriples==null) collection.mutable.Map.empty else valueTriples,
      if(NodeType == null) JsonNode.NotImportant else NodeType,
      List.empty
    )
}


