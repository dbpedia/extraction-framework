package org.dbpedia.extraction.wikiparser.impl.simple

import org.dbpedia.extraction.wikiparser.WikiParserException

/**
 * Thrown if the parser encounters too many errors.
 */
private final class TooManyErrorsException(wikiLineNumber : Int = 0, wikiLine : String = "Unknown") extends WikiParserException("Too many errors", wikiLineNumber, wikiLine)
