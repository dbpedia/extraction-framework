package org.dbpedia.extraction.wikiparser.impl.simple

import org.apache.log4j.Level
import org.dbpedia.extraction.wikiparser.WikiParserException

/**
 * Thrown if the parser encounters too many errors.
 */
private final class TooManyErrorsException(line: Int, text: String) extends WikiParserException("Too many errors", line, text, Level.ERROR)
