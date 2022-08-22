package org.dbpedia.extraction.wikiparser

/**
 * Thrown whenever a parsing error is encountered.
 */
class WikiParserException(msg : String) extends Exception(msg) {
  def this(msg: String, line: Int, text: String) = this(msg+" at '"+text+"' (line: "+line+")")
}
