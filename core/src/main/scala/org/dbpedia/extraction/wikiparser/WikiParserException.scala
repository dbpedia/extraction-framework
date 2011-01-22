package org.dbpedia.extraction.wikiparser

/**
 * Thrown whenever a parsing error is encountered.
 */
class WikiParserException(msg : String, wikiLineNumber : Int = 0, wikiLine : String = "Unknown") extends Exception(msg + " at '" + wikiLine + "' (Line: " + wikiLineNumber + ")")
