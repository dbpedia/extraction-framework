package org.dbpedia.extraction.wikiparser

import org.apache.log4j.Level

/**
 * Thrown whenever a parsing error is encountered.
 */
class WikiParserException(msg : String, val level: Level = Level.DEBUG) extends Exception(msg) {
  def this(msg: String, line: Int, text: String, level: Level) = this(msg+" at '"+text+"' (line: "+line+")", level)
}
